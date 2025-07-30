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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TuffX extends JavaPlugin implements Listener, PluginMessageListener {

    public static final String CHANNEL = "eagler:below_y0";
    public ViaBlockIds viablockids;

    private static final int CHUNKS_PER_TICK = 2;

    private final Map<UUID, Queue<Vector>> requestQueue = new ConcurrentHashMap<>();
    private BukkitTask processorTask;

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);
        if (this.viablockids == null) this.viablockids = new ViaBlockIds(this);
        startProcessorTask();
        logFancyEnable();
    }

    @Override
    public void onDisable() {
        if (processorTask != null) processorTask.cancel();
        requestQueue.clear();
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
            handleIncomingPacket(player, new Location(player.getWorld(), x, y, z), action, x, z);
        } catch (IOException e) {
            getLogger().warning("Failed to parse plugin message from " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private void handleIncomingPacket(Player player, Location loc, String action, int chunkX, int chunkZ) {
        switch (action.toLowerCase()) {
            case "request_chunk":
                Queue<Vector> queue = requestQueue.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentLinkedQueue<>());
                queue.add(new Vector(chunkX, 0, chunkZ));
                break;
            case "ready":
                String welcome = "§bWelcome, §e" + player.getName() + "§b!";
                byte[] payload = createWelcomePayload(welcome, getServer().getOnlinePlayers().size());
                if (payload != null) player.sendPluginMessage(this, CHANNEL, payload);
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
    
    private void startProcessorTask() {
        this.processorTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Queue<Vector>> entry : requestQueue.entrySet()) {
                    Player player = getServer().getPlayer(entry.getKey());
                    Queue<Vector> queue = entry.getValue();

                    if (player == null || !player.isOnline() || queue.isEmpty()) continue;
                    
                    for (int i = 0; i < CHUNKS_PER_TICK && !queue.isEmpty(); i++) {
                        Vector vec = queue.poll();
                        if (vec != null) {
                            World world = player.getWorld();
                            int cx = vec.getBlockX();
                            int cz = vec.getBlockZ();
                            if (world.isChunkLoaded(cx, cz)) {
                                processAndSendChunk(player, world.getChunkAt(cx, cz));
                            } else {
                                queue.add(vec); 
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void processAndSendChunk(final Player player, final Chunk chunk) {
        if (chunk == null || !player.isOnline()) return;

        final Map<BlockData, int[]> conversionCache = new HashMap<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                final ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);

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
            }
        }.runTaskAsynchronously(this);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        requestQueue.remove(event.getPlayer().getUniqueId());
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
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(8200); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("chunk_data");
            out.writeInt(cx); out.writeInt(cz); out.writeInt(sectionY);
            boolean hasNonAirBlock = false;
            int baseY = sectionY * 16;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int worldY = baseY + y;
                BlockData blockData = snapshot.getBlockData(x, worldY, z);

                int[] legacyData = cache.computeIfAbsent(blockData, viablockids::toLegacy);

                if (legacyData[0] != 0) hasNonAirBlock = true;
                out.writeShort((short) ((legacyData[1] << 12) | (legacyData[0] & 0xFFF)));
            }
            return hasNonAirBlock ? bout.toByteArray() : null;
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