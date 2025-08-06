package net.potato.tuff;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Level;

public class TuffX extends JavaPlugin implements Listener, PluginMessageListener {

    public static final String CHANNEL = "eagler:below_y0";
    public ViaBlockIds viablockids;

    private static int CHUNKS_PER_TICK;

    private final Map<UUID, Queue<Vector>> requestQueue = new ConcurrentHashMap<>();
    
    private final Set<UUID> awaitingInitialBatch = ConcurrentHashMap.newKeySet();
    private final Map<UUID, AtomicInteger> initialChunksToProcess = new ConcurrentHashMap<>();
    private Set<String> enabledWorlds;

    private BukkitTask processorTask;

    private boolean debug;

    private void logDebug(String message) {
        if (debug) getLogger().log(Level.INFO, "[TuffX-Debug] " + message);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.CHUNKS_PER_TICK = getConfig().getInt("chunks-per-tick", 6);
        this.debug = getConfig().getBoolean("debug-mode", false);
        this.enabledWorlds = new HashSet<>(getConfig().getStringList("enabled-worlds"));
        
        logDebug("TuffX will be active in the following worlds: " + String.join(", ", this.enabledWorlds));

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);
        if (this.viablockids == null) this.viablockids = new ViaBlockIds(this);
        logFancyEnable();
        startProcessorTask();
    }

    @Override
    public void onDisable() {
        if (processorTask != null) processorTask.cancel();
        requestQueue.clear();
        awaitingInitialBatch.clear();
        initialChunksToProcess.clear();
        getLogger().info("TuffX has been disabled.");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL) || !player.isOnline()) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            int x = in.readInt();
            int y = in.readInt();
            int z = in.readInt();
            int actionLength = in.readUnsignedByte();
            byte[] actionBytes = new byte[actionLength];
            in.readFully(actionBytes);
            String action = new String(actionBytes, StandardCharsets.UTF_8);
            handleIncomingPacket(player, new Location(player.getWorld(), x, y, z), action, x, z, in);
        } catch (IOException e) {
            getLogger().warning("Failed to parse plugin message from " + player.getName() + ": " + e.getMessage());
        }
    }

    private void handleSingleChunkRequest(Player player, int chunkX, int chunkZ, UUID playerId) {
        requestQueue.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentLinkedQueue<>()).add(new Vector(chunkX, 0, chunkZ));
    }
    
    private void handleIncomingPacket(Player player, Location loc, String action, int chunkX, int chunkZ, DataInputStream in) throws IOException {
        UUID playerId = player.getUniqueId();

        if (!enabledWorlds.contains(player.getWorld().getName())) {
            if (awaitingInitialBatch.contains(playerId)) player.sendPluginMessage(this, CHANNEL, createLoadFinishedPayload());
            return;
        }

        switch (action.toLowerCase()) {
            case "request_chunk":
                handleSingleChunkRequest(player,chunkX,chunkZ,playerId);
                break;
            case "request_chunk_batch":
                if (awaitingInitialBatch.remove(playerId)) {
                    int batchSize = in.readInt();
                    logDebug("Received definitive initial batch of " + batchSize + " chunks. Queueing for processing.");

                    initialChunksToProcess.put(playerId, new AtomicInteger(batchSize));
                    
                    Queue<Vector> playerQueue = requestQueue.computeIfAbsent(playerId, k -> new ConcurrentLinkedQueue<>());
                    for (int i = 0; i < batchSize; i++) {
                        playerQueue.add(new Vector(in.readInt(), 0, in.readInt()));
                    }

                    if (batchSize == 0) {
                        checkIfInitialLoadComplete(player);
                    }
                } else {
                    int batchSize = in.readInt();
                    Queue<Vector> playerQueue = requestQueue.computeIfAbsent(playerId, k -> new ConcurrentLinkedQueue<>());
                    for (int i = 0; i < batchSize; i++) {
                        playerQueue.add(new Vector(in.readInt(), 0, in.readInt()));
                    }
                }
                break;
            case "ready":
                logDebug("Player " + player.getName() + " is READY. Awaiting first chunk batch...");
                if (enabledWorlds.contains(player.getWorld().getName())) {
                    awaitingInitialBatch.add(player.getUniqueId());
                    player.sendPluginMessage(this, CHANNEL, createBelowY0StatusPayload(true));
                } else {
                    logDebug("Not a supported world!");
                    player.sendPluginMessage(this, CHANNEL, createBelowY0StatusPayload(false));
                }
                break;
            case "use_on_block":
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Block block = loc.getBlock();
                        ItemStack item = player.getInventory().getItemInMainHand();
                        getServer().getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, block, block.getFace(player.getLocation().getBlock())));
                    }
                }.runTask(this);
                break;
        }
    }

    private byte[] createBelowY0StatusPayload(boolean status) {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("belowy0_status");
            out.writeBoolean(status);
            return bout.toByteArray();
        } catch (IOException e) { return null; }
    }

    private byte[] createDimensionPayload() {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("dimension_change");
            return bout.toByteArray();
        } catch (IOException e) { return null; }
    }
    
    private void startProcessorTask() {
        this.processorTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerUUID : new HashSet<>(requestQueue.keySet())) {
                    Player player = getServer().getPlayer(playerUUID);
                    if (player == null || !player.isOnline()) {
                        cleanupPlayer(playerUUID);
                        continue;
                    }
                    
                    Queue<Vector> queue = requestQueue.get(playerUUID);
                    if (queue != null && !queue.isEmpty()) {
                        for (int i = 0; i < CHUNKS_PER_TICK && !queue.isEmpty(); i++) {
                            Vector vec = queue.poll();
                            if (vec != null) {
                                World world = player.getWorld();
                                if (world.isChunkLoaded(vec.getBlockX(), vec.getBlockZ())) {
                                    processAndSendChunk(player, world.getChunkAt(vec.getBlockX(), vec.getBlockZ()));
                                } else {
                                    queue.add(vec); 
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Queue<Vector> playerQueue = requestQueue.get(playerId);
        if (playerQueue != null && !playerQueue.isEmpty()) {
            logDebug("Player " + player.getName() + " changed worlds. Clearing " + playerQueue.size() + " pending chunk requests.");
            playerQueue.clear();
        }
        
        if (initialChunksToProcess.remove(playerId) != null) {
            logDebug("Player " + player.getName() + " was in the middle of an initial chunk load. The process has been cancelled.");
            awaitingInitialBatch.remove(playerId);
            player.sendPluginMessage(this, CHANNEL, createLoadFinishedPayload());
        }

        player.sendPluginMessage(this, CHANNEL, createDimensionPayload());

        player.sendPluginMessage(this, CHANNEL, createBelowY0StatusPayload(enabledWorlds.contains(player.getWorld().getName())));
    }

    private void processAndSendChunk(final Player player, final Chunk chunk) {
        if (chunk == null || !player.isOnline()) return;

        final Vector chunkVec = new Vector(chunk.getX(), 0, chunk.getZ());
        logDebug("Processing chunk " + chunk.getX() + "," + chunk.getZ() + " for " + player.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                final ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
                final Map<BlockData, int[]> conversionCache = new HashMap<>();

                for (int sectionY = -4; sectionY < 0; sectionY++) {
                    if (!player.isOnline()) break;
                    try {
                        byte[] payload = createSectionPayload(snapshot, chunk.getX(), chunk.getZ(), sectionY, conversionCache);
                        if (payload != null) {
                            player.sendPluginMessage(TuffX.this, CHANNEL, payload);
                        }
                    } catch (IOException e) {
                        getLogger().severe("Payload creation failed for " + chunk.getX() + "," + chunk.getZ() + ": " + e.getMessage());
                    }
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            checkIfInitialLoadComplete(player);
                        }
                    }
                }.runTask(TuffX.this);
            }
        }.runTaskAsynchronously(this);
    }

    private void checkIfInitialLoadComplete(Player player) {
        UUID playerId = player.getUniqueId();
        AtomicInteger counter = initialChunksToProcess.get(playerId);

        if (counter != null) {
            int remaining = counter.decrementAndGet();
            logDebug("Player " + player.getName() + " finished a chunk. Remaining initial chunks: " + remaining);
            
            if (remaining <= 0) {
                logDebug("INITIAL LOAD COMPLETE for " + player.getName() + ". Sent finished packet.");
                player.sendPluginMessage(this, CHANNEL, createLoadFinishedPayload());
                
                initialChunksToProcess.remove(playerId);
            }
        }
    }

    private byte[] createLoadFinishedPayload() {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(); 
            DataOutputStream out = new DataOutputStream(bout)) {
            
            out.writeUTF("y0_load_finished");
            
            return bout.toByteArray();
        } catch (IOException e) {
            getLogger().severe("Failed to create the y0_load_finished payload!");
            return null;
        }
    }

    private void cleanupPlayer(UUID playerId) {
        requestQueue.remove(playerId);
        awaitingInitialBatch.remove(playerId);
        initialChunksToProcess.remove(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }
    
    private byte[] createWelcomePayload(String message, int someNumber) {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("welcome_data");
            out.writeUTF(message);
            out.writeInt(someNumber);
            return bout.toByteArray();
        } catch (IOException e) { return null; }
    }
    
    private byte[] createSectionPayload(ChunkSnapshot snapshot, int cx, int cz, int sectionY, Map<BlockData, int[]> cache) throws IOException {
    try (ByteArrayOutputStream bout = new ByteArrayOutputStream(12300); DataOutputStream out = new DataOutputStream(bout)) {
        out.writeUTF("chunk_data");
        out.writeInt(cx);
        out.writeInt(cz);
        out.writeInt(sectionY);

        boolean hasAnythingToSend = false;
        int baseY = sectionY * 16;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int worldY = baseY + y;
                    
                    BlockData blockData = snapshot.getBlockData(x, worldY, z);
                    int[] legacyData = cache.computeIfAbsent(blockData, viablockids::toLegacy);
                    out.writeShort((short) ((legacyData[1] << 12) | (legacyData[0] & 0xFFF)));

                    int blockLight = snapshot.getBlockEmittedLight(x, worldY, z);
                    int skyLight = snapshot.getBlockSkyLight(x, worldY, z);
                    out.writeByte((byte) ((skyLight << 4) | blockLight));

                    if (legacyData[0] != 0 || blockLight != 0 || skyLight != 0) {
                        hasAnythingToSend = true;
                    }
                }
            }
        }
        
        return hasAnythingToSend ? bout.toByteArray() : null;
    }
}

    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) { if (event.getBlock().getY() < 0) sendBlockUpdateToNearby(event.getBlock().getLocation(), Material.AIR.createBlockData()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) { if (event.getBlock().getY() < 0) sendBlockUpdateToNearby(event.getBlock().getLocation(), event.getBlock().getBlockData()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) { if (event.getBlock().getY() < 0) sendBlockUpdateToNearby(event.getBlock().getLocation(), event.getBlock().getBlockData()); }

    private void sendBlockUpdateToNearby(Location loc, BlockData data) {
        try {
            byte[] payload = createBlockUpdatePayload(loc, data);
            if (payload == null) return;
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(loc) < 4096) p.sendPluginMessage(this, CHANNEL, payload);
            }
        } catch (IOException e) { getLogger().severe("Failed to send block update: " + e.getMessage()); }
    }
    
    private byte[] createBlockUpdatePayload(Location loc, BlockData data) throws IOException {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("block_update");
            out.writeInt(loc.getBlockX()); out.writeInt(loc.getBlockY()); out.writeInt(loc.getBlockZ());
            int[] legacyData = viablockids.toLegacy(data);
            out.writeShort((short) ((legacyData[1] << 12) | (legacyData[0] & 0xFFF)));
            return bout.toByteArray();
        }
    }

    private void logFancyEnable() {
        getLogger().info("");
        getLogger().info("████████╗██╗   ██╗███████╗ ███████╗ ██╗  ██╗");
        getLogger().info("╚══██╔══╝██║   ██║██╔════╝ ██╔════╝ ╚██╗██╔╝");
        getLogger().info("   ██║   ██║   ██║██████╗  ██████╗   ╚███╔╝ ");
        getLogger().info("   ██║   ██║   ██║██╔═══╝  ██╔═══╝   ██╔██╗ ");
        getLogger().info("   ██║   ╚██████╔╝██║      ██║      ██╔╝╚██╗");
        getLogger().info("   ╚═╝    ╚═════╝ ╚═╝      ╚═╝      ╚═╝  ╚═╝");
        getLogger().info("");
        getLogger().info("Below y0 and TuffX programmed by Potato");
    }
}