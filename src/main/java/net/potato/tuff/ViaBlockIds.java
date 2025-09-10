package net.potato.tuff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.data.BlockData;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import it.unimi.dsi.fastutil.objects.*;

public class ViaBlockIds {
    private final JavaPlugin p;
    private final String sv;
    private final File mf;
    private Object2ObjectOpenHashMap<String, int[]> lm = new Object2ObjectOpenHashMap<>();

    public ViaBlockIds(JavaPlugin pl) {
        p = pl;
        sv = gsmv();
        mf = new File(pl.getDataFolder(), sv + "-mappings.json");

        Bukkit.getLogger().info("[TuffX] Server Minecraft Version: " + sv);

        new BukkitRunnable() {
            @Override
            public void run() {
                im();
            }
        }.runTaskLater(pl, 1L);
    }

    private void im() {
        if (Via.getAPI() == null) {
            Bukkit.getLogger().severe("[TuffX] ViaVersion API not found! Is ViaVersion installed?");
            return;
        }

        if (!mf.exists()) {
            Bukkit.getLogger().info("[TuffX] Mapping file not found, generating...");
            if (!p.getDataFolder().exists()) {
                p.getDataFolder().mkdirs();
            }
            gasm(mf);
        } else {
            Bukkit.getLogger().info("[TuffX] Loading mappings from " + mf.getName());
            lm(mf);
        }
    }

    private static final int[] DEFAULT_LEGACY = {1, 0};
    
    public int[] toLegacy(String k) {
        int[] result = lm.get(k);
        return result != null ? result : DEFAULT_LEGACY;
    }

    public int[] toLegacy(Block b) {
        String k = b.getBlockData().getAsString();
        if (k.startsWith("minecraft:")) {
            k = k.substring(10);
        }
        int[] result = lm.get(k);
        return result != null ? result : DEFAULT_LEGACY;
    }

    public int[] toLegacy(BlockData bd) {
        String k = bd.getAsString();
        if (k.startsWith("minecraft:")) {
            k = k.substring(10);
        }
        int[] result = lm.get(k);
        return result != null ? result : DEFAULT_LEGACY;
    }

    private String gsmv() {
        String vs = Bukkit.getServer().getVersion();
        int mi = vs.indexOf("MC: ");
        if (mi != -1) {
            int ei = vs.indexOf(')', mi);
            return ei != -1 ? vs.substring(mi + 4, ei) : vs.substring(mi + 4);
        }
        Bukkit.getLogger().warning("[TuffX] Could not detect Minecraft version. Defaulting to 1.21.");
        return "1.21";
    }

    private InputStream fmf() {
        String[] vp = sv.split("\\.");
        
        int maj, min, pat;

        try {
            maj = Integer.parseInt(vp[0]);
            min = Integer.parseInt(vp[1]);
            pat = vp.length > 2 ? Integer.parseInt(vp[2]) : 0;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Bukkit.getLogger().severe("[TuffX] Could not parse server version string: " + sv);
            return p.getResource("mapping-" + sv + ".json");
        }

        Bukkit.getLogger().info("[TuffX] Searching for mappings, starting from " + sv + " and going down.");

        for (int m = min; m >= 16; m--) { 
            int sp = (m == min) ? pat : 9;

            for (int pt = sp; pt >= 0; pt--) {
                String vtt = maj + "." + m + "." + pt;
                String fn = "mapping-" + vtt + ".json";
                
                InputStream is = p.getResource(fn);

                if (is != null) {
                    if (!vtt.equals(sv)) {
                        Bukkit.getLogger().info("[TuffX] Using fallback mapping file: " + fn);
                    } else {
                        Bukkit.getLogger().info("[TuffX] Found exact mapping file: " + fn);
                    }
                    return is;
                }
            }
            
            String mvfn = "mapping-" + maj + "." + m + ".json";
            InputStream is = p.getResource(mvfn);
            if (is != null) {
                Bukkit.getLogger().info("[TuffX] Using fallback mapping file: " + mvfn);
                return is;
            }
        }

        Bukkit.getLogger().severe("[TuffX] Could not find any suitable mapping file after checking all versions down to " + maj + ".16.0");
        return null;
    }

    private void gasm(File f) {
        try (InputStream is = fmf()) {
            if (is == null) {
                Bukkit.getLogger().severe("[TuffX] Failed to find mapping-" + sv + ".json in plugin resources!");
                return;
            }

            ObjectMapper m = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> r = m.readValue(is, Map.class);
            @SuppressWarnings("unchecked")
            List<String> s = (List<String>) r.get("blockstates");

            if (s == null) {
                Bukkit.getLogger().severe("[TuffX] 'blockstates' key not found in JSON.");
                return;
            }

            Object2ObjectOpenHashMap<String, int[]> nlm = new Object2ObjectOpenHashMap<>();
            Bukkit.getLogger().info("[TuffX] Generating legacy mappings for " + s.size() + " block states...");

            for (int i = 0; i < s.size(); i++) {
                String k = s.get(i).replace("minecraft:", "");
                int[] l = ctl(i);
                nlm.put(k, l);
            }

            lm = nlm;

            Map<String, Object> o = new Object2ObjectOpenHashMap<>();
            o.put("blockstates", lm);

            f.getParentFile().mkdirs();
            m.writerWithDefaultPrettyPrinter().writeValue(f, o);
            Bukkit.getLogger().info("[TuffX] Successfully wrote mappings to " + f.getName());

        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[TuffX] Error generating legacy mappings.", e);
        }
    }

    private void lm(File f) {
        try {
            ObjectMapper m = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> r = m.readValue(f, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, List<Integer>> rm = (Map<String, List<Integer>>) r.get("blockstates");

            if (rm == null) {
                Bukkit.getLogger().severe("[TuffX] Invalid format in mappings file. Regenerating...");
                gasm(f);
                return;
            }

            lm = new Object2ObjectOpenHashMap<>();
            for (Map.Entry<String, List<Integer>> e : rm.entrySet()) {
                List<Integer> ll = e.getValue();
                if (ll != null && ll.size() == 2) {
                    lm.put(e.getKey(), new int[]{ll.get(0), ll.get(1)});
                }
            }
            Bukkit.getLogger().info("[TuffX] Loaded " + lm.size() + " legacy mappings.");
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[TuffX] Failed to load mappings file.", e);
        }
    }

    public int[] ctl(int mbsi) {
        ProtocolVersion sp = Via.getAPI().getServerVersion().highestSupportedProtocolVersion();
        ProtocolVersion cp = ProtocolVersion.v1_12_2;

        List<ProtocolPathEntry> pt = Via.getManager()
            .getProtocolManager()
            .getProtocolPath(
                cp.getVersion(),
                sp.getVersion()
            );

        if (pt == null) {
            Bukkit.getLogger().warning("[TuffX] Protocol path is null!");
            return new int[]{1, 0};
        }
        
        int csi = mbsi;

        for (int i = pt.size() - 1; i >= 0; i--) {
            ProtocolPathEntry e = pt.get(i);
            Protocol pr = e.protocol();

            if (pr instanceof BackwardsProtocol) {
                BackwardsMappingData md = ((BackwardsProtocol) pr).getMappingData();
                if (md != null && md.getBlockStateMappings() != null) {
                    int ni = md.getBlockStateMappings().getNewId(csi);

                    if (ni != -1) {
                        csi = ni;
                    }
                }
            }
        }

        int bi = csi >> 4;
        int mt = csi & 0xF;

        return new int[]{bi, mt};
    }
}
