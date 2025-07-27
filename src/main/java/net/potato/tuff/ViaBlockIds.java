package net.potato.tuff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ViaBlockIds {
    private final JavaPlugin plugin;
    private final String serverVersion;
    private final File mappingsFile;
    private Map<String, int[]> legacyMap = new LinkedHashMap<>();

    public ViaBlockIds(JavaPlugin plugin) {
        this.plugin = plugin;
        this.serverVersion = getServerMinecraftVersion();
        this.mappingsFile = new File(plugin.getDataFolder(), serverVersion + "-mappings.json");

        Bukkit.getLogger().info("[TuffX] Server Minecraft Version: " + serverVersion);

        new BukkitRunnable() {
            @Override
            public void run() {
                initializeMappings();
            }
        }.runTaskLater(plugin, 1L);
    }

    private void initializeMappings() {
        if (Via.getAPI() == null) {
            Bukkit.getLogger().severe("[TuffX] ViaVersion API not found! Is ViaVersion installed?");
            return;
        }

        if (!mappingsFile.exists()) {
            Bukkit.getLogger().info("[TuffX] Mapping file not found, generating...");
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            generateAndSaveMappings(mappingsFile);
        } else {
            Bukkit.getLogger().info("[TuffX] Loading mappings from " + mappingsFile.getName());
            loadMappings(mappingsFile);
        }
    }

    public int[] toLegacy(String blockStateKey) {
        return legacyMap.getOrDefault(blockStateKey, new int[]{1, 0});
    }

    public int[] toLegacy(Block block) {
        String blockKey = block.getBlockData().getAsString().replace("minecraft:", "");
        return legacyMap.getOrDefault(blockKey, new int[]{1, 0});
    }

    private String getServerMinecraftVersion() {
        String versionString = Bukkit.getServer().getVersion();
        int mcIndex = versionString.indexOf("MC: ");
        if (mcIndex != -1) {
            int endIndex = versionString.indexOf(')', mcIndex);
            return endIndex != -1 ? versionString.substring(mcIndex + 4, endIndex) : versionString.substring(mcIndex + 4);
        }
        Bukkit.getLogger().warning("[TuffX] Could not detect Minecraft version. Defaulting to 1.21.");
        return "1.21";
    }

    private void generateAndSaveMappings(File file) {
        try (InputStream is = plugin.getResource("mapping-" + serverVersion + ".json")) {
            if (is == null) {
                Bukkit.getLogger().severe("[TuffX] Failed to find mapping-" + serverVersion + ".json in plugin resources!");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> root = mapper.readValue(is, Map.class);
            List<String> states = (List<String>) root.get("blockstates");

            if (states == null) {
                Bukkit.getLogger().severe("[TuffX] 'blockstates' key not found in JSON.");
                return;
            }

            Map<String, int[]> newLegacyMap = new LinkedHashMap<>();
            Bukkit.getLogger().info("[TuffX] Generating legacy mappings for " + states.size() + " block states...");

            for (int i = 0; i < states.size(); i++) {
                String key = states.get(i).replace("minecraft:", "");
                int[] legacy;
                legacy = convertToLegacy(i);
                
                newLegacyMap.put(key, legacy);
            }

            this.legacyMap = newLegacyMap;

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("blockstates", this.legacyMap);

            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, output);
            Bukkit.getLogger().info("[TuffX] Successfully wrote mappings to " + file.getName());

        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[TuffX] Error generating legacy mappings.", e);
        }
    }

    private void loadMappings(File file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> root = mapper.readValue(file, Map.class);
            Map<String, List<Integer>> rawMap = (Map<String, List<Integer>>) root.get("blockstates");

            if (rawMap == null) {
                Bukkit.getLogger().severe("[TuffX] Invalid format in mappings file. Regenerating...");
                generateAndSaveMappings(file);
                return;
            }

            legacyMap = new LinkedHashMap<>();
            for (Map.Entry<String, List<Integer>> entry : rawMap.entrySet()) {
                List<Integer> legacyList = entry.getValue();
                if (legacyList != null && legacyList.size() == 2) {
                    legacyMap.put(entry.getKey(), new int[]{legacyList.get(0), legacyList.get(1)});
                }
            }
            Bukkit.getLogger().info("[TuffX] Loaded " + legacyMap.size() + " legacy mappings.");
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[TuffX] Failed to load mappings file.", e);
        }
    }

    public int[] convertToLegacy(int modernBlockStateId) {
        ProtocolVersion serverProtocol = Via.getAPI().getServerVersion().highestSupportedProtocolVersion();
        ProtocolVersion clientProtocol = ProtocolVersion.v1_12_2;

        List<ProtocolPathEntry> path = Via.getManager()
            .getProtocolManager()
            .getProtocolPath(
                clientProtocol.getVersion(),
                serverProtocol.getVersion()
            );

        if (path == null) {
            if (debug) Bukkit.getLogger().warning("[TuffX] Protocol path came back as NULL. Is ViaBackwards installed?");
            return new int[]{1, 0};
        }
        
        int currentStateId = modernBlockStateId;

        for (int i = path.size() - 1; i >= 0; i--) {
            ProtocolPathEntry entry = path.get(i);
            Protocol protocol = entry.protocol();

            if (protocol instanceof BackwardsProtocol) {
                BackwardsMappingData mappingData = ((BackwardsProtocol) protocol).getMappingData();
                if (mappingData != null && mappingData.getBlockStateMappings() != null) {
                    int newid = mappingData.getBlockStateMappings().getNewId(currentStateId);

                    if (newid != -1) {
                        currentStateId = newid;
                    }
                }
            }
        }

        int blockId = currentStateId >> 4;
        int meta = currentStateId & 0xF;

        return new int[]{blockId, meta};
    }
}