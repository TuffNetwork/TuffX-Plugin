package net.potato.tuff;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LegacyBlockIds {
    public static final Map<String, Integer> BLOCK_ID_MAP = new HashMap<>();
    public static final Map<String, Integer> BLOCK_META_MAP = new HashMap<>();

    private static final Set<String> unmapped = new HashSet<>();

    static {
        put("air",                     0,   0);
        put("stone",                   1,   0);
        put("deepslate", 1, 0);
        put("tuff", 1, 0);
        put("deepslate_coal_ore", 16, 0);   
        put("deepslate_iron_ore", 15, 0);   
        put("deepslate_gold_ore", 14, 0);    
        put("deepslate_diamond_ore", 56, 0); 
        put("deepslate_redstone_ore", 73, 0); 
        put("deepslate_lapis_ore", 21, 0);
        put("deepslate_emerald_ore", 129, 0);
        put("grass",                   2,   0);
        put("dirt",                    3,   0);
        put("cobblestone",             4,   0);
        put("magma_block",            213,   0);
        put("bone_block",            155,   0);
        put("flower_pot_item",       140,   0);
        put("bed_block",              26,   0);
        put("red_glazed_terracotta", 159,  14);
        put("wood",                   5,   0);
        put("planks_spruce",          5,   1);
        put("planks_birch",           5,   2);
        put("planks_jungle",          5,   3);
        put("planks_acacia",          5,   4);
        put("planks_dark_oak",        5,   5);
        
        put("sapling",                 6,   0);
        put("bedrock",                 7,   0);
        put("flowing_water",           8,   0);
        put("water",                   9,   0);
        put("flowing_lava",           10,   0);
        put("lava",                   11,   0);
        put("sand",                   12,   0);
        put("red_sand",               12,   0);
        put("gravel",                 13,   0);
        put("gold_ore",               14,   0);
        put("iron_ore",               15,   0);

        put("coal_ore",               16,   0);
        put("log",                    17,   0);
        put("spruce_log",             17,   1);
        put("birch_log",              17,   2);
        put("jungle_log",             17,   3);

        put("leaves",                 18,   0);
        put("spruce_leaves",          18,   1);
        put("birch_leaves",           18,   2);
        put("jungle_leaves",          18,   3);

        put("sponge",                 19,   0);
        put("glass",                  20,   0);
        put("lapis_ore",              21,   0);
        put("lapis_block",            22,   0);
        put("dispenser",              23,   0);
        put("sandstone",              24,   0);
        put("chiseled_sandstone",     24,   1);
        put("cut_sandstone",          24,   2);

        put("noteblock",              25,   0);
        put("bed",                    26,   0);
        put("golden_rail",            27,   0);
        put("detector_rail",          28,   0);
        put("sticky_piston",          29,   0);
        put("web",                    30,   0);
        put("tallgrass",              31,   2);
        put("deadbush",               32,   0);
        put("piston",                 33,   0);
        put("piston_head",                 34,   0);
        put("wool",                   35,   0);
        put("piston_extension", 36, 0);
        put("yellow_flower",          37,   0);
        put("red_flower",             38,   0);
        put("brown_mushroom",         39,   0);
        put("red_mushroom",           40,   0);
        put("gold_block",             41,   0);
        put("iron_block",             42,   0);
        put("double_stone_slab",      43,   0);
        put("stone_slab",             44,   0);
        put("brick_block",            45,   0);
        put("tnt",                    46,   0);
        put("bookshelf",              47,   0);
        put("mossy_cobblestone",      48,   0);
        put("obsidian",               49,   0);
        put("torch",                  50,   0);
        put("fire",                   51,   0);
        put("mob_spawner",            52,   0);
        put("oak_stairs",             53,   0);
        put("chest",                  54,   0);
        put("redstone_wire",          55,   0);
        put("diamond_ore",            56,   0);
        put("diamond_block",          57,   0);
        put("crafting_table",         58,   0);
        put("wheat",                  59,   0);

        put("farmland",               60,   0);
        put("furnace",                61,   0);
        put("lit_furnace",            62,   0);
        put("standing_sign",          63,   0);
        put("wooden_door",            64,   0);
        put("ladder",                 65,   0);
        put("rail",                   66,   0);
        put("rails",                   66,   0);
        put("stone_stairs",           67,   0);
        put("wall_sign",              68,   0);
        put("lever",                  69,   0);
        put("stone_pressure_plate",   70,   0);
        put("iron_door",              71,   0);
        put("wooden_pressure_plate",  72,   0);
        put("redstone_ore",           73,   0);
        put("lit_redstone_ore",       74,   0);
        put("unlit_redstone_torch",   75,   0);
        put("redstone_torch",         76,   0);
        put("stone_button",           77,   0);
        put("snow_layer",             78,   0);
        put("ice",                    79,   0);
        put("snow",                   80,   0);

        put("cactus",                 81,   0);
        put("clay",                   82,   0);
        put("reeds",                  83,   0);
        put("jukebox",                84,   0);
        put("fence",                  85,   0);
        put("pumpkin",                86,   0);
        put("netherrack",             87,   0);
        put("soul_sand",              88,   0);
        put("glowstone",              89,   0);
        put("portal",                 90,   0);
        put("lit_pumpkin",            91,   0);
        put("cake",                   92,   0);
        put("unpowered_repeater",     93,   0);
        put("powered_repeater",       94,   0);
        put("stained_glass",          95,   0); // colored glass
        put("trapdoor",               96,   0);
        put("monster_egg",            97,   0);
        put("stone_brick",            98,   0);
        put("brown_mushroom_block",   99,   0);
        put("red_mushroom_block",    100,   0);

        put("iron_bars",             101,   0);
        put("glass_pane",            102,   0);
        put("melon_block",           103,   0);
        put("pumpkin_stem",          104,   0);
        put("melon_stem",            105,   0);
        put("vine",                  106,   0);
        put("fence_gate",            107,   0);
        put("brick_stairs",          108,   0);
        put("stone_brick_stairs",    109,   0);
        put("mycelium",             110,    0);
        put("waterlily",            111,    0);
        put("nether_brick",         112,    0);
        put("nether_brick_fence",   113,    0);
        put("nether_brick_stairs",  114,    0);
        put("nether_wart",          115,    0);
        put("enchanting_table",     116,    0);
        put("brewing_stand",        117,    0);
        put("cauldron",             118,    0);
        put("end_portal",           119,    0);
        put("end_portal_frame",     120,    0);

        put("end_stone",            121,    0);
        put("dragon_egg",           122,    0);
        put("redstone_lamp",        123,    0);
        put("lit_redstone_lamp",    124,    0);
        put("double_wooden_slab",   125,    0);
        put("wooden_slab",          126,    0);
        put("cocoa",                127,    0);
        put("sandstone_stairs",     128,    0);
        put("emerald_ore",          129,    0);
        put("ender_chest",          130,    0);
        put("tripwire_hook",        131,    0);
        put("tripwire",             132,    0);
        put("emerald_block",        133,    0);
        put("spruce_stairs",        134,    0);
        put("birch_stairs",         135,    0);
        put("jungle_stairs",        136,    0);
        put("command_block",        137,    0);
        put("beacon",               138,    0);
        put("cobblestone_wall",     139,    0);
        put("flower_pot",           140,    0);

        put("carrots",              141,    0);
        put("potatoes",             142,    0);
        put("wooden_button",        143,    0);
        put("wood_button",        143,    0);
        put("skull",                144,    0);
        put("anvil",                145,    0);
        put("trapped_chest",        146,    0);
        put("light_weighted_pressure_plate", 147, 0);
        put("heavy_weighted_pressure_plate", 148, 0);
        put("unpowered_comparator", 149,    0);
        put("powered_comparator",   150,    0);
        put("daylight_detector",    151,    0);
        put("redstone_block",       152,    0);
        put("quartz_ore",           153,    0);
        put("hopper",               154,    0);
        put("quartz_block",         155,    0);
        put("quartz_block_chiseled", 155, 1);
        put("quartz_block_lines", 155, 2);

        put("quartz_stairs",        156,    0);
        put("activator_rail",       157,    0);
        put("dropper",              158,    0);
        put("stained_hardened_clay",159,    0);
        put("stained_glass_pane",   160,    0);
        put("leaves2",              161,    0);
        put("log2",                 162,    0);
        put("acacia_stairs", 163, 0);
        put("dark_oak_stairs", 164, 0);

        put("spruce_fence_gate",    107,    0);
        put("birch_fence_gate",     107,    0);
        put("jungle_fence_gate",    107,    0);
        put("acacia_fence_gate",    107,    0);
        put("dark_oak_fence_gate",  107,    0);

        put("spruce_fence",         85,     0);
        put("birch_fence",          85,     0);
        put("jungle_fence",         85,     0);
        put("acacia_fence",         85,     0);
        put("dark_oak_fence",       85,     0);

        put("spruce_door",          64,     0);
        put("birch_door",           64,     0);
        put("jungle_door",          64,     0);
        put("acacia_door",          64,     0);
        put("dark_oak_door",        64,     0);

        put("end_rod", 50, 0);  
        put("chorus_plant", 32, 0);
        put("chorus_flower", 32, 1);
        put("purpur_block", 155, 0);
        put("purpur_pillar", 155, 1);
        put("purpur_stairs", 156, 0);
        put("purpur_double_slab", 43, 0);
        put("purpur_slab", 44, 0);
        put("sea_lantern", 89, 0);
        put("hay_block", 86, 0);
        put("carpet", 35, 0);
        put("hardened_clay", 159, 0);
        put("coal_block",        173,    0);
        put("packed_ice", 79, 0);
        put("double_plant", 31, 0);
        put("long_grass", 31, 0);
        put("standing_banner", 63, 0);
        put("wall_banner", 68, 0);
        put("red_sandstone", 24, 1);
        put("red_sandstone_stairs", 24, 1);
        put("double_stone_slab2", 43, 0);
        put("stone_slab2", 44, 0);

        put("sea_pickle", 38, 8);
        put("grass_path", 3, 0);
        put("end_bricks", 98, 0);
        put("beetroots", 141, 0);
        put("stonecutter", 58, 0);
        put("glow_lichen", 89, 0);
        
        // Additional modern blocks
        put("dripstone_block", 1, 0);
        put("pointed_dripstone", 39, 0);
        put("moss_block", 2, 0);
        put("moss_carpet", 35, 0);
        put("azalea", 6, 0);
        put("flowering_azalea", 38, 0);
        put("azalea_leaves", 18, 0);
        put("flowering_azalea_leaves", 18, 0);
        put("cave_vines", 106, 0);
        put("cave_vines_plant", 106, 0);
        put("glow_berries", 103, 0);
        put("big_dripleaf", 31, 0);
        put("small_dripleaf", 31, 0);
        put("hanging_roots", 106, 0);
        put("rooted_dirt", 3, 0);
        put("deepslate_bricks", 98, 0);
        put("cracked_deepslate_bricks", 98, 0);
        put("deepslate_tiles", 98, 0);
        put("cracked_deepslate_tiles", 98, 0);
        put("chiseled_deepslate", 98, 0);
        put("polished_deepslate", 1, 0);
        put("cobbled_deepslate", 4, 0);
        put("reinforced_deepslate", 49, 0);
        put("sculk", 1, 0);
        put("sculk_vein", 30, 0);
        put("sculk_catalyst", 52, 0);
        put("sculk_shrieker", 52, 0);
        put("sculk_sensor", 52, 0);
        put("calibrated_sculk_sensor", 52, 0);
    }

    private static void put(String name, int id, int meta) {
        BLOCK_ID_MAP.put(name, id);
        BLOCK_META_MAP.put(name, meta);
    }

    public static int getId(String name) {
        if (!BLOCK_ID_MAP.containsKey(name) && unmapped.add(name)) {
            System.out.println("Unmapped block: " + name);
        }
        return BLOCK_ID_MAP.getOrDefault(name, 1);
    }

    public static int getMeta(String name) {
        return BLOCK_META_MAP.getOrDefault(name, 0);
    }
}
