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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.common.cache.*;
import java.util.logging.Level;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.shorts.*;
import it.unimi.dsi.fastutil.bytes.*;

public class TuffX extends JavaPlugin implements Listener, PluginMessageListener {

  public static final String CH = "eagler:below_y0";
  public ViaBlockIds v;

  private final ObjectOpenHashSet<UUID> aib = new ObjectOpenHashSet<>();
  private ObjectOpenHashSet<String> ew;
  private Cache<WCK, ObjectArrayList<byte[]>> cc;
  private boolean d;
  private ExecutorService cp;

  private final ThreadLocal<Object2ObjectOpenHashMap<BlockData, int[]>> tlcc = ThreadLocal
      .withInitial(() -> new Object2ObjectOpenHashMap<>(256));
  private final ThreadLocal<ShortArrayList> tlba = ThreadLocal.withInitial(() -> new ShortArrayList(4096));
  private final ThreadLocal<ByteArrayList> tlla = ThreadLocal.withInitial(() -> new ByteArrayList(4096));
  private final ThreadLocal<ByteArrayOutputStream> tlos = ThreadLocal
      .withInitial(() -> new ByteArrayOutputStream(8256));

  private static final int[] EMPTY_LEGACY = { 1, 0 };

  private void ld(String m) {
    if (d)
      getLogger().log(Level.INFO, "[TuffX-Debug] " + m);
  }

  public record WCK(String w, int x, int z) {
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    d = getConfig().getBoolean("debug-mode", false);
    ObjectArrayList<String> ewList = new ObjectArrayList<>(getConfig().getStringList("enabled-worlds"));
    ew = new ObjectOpenHashSet<>(ewList.size());
    ew.addAll(ewList);

    Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
    if (protocolLib == null) {
      getLogger().severe("ProtocolLib not found! This plugin is required for proactive chunk sending.");
      getLogger().severe("TuffX will not be able to send below y=0 data.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    ChunkPacketListener.initialize(this);

    cc = CacheBuilder.newBuilder()
        .maximumSize(getConfig().getInt("cache-size", 1024))
        .expireAfterAccess(getConfig().getInt("cache-expiration", 5), TimeUnit.MINUTES)
        .concurrencyLevel(Runtime.getRuntime().availableProcessors())
        .initialCapacity(256)
        .build();

    getServer().getMessenger().registerOutgoingPluginChannel(this, CH);
    getServer().getMessenger().registerIncomingPluginChannel(this, CH, this);
    getServer().getPluginManager().registerEvents(this, this);
    if (v == null)
      v = new ViaBlockIds(this);
    lfe();

    int ct = getConfig().getInt("chunk-processor-threads", -1);
    int tc;
    if (ct <= 0) {
      tc = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    } else {
      tc = ct;
    }

    cp = Executors.newFixedThreadPool(tc, r -> {
      Thread t = new Thread(r, "TuffX-Chunk-" + System.nanoTime());
      t.setDaemon(true);
      t.setPriority(Thread.NORM_PRIORITY + 1);
      return t;
    });
  }

  public record CSC(int x, int y, int z) {
  }

  @Override
  public void onDisable() {
    if (cp != null) {
      cp.shutdown();
      try {
        if (!cp.awaitTermination(10, TimeUnit.SECONDS)) {
          cp.shutdownNow();
          if (!cp.awaitTermination(5, TimeUnit.SECONDS)) {
            getLogger().severe("Failed to shutdown chunk processor pool!");
          }
        }
      } catch (InterruptedException e) {
        cp.shutdownNow();
        Thread.currentThread().interrupt();
      } finally {
        cp = null;
      }
    }

    if (cc != null) {
      cc.invalidateAll();
      cc = null;
    }

    aib.clear();

    if (v != null) {
      v = null;
    }
  }

  public boolean isPlayerReady(Player p) {
    if (p == null)
      return false;
    return aib.contains(p.getUniqueId());
  }

  @Override
  public void onPluginMessageReceived(String ch, Player p, byte[] m) {
    if (!ch.equals(CH) || !p.isOnline())
      return;
    try (DataInputStream i = new DataInputStream(new ByteArrayInputStream(m))) {
      int x = i.readInt();
      int y = i.readInt();
      int z = i.readInt();
      int al = i.readUnsignedByte();
      byte[] ab = new byte[al];
      i.readFully(ab);
      String a = new String(ab, StandardCharsets.UTF_8);
      hip(p, new Location(p.getWorld(), x, y, z), a);
    } catch (IOException e) {
      getLogger().warning("Failed to parse plugin message from " + p.getName() + ": " + e.getMessage());
    }
  }

  private void hip(Player p, Location l, String a) {
    if (!ew.contains(p.getWorld().getName()) && !a.equalsIgnoreCase("ready")) {
      p.sendPluginMessage(this, CH, cby0sp(false));
      return;
    }

    switch (a.toLowerCase()) {
      case "ready2":
        ld("Player " + p.getName() + " is READY.");
        aib.add(p.getUniqueId());
        if (ew.contains(p.getWorld().getName())) {
          aib.add(p.getUniqueId());
          p.sendPluginMessage(this, CH, cby0sp(true));
          resendChunksInView(p);
        } else {
          p.sendPluginMessage(this, CH, cby0sp(false));
        }
        break;
      case "use_on_block":
        new BukkitRunnable() {
          @Override
          public void run() {
            Block b = l.getBlock();
            ItemStack it = p.getInventory().getItemInMainHand();
            getServer().getPluginManager().callEvent(
                new PlayerInteractEvent(p, Action.RIGHT_CLICK_BLOCK, it, b, b.getFace(p.getLocation().getBlock())));
          }
        }.runTask(this);
        break;
      case "ready":
        if (getConfig().getBoolean("kick-outdated-clients", true))
          p.kickPlayer(
              "§cYour client is not compatible with the version of §6TuffX §cthe server has installed!\n§7Please update your client.");
    }
  }

  public void resendChunksInView(Player p) {
    World world = p.getWorld();
    int viewDistance = p.getClientViewDistance();
    int playerChunkX = p.getLocation().getChunk().getX();
    int playerChunkZ = p.getLocation().getChunk().getZ();

    getServer().getScheduler().runTaskAsynchronously(this, () -> {
      for (int x = -viewDistance; x <= viewDistance; x++) {
        for (int z = -viewDistance; z <= viewDistance; z++) {
          int currentChunkX = playerChunkX + x;
          int currentChunkZ = playerChunkZ + z;

          if (world.isChunkLoaded(currentChunkX, currentChunkZ)) {
            Chunk chunk = world.getChunkAt(currentChunkX, currentChunkZ);
            processAndSendChunk(p, chunk);
          }
        }
      }
    });
  }

  private byte[] cby0sp(boolean s) {
    try (ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b)) {
      o.writeUTF("y0_status");
      o.writeBoolean(s);
      return b.toByteArray();
    } catch (IOException e) {
      return null;
    }
  }

  private byte[] cdp() {
    try (ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b)) {
      o.writeUTF("dimension_change");
      return b.toByteArray();
    } catch (IOException e) {
      return null;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
    Player p = e.getPlayer();
    p.sendPluginMessage(this, CH, cdp());
    boolean isEnabledWorld = ew.contains(p.getWorld().getName());
    p.sendPluginMessage(this, CH, cby0sp(isEnabledWorld));
    if (isPlayerReady(p) && isEnabledWorld) {
      resendChunksInView(p);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent e) {
    Player p = e.getPlayer();
    p.sendPluginMessage(this, CH, cdp());
    boolean isEnabledWorld = ew.contains(p.getWorld().getName());
    p.sendPluginMessage(this, CH, cby0sp(isEnabledWorld));
  }

  public void processAndSendChunk(final Player p, final Chunk c) {
    if (c == null || p == null || !p.isOnline() || cp == null || cp.isShutdown()) {
      return;
    }
    final WCK k = new WCK(c.getWorld().getName(), c.getX(), c.getZ());
    ObjectArrayList<byte[]> cachedData = cc.getIfPresent(k);
    if (cachedData != null) {
      if (p.isOnline()) {
        for (byte[] py : cachedData) {
          p.sendPluginMessage(TuffX.this, CH, py);
        }
      }
      return;
    }
    cp.submit(() -> {
      final ObjectArrayList<byte[]> pp = new ObjectArrayList<>(4);
      final ChunkSnapshot s = c.getChunkSnapshot(true, false, false);
      final Object2ObjectOpenHashMap<BlockData, int[]> cvt = tlcc.get();
      cvt.clear();
      for (int sy = -4; sy < 0; sy++) {
        if (!p.isOnline()) {
          return;
        }
        try {
          byte[] py = csp(s, c.getX(), c.getZ(), sy, cvt);
          if (py != null) {
            pp.add(py);
          }
        } catch (IOException e) {
          getLogger().severe("Payload creation failed for " + c.getX() + "," + c.getZ() + ": " + e.getMessage());
        }
      }
      TuffX.this.cc.put(k, pp);
      new BukkitRunnable() {
        @Override
        public void run() {
          if (p.isOnline()) {
            for (byte[] py : pp) {
              p.sendPluginMessage(TuffX.this, CH, py);
            }
          }
        }
      }.runTask(TuffX.this);
    });
  }

  private void icc(World w, int x, int z) {
    WCK k = new WCK(w.getName(), x >> 4, z >> 4);
    cc.invalidate(k);
  }

  private void cp(UUID id) {
    aib.remove(id);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    cp(e.getPlayer().getUniqueId());
  }

  private byte[] csp(ChunkSnapshot s, int x, int z, int sy, Object2ObjectOpenHashMap<BlockData, int[]> c)
      throws IOException {
    ShortArrayList ba = tlba.get();
    ByteArrayList la = tlla.get();
    ba.clear();
    la.clear();
    ba.ensureCapacity(4096);
    la.ensureCapacity(4096);

    boolean h = false;
    int by = sy << 4;

    for (int y = 0; y < 16; y++) {
      int wy = by + y;
      for (int zz = 0; zz < 16; zz++) {
        for (int xx = 0; xx < 16; xx++) {
          BlockData bd = s.getBlockData(xx, wy, zz);
          int[] ld = c.getOrDefault(bd, EMPTY_LEGACY);
          if (ld == EMPTY_LEGACY && v != null) {
            ld = v.toLegacy(bd);
            c.put(bd, ld);
          }

          short lb = (short) ((ld[1] << 12) | (ld[0] & 0xFFF));
          byte pl = (byte) ((s.getBlockSkyLight(xx, wy, zz) << 4) | s.getBlockEmittedLight(xx, wy, zz));

          ba.add(lb);
          la.add(pl);

          if (lb != 0 || pl != 0) {
            h = true;
          }
        }
      }
    }

    if (!h) {
      return null;
    }

    ByteArrayOutputStream b = tlos.get();
    b.reset();

    try (DataOutputStream o = new DataOutputStream(b)) {
      o.writeUTF("chunk_data");
      o.writeInt(x);
      o.writeInt(z);
      o.writeInt(sy);

      int sz = ba.size();
      for (int j = 0; j < sz; j++) {
        o.writeShort(ba.getShort(j));
        o.writeByte(la.getByte(j));
      }
      return b.toByteArray();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent e) {
    if (e.getBlock().getY() < 0) {
      hbc(e.getBlock().getLocation(), e.getBlock().getBlockData(), Material.AIR.createBlockData());
      icc(e.getBlock().getWorld(), e.getBlock().getX(), e.getBlock().getZ());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent e) {
    if (e.getBlock().getY() < 0) {
      hbc(e.getBlock().getLocation(), e.getBlockReplacedState().getBlockData(), e.getBlock().getBlockData());
      icc(e.getBlock().getWorld(), e.getBlock().getX(), e.getBlock().getZ());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPhysics(BlockPhysicsEvent e) {
    final Block b = e.getBlock();
    if (b.getY() < 0) {
      final Location l = b.getLocation();
      final World w = l.getWorld();
      new BukkitRunnable() {
        @Override
        public void run() {
          BlockData ud = w.getBlockData(l);
          ssbu(l, ud);
          icc(w, l.getBlockX(), l.getBlockZ());
        }
      }.runTask(this);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent e) {
    final ObjectOpenHashSet<WCK> ac = new ObjectOpenHashSet<>();
    final List<Block> btu = new ArrayList<>(e.blockList());
    new BukkitRunnable() {
      @Override
      public void run() {
        for (Block b : btu) {
          if (b.getY() < 0) {
            ssbu(b.getLocation(), Material.AIR.createBlockData());
            ac.add(new WCK(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4));
          }
        }
        if (!ac.isEmpty()) {
          ac.forEach(cc::invalidate);
        }
      }
    }.runTask(this);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockFromTo(BlockFromToEvent e) {
    final Block b = e.getToBlock();
    if (b.getY() < 0) {
      new BukkitRunnable() {
        @Override
        public void run() {
          ssbu(b.getLocation(), b.getBlockData());
          icc(b.getWorld(), b.getX(), b.getZ());
        }
      }.runTask(this);
    }
  }

  private void ssbu(Location l, BlockData d) {
    try (ByteArrayOutputStream b = new ByteArrayOutputStream(64);
        DataOutputStream o = new DataOutputStream(b)) {

      o.writeUTF("block_update");
      o.writeInt(l.getBlockX());
      o.writeInt(l.getBlockY());
      o.writeInt(l.getBlockZ());

      int[] ld = v.toLegacy(d);
      o.writeShort((short) ((ld[1] << 12) | (ld[0] & 0xFFF)));
      byte[] py = b.toByteArray();

      new BukkitRunnable() {
        @Override
        public void run() {
          for (Player p : l.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(l) < 4096) {
              p.sendPluginMessage(TuffX.this, CH, py);
            }
          }
        }
      }.runTask(this);

    } catch (IOException e) {
      getLogger().severe("Failed to create single block update payload: " + e.getMessage());
    }
  }

  private void hbc(Location l, BlockData od, BlockData nd) {
    ssbu(l, nd);
    boolean oe = od.getLightEmission() > 0;
    boolean ne = nd.getLightEmission() > 0;
    boolean oo = od.getMaterial().isOccluding();
    boolean no = nd.getMaterial().isOccluding();
    if (oe != ne || oo != no) {
      slu(l);
    }
  }

  private void slu(Location l) {
    ObjectOpenHashSet<CSC> stu = new ObjectOpenHashSet<>();
    World w = l.getWorld();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        for (int dz = -1; dz <= 1; dz++) {
          Location n = l.clone().add(dx, dy, dz);
          if (n.getY() < -64 || n.getY() >= 0)
            continue;

          stu.add(new CSC(
              n.getBlockX() >> 4,
              n.getBlockY() >> 4,
              n.getBlockZ() >> 4));
        }
      }
    }
    for (CSC sc : stu) {
      ChunkSnapshot s = w.getChunkAt(sc.x, sc.z).getChunkSnapshot(true, false, false);
      new BukkitRunnable() {
        @Override
        public void run() {
          try {
            byte[] py = clp(s, sc);
            new BukkitRunnable() {
              @Override
              public void run() {
                for (Player p : w.getPlayers()) {
                  if (p.getLocation().distanceSquared(l) < 4096) {
                    p.sendPluginMessage(TuffX.this, CH, py);
                  }
                }
              }
            }.runTask(TuffX.this);
          } catch (IOException e) {
            getLogger().severe("Failed to create lighting payload: " + e.getMessage());
          }
        }
      }.runTaskAsynchronously(this);
    }
  }

  private byte[] clp(ChunkSnapshot s, CSC sc) throws IOException {
    try (ByteArrayOutputStream b = new ByteArrayOutputStream(4120);
        DataOutputStream o = new DataOutputStream(b)) {
      o.writeUTF("lighting_update");
      o.writeInt(sc.x);
      o.writeInt(sc.z);
      o.writeInt(sc.y);

      byte[] ld = new byte[4096];
      int by = sc.y * 16;
      int i = 0;

      for (int y = 0; y < 16; y++) {
        for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++) {
            int wy = by + y;
            int bl = s.getBlockEmittedLight(x, wy, z);
            int sl = s.getBlockSkyLight(x, wy, z);
            ld[i++] = (byte) ((sl << 4) | bl);
          }
        }
      }
      o.write(ld);
      return b.toByteArray();
    }
  }

  private void lfe() {
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
