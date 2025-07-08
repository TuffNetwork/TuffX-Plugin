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

public class TuffX extends JavaPlugin implements Listener, PluginMessageListener, TabCompleter {

    public static final String CHANNEL = "eagler:below_y0";
    private final Set<ChunkSectionKey> sentSections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean isMuted = false;

    @Override
    public void onEnable() {
        LegacyBlockIdManager.initialize(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("tuffx").setExecutor(this);
        this.getCommand("tuffx").setTabCompleter(this);
        logFancyEnable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("tuffx")) {
            return false;
        }

        if (args.length == 0) {
            handleHelpCommand(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "mute":
                handleMuteCommand(sender);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "reloadchunks":
                handleReloadChunksCommand(sender);
                break;
            case "help":
                handleHelpCommand(sender);
                break;
            default:
                sender.sendMessage(ChatColor.GOLD + "[TuffX] " + ChatColor.RED + "Unknown subcommand. Use /tuffx help.");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("tuffx")) {
            if (args.length == 1) {
                List<String> subcommands = new ArrayList<>();
                if (sender.hasPermission("tuffx.mute")) subcommands.add("mute");
                if (sender.hasPermission("tuffx.reload")) subcommands.add("reload");
                if (sender.hasPermission("tuffx.reloadchunks")) subcommands.add("reloadchunks");
                if (sender.hasPermission("tuffx.help")) subcommands.add("help");

                return subcommands.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private void handleMuteCommand(CommandSender sender) {
        if (!sender.hasPermission("tuffx.mute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }
        isMuted = !isMuted;
        String status = isMuted ? ChatColor.RED + "MUTED" : ChatColor.GREEN + "UNMUTED";
        sender.sendMessage(ChatColor.GOLD + "[TuffX] " + ChatColor.YELLOW + "Console output is now " + status + ".");
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("tuffx.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "[TuffX] " + ChatColor.YELLOW + "Reloading TuffX...");
        onDisable();
        onEnable();
        sender.sendMessage(ChatColor.GOLD + "[TuffX] " + ChatColor.GREEN + "Reload complete.");
    }

    private void handleReloadChunksCommand(CommandSender sender) {
        if (!sender.hasPermission("tuffx.reloadchunks")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "[TuffX] " + ChatColor.YELLOW + "Clearing chunk cache and resending to all players...");
        sentSections.clear();

        for (Player player : getServer().getOnlinePlayers()) {
            Chunk playerChunk = player.getLocation().getChunk();
            int viewDistance = getServer().getViewDistance();
            World world = player.getWorld();

            for (int x = -viewDistance; x <= viewDistance; x++) {
                for (int z = -viewDistance; z <= viewDistance; z++) {
                    if (world.isChunkLoaded(playerChunk.getX() + x, playerChunk.getZ() + z)) {
                        Chunk chunk = world.getChunkAt(playerChunk.getX() + x, playerChunk.getZ() + z);
                        sendChunkSectionsAsync(player, chunk);
                    }
                }
            }
        }
        sender.sendMessage(ChatColor.GOLD + "[TuffX] " + ChatColor.GREEN + "Chunk reload initiated for all online players. Reload is complete.");
    }

    private void handleHelpCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- TuffX Commands ---");
        if (sender.hasPermission("tuffx.help")) {
            sender.sendMessage(ChatColor.YELLOW + "/tuffx help" + ChatColor.GRAY + " - Shows this help message.");
        }
        if (sender.hasPermission("tuffx.mute")) {
            sender.sendMessage(ChatColor.YELLOW + "/tuffx mute" + ChatColor.GRAY + " - Toggles console logging for the plugin.");
        }
        if (sender.hasPermission("tuffx.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/tuffx reload" + ChatColor.GRAY + " - Reloads the TuffX plugin.");
        }
        if (sender.hasPermission("tuffx.reloadchunks")) {
            sender.sendMessage(ChatColor.YELLOW + "/tuffx reloadchunks" + ChatColor.GRAY + " - Resends below y0 chunks to players.");
        }
        sender.sendMessage(ChatColor.GOLD + "----------------------");
    }

    private void logFancyEnable() {
        if (isMuted) return;
        getLogger().info("");
        getLogger().info("████████╗██╗   ██╗███████╗ ███████╗ ██╗  ██╗");
        getLogger().info("╚══██╔══╝██║   ██║██╔════╝ ██╔════╝ ╚██╗██╔╝");
        getLogger().info("   ██║   ██║   ██║██████╗  ██████╗   ╚███╔╝ ");
        getLogger().info("   ██║   ██║   ██║██╔═══╝  ██╔═══╝   ██╔██╗ ");
        getLogger().info("   ██║   ╚██████╔╝██║      ██║      ██╔╝╚██╗");
        getLogger().info("   ╚═╝    ╚═════╝ ╚═╝      ╚═╝      ╚═╝  ╚═╝");
        getLogger().info("");
        getLogger().info("Below y0 and TuffX programmed by Potato");
        getLogger().info("Edited by coleis1op");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        sentSections.clear();
        if (!isMuted) getLogger().info("TuffX disabled");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        try (ByteArrayInputStream bin = new ByteArrayInputStream(message); DataInputStream in = new DataInputStream(bin)) {
            int x = in.readInt();
            int y = in.readInt();
            int z = in.readInt();
            int actionLength = in.readUnsignedByte();
            byte[] actionBytes = new byte[actionLength];
            in.readFully(actionBytes);
            String action = new String(actionBytes, StandardCharsets.UTF_8);

            new BukkitRunnable() {
                @Override
                public void run() {
                    handleInteraction(player, new Location(player.getWorld(), x, y, z), action);
                }
            }.runTask(this);
        } catch (IOException e) {
            if (!isMuted)
                getLogger().warning("Failed to parse plugin message from " + player.getName() + ": " + e.getMessage());
        }
    }

    private void handleInteraction(Player player, Location loc, String action) {
        World world = loc.getWorld();
        Block block = world.getBlockAt(loc);

        switch (action.toLowerCase()) {
            case "break":
                getLogger().info("breaking block at "+loc.getX()+","+loc.getY()+","+loc.getZ());
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    block.breakNaturally();
                } else {
                    block.setType(Material.AIR);
                }
                sendBlockUpdateToNearby(block.getLocation(), Material.AIR);
                break;

            case "use_on_block":
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, itemInHand, block, block.getFace(player.getLocation().getBlock()));
                getServer().getPluginManager().callEvent(event);

                sendBlockUpdateToNearby(block.getLocation(), world.getBlockAt(loc).getType());
                break;

            case "dig_start_destroy_block":
            case "dig_abort_destroy_block":
                break;

            default:
                if (!isMuted) getLogger().info("Received unhandled action '" + action + "' from " + player.getName());
                break;
        }
    }

    private void sendBlockUpdateToNearby(Location loc, Material newMaterial) {
        byte[] payload = createBlockUpdatePayload(loc, newMaterial);
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) < 4096) {
                p.sendPluginMessage(this, CHANNEL, payload);
            }
        }
    }

    private byte[] createBlockUpdatePayload(Location loc, Material newMaterial) {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("block_update");
            out.writeInt(loc.getBlockX());
            out.writeInt(loc.getBlockY());
            out.writeInt(loc.getBlockZ());
            out.writeShort(LegacyBlockIdManager.getLegacyShort(newMaterial));
            return bout.toByteArray();
        } catch (IOException e) {
            if (!isMuted) getLogger().severe("Failed to create block update payload! " + e.getMessage());
            return new byte[0];
        }
    }

    private byte[] createWelcomePayload(String message, int someNumber) {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bout)) {

            out.writeUTF("welcome_data");
            out.writeUTF(message);
            out.writeInt(someNumber);

            return bout.toByteArray();

        } catch (IOException e) {
            if (!isMuted) getLogger().severe("Failed to create welcome payload! " + e.getMessage());
            return new byte[0];
        }
    }

    private byte[] createSectionPayload(World world, int cx, int cz, int sectionY) throws IOException {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream(8200); DataOutputStream out = new DataOutputStream(bout)) {
            out.writeUTF("chunk_data");
            out.writeInt(cx);
            out.writeInt(cz);
            out.writeInt(sectionY);

            int baseY = sectionY * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int worldX = (cx << 4) + x;
                        int worldY = baseY + y;
                        int worldZ = (cz << 4) + z;
                        Material type = world.getBlockAt(worldX, worldY, worldZ).getType();
                        short legacyId = LegacyBlockIdManager.getLegacyShort(type);
                        out.writeShort(legacyId);
                    }
                }
            }
            return bout.toByteArray();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.UNKNOWN && event.getFrom().getY() < 0) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE && event.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        sentSections.removeIf(key -> key.playerId().equals(playerId));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sentSections.removeIf(key -> key.playerId().equals(player.getUniqueId()));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                String welcomeMessage = "§bWelcome to the server, §e" + player.getName() + "§b!";
                int playersOnline = getServer().getOnlinePlayers().size();
                byte[] welcomePayload = createWelcomePayload(welcomeMessage, playersOnline);
                if (welcomePayload.length > 0) {
                    player.sendPluginMessage(TuffX.this, CHANNEL, welcomePayload);
                }

                Chunk playerChunk = player.getLocation().getChunk();
                int viewDistance = getServer().getViewDistance();
                World world = player.getWorld();

                for (int x = -viewDistance; x <= viewDistance; x++) {
                    for (int z = -viewDistance; z <= viewDistance; z++) {
                        if (world.isChunkLoaded(playerChunk.getX() + x, playerChunk.getZ() + z)) {
                            Chunk chunk = world.getChunkAt(playerChunk.getX() + x, playerChunk.getZ() + z);
                            sendChunkSectionsAsync(player, chunk);
                        }
                    }
                }
            }
        }.runTaskLater(this, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        for (Player player : chunk.getWorld().getPlayers()) {
            if (isPlayerInChunkRange(player, chunk, getServer().getViewDistance())) {
                sendChunkSectionsAsync(player, chunk);
            }
        }
    }

    private boolean isPlayerInChunkRange(Player player, Chunk chunk, int range) {
        int playerChunkX = player.getLocation().getChunk().getX();
        int playerChunkZ = player.getLocation().getChunk().getZ();
        return Math.abs(playerChunkX - chunk.getX()) <= range && Math.abs(playerChunkZ - chunk.getZ()) <= range;
    }

    private void sendChunkSectionsAsync(Player player, Chunk chunk) {
        new BukkitRunnable() {
            @Override
            public void run() {
                int cx = chunk.getX();
                int cz = chunk.getZ();
                UUID uid = player.getUniqueId();
                String worldName = chunk.getWorld().getName();
                World world = chunk.getWorld();

                for (int sectionY = -4; sectionY < 0; sectionY++) {
                    ChunkSectionKey key = new ChunkSectionKey(uid, worldName, cx, cz, sectionY);
                    if (sentSections.contains(key)) {
                        //getLogger().info("Skipping already-sent section: " + key);
                        continue;
                    }
                    try {
                        byte[] payload = createSectionPayload(world, cx, cz, sectionY);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    player.sendPluginMessage(TuffX.this, CHANNEL, payload);
                                    sentSections.add(key);
                                }
                            }
                        }.runTask(TuffX.this);
                    } catch (IOException e) {
                        if (!isMuted)
                            getLogger().severe("Failed to create payload for chunk section: " + key + " | " + e.getMessage());
                    }
                }
            }
        }.runTaskAsynchronously(this);
    }

    private record ChunkSectionKey(UUID playerId, String worldName, int cx, int cz, int sectionY) {
    }
}