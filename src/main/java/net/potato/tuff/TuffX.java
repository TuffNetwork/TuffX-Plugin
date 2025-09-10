package net.potato.tuff;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPhysicsEvent;

import org.bukkit.ChunkSnapshot;

public class TuffX extends JavaPlugin implements Listener, PluginMessageListener {

    public static final String CHANNEL = "eagler:below_y0";
    
    private final Set<ChunkSectionKey> sentChunks = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor chunkExecutor = new ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000),
        r -> {
            Thread t = new Thread(r, "TuffX-ChunkProcessor");
            t.setDaemon(true);
            return t;
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
    ) {{
        allowCoreThreadTimeOut(true);
    }};
    private volatile int cachedViewDistance = -1;

    @Override
    public void onEnable() {
        getLogger().info("TuffX has been enabled.");
        
        LegacyBlockIdManager.initialize(this);
        
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("TuffX has been disabled.");
        sentChunks.clear();
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                chunkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL) || player == null || !player.isOnline() || message == null) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subchannel = in.readUTF();
            if ("block_op".equals(subchannel)) {
                String action = in.readUTF();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                
                World world = player.getWorld();
                if (world == null) return;
                
                Location loc = new Location(world, x, y, z);
                Block block = world.getBlockAt(loc);
                if (block == null) return;

                if ("break".equals(action)) {
                    if (player.getGameMode() == GameMode.SURVIVAL) {
                        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
                        getServer().getPluginManager().callEvent(breakEvent);
                        if (!breakEvent.isCancelled()) {
                            block.breakNaturally(player.getInventory().getItemInMainHand());
                        }
                    } else {
                        block.setType(Material.AIR);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] createSectionPayload(World world, int cx, int cz, int sectionY) throws IOException {
        if (world == null) throw new IOException("World is null");
        
        Chunk chunk = world.getChunkAt(cx, cz);
        if (chunk == null) throw new IOException("Chunk is null");
        
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
        if (snapshot == null) throw new IOException("ChunkSnapshot is null");
        
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(8200);
             DataOutputStream out = new DataOutputStream(bout)) {
            
            out.writeUTF("chunk_data");
            out.writeInt(cx);
            out.writeInt(cz);
            out.writeInt(sectionY);
            
            int baseY = sectionY << 4;
            for (int y = 0; y < 16; y++) {
                int worldY = baseY + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (worldY < world.getMinHeight() || worldY > world.getMaxHeight()) {
                            out.writeShort((short) 0);
                        } else {
                            try {
                                Material type = snapshot.getBlockType(x, worldY, z);
                                out.writeShort(type == null ? 0 : LegacyBlockIdManager.getLegacyShort(type));
                            } catch (Exception e) {
                                out.writeShort((short) 0);
                            }
                        }
                    }
                }
            }
            out.flush();
            return bout.toByteArray();
        }
    }

    private void sendBlockUpdateToPlayer(Player player, int x, int y, int z, short legacyId) {
        if (player == null || !player.isOnline()) return;
        
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(32);
             DataOutputStream out = new DataOutputStream(bout)) {
            
            out.writeUTF("block_update");
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(z);
            out.writeShort(legacyId);
            out.flush();
            
            player.sendPluginMessage(this, CHANNEL, bout.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendChunkSectionsAsync(Player player, Chunk chunk) {
        if (player == null || chunk == null || !player.isOnline()) return;
        chunkExecutor.execute(() -> {
            try {
                sendChunkSections(player, chunk);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void sendChunkSections(Player player, Chunk chunk) {
        if (player == null || !player.isOnline() || chunk == null) return;
        
        UUID playerId = player.getUniqueId();
        if (playerId == null) return;
        
        World world = chunk.getWorld();
        if (world == null) return;
        
        String worldName = world.getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        
        for (int sectionY = -4; sectionY < 0; sectionY++) {
            ChunkSectionKey key = new ChunkSectionKey(playerId, worldName, cx, cz, sectionY);
            if (sentChunks.add(key)) {
                try {
                    byte[] payload = createSectionPayload(world, cx, cz, sectionY);
                    if (player.isOnline()) {
                        player.sendPluginMessage(this, CHANNEL, payload);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private boolean isPlayerInChunkRange(Player player, int chunkX, int chunkZ) {
        if (player == null || !player.isOnline()) return false;
        
        int viewDistance = getViewDistance();
        Location playerLoc = player.getLocation();
        if (playerLoc == null) return false;
        
        Chunk playerChunk = playerLoc.getChunk();
        if (playerChunk == null) return false;
        
        int dx = Math.abs(playerChunk.getX() - chunkX);
        int dz = Math.abs(playerChunk.getZ() - chunkZ);
        return dx <= viewDistance && dz <= viewDistance;
    }
    
    private int getViewDistance() {
        if (cachedViewDistance == -1) {
            synchronized (this) {
                if (cachedViewDistance == -1) {
                    cachedViewDistance = getServer().getViewDistance();
                }
            }
        }
        return cachedViewDistance;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block == null) return;
        
        int y = block.getY();
        if (y < 0) {
            int x = block.getX();
            int z = block.getZ();
            Chunk chunk = block.getChunk();
            if (chunk == null) return;
            
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            
            List<Player> nearbyPlayers = new ArrayList<>();
            World world = block.getWorld();
            if (world == null) return;
            
            for (Player player : world.getPlayers()) {
                if (player != null && player.isOnline() && isPlayerInChunkRange(player, chunkX, chunkZ)) {
                    nearbyPlayers.add(player);
                }
            }
            
            if (!nearbyPlayers.isEmpty()) {
                chunkExecutor.execute(() -> {
                    try {
                        for (Player player : nearbyPlayers) {
                            if (player != null && player.isOnline()) {
                                sendBlockUpdateToPlayer(player, x, y, z, (short) 0);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block == null) return;
        
        int y = block.getY();
        if (y < 0) {
            int x = block.getX();
            int z = block.getZ();
            Chunk chunk = block.getChunk();
            if (chunk == null) return;
            
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            short legacyId = LegacyBlockIdManager.getLegacyShort(block.getType());
            
            List<Player> nearbyPlayers = new ArrayList<>();
            World world = block.getWorld();
            if (world == null) return;
            
            for (Player player : world.getPlayers()) {
                if (player != null && player.isOnline() && isPlayerInChunkRange(player, chunkX, chunkZ)) {
                    nearbyPlayers.add(player);
                }
            }
            
            if (!nearbyPlayers.isEmpty()) {
                chunkExecutor.execute(() -> {
                    try {
                        for (Player player : nearbyPlayers) {
                            if (player != null && player.isOnline()) {
                                sendBlockUpdateToPlayer(player, x, y, z, legacyId);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block == null) return;
        
        int y = block.getY();
        if (y < 0) {
            int x = block.getX();
            int z = block.getZ();
            Chunk chunk = block.getChunk();
            if (chunk == null) return;
            
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            short legacyId = LegacyBlockIdManager.getLegacyShort(block.getType());
            
            List<Player> nearbyPlayers = new ArrayList<>();
            World world = block.getWorld();
            if (world == null) return;
            
            for (Player player : world.getPlayers()) {
                if (player != null && player.isOnline() && isPlayerInChunkRange(player, chunkX, chunkZ)) {
                    nearbyPlayers.add(player);
                }
            }
            
            if (!nearbyPlayers.isEmpty()) {
                chunkExecutor.execute(() -> {
                    try {
                        for (Player player : nearbyPlayers) {
                            if (player != null && player.isOnline()) {
                                sendBlockUpdateToPlayer(player, x, y, z, legacyId);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;
        
        chunkExecutor.execute(() -> {
            try {
                if (!player.isOnline()) return;
                
                Location playerLoc = player.getLocation();
                if (playerLoc == null) return;
                
                Chunk playerChunk = playerLoc.getChunk();
                World world = player.getWorld();
                if (world == null) return;
                
                int playerChunkX = playerChunk.getX();
                int playerChunkZ = playerChunk.getZ();
                
                int viewDistance = getViewDistance();
                
                for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                    for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                        if (!player.isOnline()) return;
                        Chunk chunk = world.getChunkAt(playerChunkX + dx, playerChunkZ + dz);
                        sendChunkSections(player, chunk);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        Location to = event.getTo();
        if (to == null) return;
        
        UUID playerId = player.getUniqueId();
        if (playerId != null) {
            try {
                sentChunks.removeIf(key -> key.playerId() != null && key.playerId().equals(playerId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            
            chunkExecutor.execute(() -> {
                try {
                    if (!player.isOnline() || to == null) return;
                    
                    Chunk playerChunk = to.getChunk();
                    World world = to.getWorld();
                    if (world == null) return;
                    
                    int playerChunkX = playerChunk.getX();
                    int playerChunkZ = playerChunk.getZ();
                    
                    int viewDistance = getViewDistance();
                    
                    for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                            if (!player.isOnline()) return;
                            Chunk chunk = world.getChunkAt(playerChunkX + dx, playerChunkZ + dz);
                            sendChunkSections(player, chunk);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, 10L);
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        if (chunk == null) return;
        
        World world = chunk.getWorld();
        if (world == null) return;
        
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (player != null && player.isOnline() && isPlayerInChunkRange(player, chunkX, chunkZ)) {
                nearbyPlayers.add(player);
            }
        }
        
        if (!nearbyPlayers.isEmpty()) {
            chunkExecutor.execute(() -> {
                try {
                    for (Player player : nearbyPlayers) {
                        if (player != null && player.isOnline()) {
                            sendChunkSections(player, chunk);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        UUID playerId = player.getUniqueId();
        if (playerId != null) {
            chunkExecutor.execute(() -> {
                try {
                    sentChunks.removeIf(key -> key.playerId() != null && key.playerId().equals(playerId));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
