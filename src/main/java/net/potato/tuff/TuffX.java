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
import org.bukkit.event.EventPriority;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Chunk;

public class TuffX extends JavaPlugin implements Listener, PluginMessageListener, TabCompleter {

    public static final String CHANNEL = "eagler:below_y0";
@@ -224,9 +232,14 @@

        switch (action.toLowerCase()) {
            case "break":
                getLogger().info("breaking block at "+loc.getX()+","+loc.getY()+","+loc.getZ());
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    block.breakNaturally();
                    BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
                    getServer().getPluginManager().callEvent(breakEvent);

                    if (!breakEvent.isCancelled()) {
                        block.breakNaturally(player.getInventory().getItemInMainHand());
                    }
                } else {
                    block.setType(Material.AIR);
                }
@@ -290,7 +303,7 @@
        }
    }

    private byte[] createSectionPayload(World world, int cx, int cz, int sectionY) throws IOException {
   /* private byte[] createSectionPayload(World world, int cx, int cz, int sectionY) throws IOException {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(8200); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("chunk_data");
            out.writeInt(cx);
@@ -312,6 +325,37 @@
            }
            return bout.toByteArray();
        }
    }*/

    private byte[] createSectionPayload(World world, int cx, int cz, int sectionY) throws IOException {
        Chunk chunk = world.getChunkAt(cx, cz);
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false); // avoid lighting overhead

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

                        Material type;
                        type = snapshot.getBlockType(x, worldY, z);
                        if (type == null) type = Material.AIR;

                        short legacyId = LegacyBlockIdManager.getLegacyShort(type);
                        out.writeShort(legacyId);
                    }
                }
            }
            return bout.toByteArray();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
@@ -416,4 +460,44 @@

    private record ChunkSectionKey(UUID playerId, String worldName, int cx, int cz, int sectionY) {
    }



    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        sendBlockUpdateToNearby(block.getLocation(), Material.AIR);
        //sendChunkSectionIfBelowY0(event.getPlayer(), block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        sendBlockUpdateToNearby(block.getLocation(), block.getType());
        //sendChunkSectionIfBelowY0(event.getPlayer(), block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        sendBlockUpdateToNearby(block.getLocation(), block.getType());
        //sendChunkSectionIfBelowY0(null, block);
    }

    private void sendChunkSectionIfBelowY0(Player player, Block block) {
        if (block.getY() >= 0) return; 

        int sectionY = block.getY() >> 4;
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
