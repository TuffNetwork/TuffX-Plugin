package net.potato.tuff;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class LegacyBlockIdManager {

    private static final short[] ID_CACHE = new short[Material.values().length];
    private static final Set<String> unmappedBlocks = new HashSet<>();
    private static boolean initialized = false;

    public static synchronized void initialize(Plugin plugin) {
        if (initialized) return;
        
        Material[] materials = Material.values();
        for (int i = 0; i < materials.length; i++) {
            Material material = materials[i];
            if (!material.isBlock()) {
                ID_CACHE[i] = 0;
                continue;
            }

            String blockName = material.name().toLowerCase();
            Integer id = LegacyBlockIds.BLOCK_ID_MAP.get(blockName);
            if (id == null) {
                id = 1;
                if (unmappedBlocks.add(blockName)) {
                    plugin.getLogger().warning("Unmapped block: " + blockName + ". Defaulting to stone (ID=1).");
                }
            }
            
            Integer meta = LegacyBlockIds.BLOCK_META_MAP.get(blockName);
            if (meta == null) meta = 0;
            
            ID_CACHE[i] = (short) ((id & 0xFFF) | ((meta & 0xF) << 12));
        }
        
        initialized = true;
        plugin.getLogger().info("Legacy Block ID cache initialized successfully.");
    }

    public static short getLegacyShort(Material material) {
        if (material == null) return 0;
        int ordinal = material.ordinal();
        if (ordinal >= 0 && ordinal < ID_CACHE.length) {
            return ID_CACHE[ordinal];
        }
        return 0;
    }
}
