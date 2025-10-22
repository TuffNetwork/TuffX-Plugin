package net.potato.tuff;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.potato.tuff.TuffX;

public class ChunkPacketListener {

    private static boolean initialized = false;

    public static void initialize(TuffX plugin) {
        if (initialized) {
            return;
        }

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            plugin.getLogger().severe("Could not initialize ChunkPacketListener because ProtocolManager is null!");
            return;
        }

        protocolManager.addPacketListener(
            new PacketAdapter(plugin, PacketType.Play.Server.MAP_CHUNK) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    Player player = event.getPlayer();

                    if (!((TuffX) this.plugin).isPlayerReady(player)) {
                        return;
                    }

                    World world = player.getWorld();
                    int chunkX = event.getPacket().getIntegers().read(0);
                    int chunkZ = event.getPacket().getIntegers().read(1);
                    
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        if (player.isOnline() && world.isChunkLoaded(chunkX, chunkZ)) {
                            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                            ((TuffX) this.plugin).processAndSendChunk(player, chunk);
                        }
                    });
                }
            }
        );

        plugin.getLogger().info("ProtocolLib listener for chunks registered successfully.");
        initialized = true;
    }
}