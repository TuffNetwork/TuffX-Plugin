package net.potato.tuff;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPhysicsEvent;

import org.bukkit.ChunkSnapshot;

public class TuffX extends JavaPlugin implements Listener, PluginMessageListener, TabCompleter {

    public static final String CHANNEL = "eagler:below_y0";

    private final Set<ChunkSectionKey> sentChunks = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        getLogger().info("TuffX has been enabled.");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("TuffX has been disabled.");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subchannel = in.readUTF();
            if ("block_op".equals(subchannel)) {
                String action = in.readUTF();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                Location loc = new Location(player.getWorld(), x, y, z);
                Block block = loc.getBlock();

                switch (action.toLowerCase()) {
                    case "break":
                        getLogger().info("breaking block at " + loc.getX() + "," + loc.getY() + "," + loc.getZ());
                        if (player.getGameMode() == GameMode.SURVIVAL) {
                            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
                            getServer().getPluginManager().callEvent(breakEvent);

                            if (!breakEvent.isCancelled()) {
                                block.breakNaturally(player.getInventory().getItemInMainHand());
                            }
                        } else {
                            block.setType(Material.AIR);
                        }
                        break;
                    default:
                        getLogger().warning("Received unknown block_op action: " + action);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] createSectionPayload(World world, int cx, int cz, int sectionY) throws IOException {
        Chunk chunk = world.getChunkAt(cx, cz);
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);

        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(8200);
             DataOutputStream out = new DataOutputStream(bout)) {

            out.writeUTF("chunk_data");
            out.writeInt(cx);
            out.writeInt(cz);
            out.writeInt(sectionY);

            int baseY = sectionY * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int worldY = baseY + y;
                        Material type = snapshot.getBlockType(x, worldY, z);
                        if (type == null) type = Material.AIR;
                        
                        short legacyId = LegacyBlockIdManager.getLegacyShort(type);
                        out.writeShort(legacyId);
                    }
                }
            }
            return bout.toByteArray();
        }
    }

    private record ChunkSectionKey(UUID playerId, String worldName, int cx, int cz, int sectionY) {}
    
    private void sendBlockUpdateToNearby(Location loc, Material material) {
        getLogger().info("Sending block update for " + material.name() + " at " + loc.toString());
    }

    private void sendChunkSectionsAsync(Player player, Chunk chunk) {
        getLogger().info("Sending chunk sections to " + player.getName() + " for chunk " + chunk.getX() + "," + chunk.getZ());
    }
    
    private boolean isPlayerInChunkRange(Player player, Chunk chunk, int viewDistance) {
        int dx = Math.abs(player.getLocation().getChunk().getX() - chunk.getX());
        int dz = Math.abs(player.getLocation().getChunk().getZ() - chunk.getZ());
        return dx <= viewDistance && dz <= viewDistance;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        sendBlockUpdateToNearby(block.getLocation(), Material.AIR);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        sendBlockUpdateToNearby(block.getLocation(), block.getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        sendBlockUpdateToNearby(block.getLocation(), block.getType());
    }

    private void sendChunkSectionIfBelowY0(Player player, Block block) {
        if (block.getY() >= 0) return;

        Chunk chunk = block.getChunk();

        if (player != null) {
            sendChunkSectionsAsync(player, chunk);
        } else {
            for (Player p : chunk.getWorld().getPlayers()) {
                if (isPlayerInChunkRange(p, chunk, getServer().getViewDistance())) {
                    sendChunkSectionsAsync(p, chunk);
                }
            }
        }
    }
}
