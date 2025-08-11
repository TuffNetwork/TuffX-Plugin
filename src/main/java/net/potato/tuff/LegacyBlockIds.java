package net.potato.tuff;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

//I forced chatgpt to generate this for me
// I made gemini explain some stuff to me - coleis1op
public class LegacyBlockIds {
    public static final Map<String, Integer> BLOCK_ID_MAP = new HashMap<>();
    // Corresponding metadata (0–15) where needed
    public static final Map<String, Integer> BLOCK_META_MAP = new HashMap<>();

    private static final Set<String> unmapped = new HashSet<>();

    static {
        // —— 0–15 —— basic blocks
        put("air",                     0,   0);
        put("stone",                   1,   0);
        put("deepslate", 1, 0);
        put("deepslate_tuff", 1, 0);
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
        BLOCK_ID_MAP.put("magma", 87);
        BLOCK_ID_MAP.put("bone_block", 155); 
        BLOCK_META_MAP.put("bone_block", 0);
        put("flower_pot_item", 140, 0);
        BLOCK_ID_MAP.put("bed_block", 26);
        BLOCK_META_MAP.put("bed_block", 0);
        BLOCK_ID_MAP.put("red_glazed_terracotta", 159);
        BLOCK_META_MAP.put("red_glazed_terracotta", 14);
        put("wood",                  5,   0);
        BLOCK_ID_MAP.put("planks_spruce", 5);
        BLOCK_META_MAP.put("planks_spruce", 1);

        BLOCK_ID_MAP.put("planks_birch", 5);
        BLOCK_META_MAP.put("planks_birch", 2);

        BLOCK_ID_MAP.put("planks_jungle", 5);
        BLOCK_META_MAP.put("planks_jungle", 3);

        BLOCK_ID_MAP.put("planks_acacia", 5);
        BLOCK_META_MAP.put("planks_acacia", 4);

        BLOCK_ID_MAP.put("planks_dark_oak", 5);
        BLOCK_META_MAP.put("planks_dark_oak", 5);
        
        put("sapling",                 6,   0); // meta: 0=oak,1=spruce,2=birch,3=jungle,4=acacia,5=dark_oak
        put("bedrock",                 7,   0);
        put("flowing_water",           8,   0);
        put("water",                   9,   0);
        put("flowing_lava",           10,   0);
        put("lava",                   11,   0);
        put("sand",                   12,   0); // meta: 0=sand,1=red_sand
        BLOCK_META_MAP.put("sand_red", 12);       // meta 1
        put("gravel",                 13,   0);
        put("gold_ore",               14,   0);
        put("iron_ore",               15,   0);

        // —— 16–35 —— wood & stone variants
        put("coal_ore",               16,   0);
        put("log",                    17,   0); // meta: 0=oak,1=spruce,2=birch,3=jungle
        BLOCK_META_MAP.put("log_spruce",17);
        BLOCK_META_MAP.put("log_birch", 17);
        BLOCK_META_MAP.put("log_jungle",17);

        put("leaves",                 18,   0); // meta: 0=oak,1=spruce,2=birch,3=jungle
        BLOCK_META_MAP.put("leaves_spruce",18);
        BLOCK_META_MAP.put("leaves_birch", 18);
        BLOCK_META_MAP.put("leaves_jungle",18);

        put("sponge",                 19,   0); // meta 1 = wet
        put("glass",                  20,   0);
        put("lapis_ore",              21,   0);
        put("lapis_block",            22,   0);
        put("dispenser",              23,   0);
        put("sandstone",              24,   0); // meta: 0=normal,1=chiseled,2=cut
        BLOCK_META_MAP.put("sandstone_chiseled", 24);
        BLOCK_META_MAP.put("sandstone_cut",     24);

        put("noteblock",              25,   0);
        put("bed",                    26,   0);
        put("golden_rail",            27,   0);
        put("detector_rail",          28,   0);
        put("sticky_piston",          29,   0);
        put("web",                    30,   0);
        put("tallgrass",              31,   2); // meta 1=fern,2=grass
        put("deadbush",               32,   0);
        put("piston",                 33,   0);
        put("piston_head",                 34,   0);
        put("wool",                   35,   0); // meta 0=white …15=black
        put("piston_extension", 36, 0);
        // —— 36–59 —— decoration & ores
        put("yellow_flower",          37,   0);
        put("red_flower",             38,   0); // meta 0=dandelion,1=poppy… etc.
        put("brown_mushroom",         39,   0);
        put("red_mushroom",           40,   0);
        put("gold_block",             41,   0);
        put("iron_block",             42,   0);
        put("double_stone_slab",      43,   0); // meta: various stone slab types
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

        // —— 60–80 —— functional blocks
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

        // —— 81–100 —— natural & craft blocks
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
        put("stained_glass",          95,   0); // meta 0–15 colored glass
        put("trapdoor",               96,   0);
        put("monster_egg",            97,   0);
        put("stone_brick",            98,   0);
        put("brown_mushroom_block",   99,   0);
        put("red_mushroom_block",    100,   0);

        // —— 101–120 —— iron & panes & fruit
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

        // —— 121–140 —— end & redstone lamps
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

        // —— 141–160 —— crops & comparators & banners
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
        put("quartz_block",         155,    0); // meta: 0=default,1=chiseled,2=lines
        BLOCK_META_MAP.put("quartz_block_chiseled", 155);
        BLOCK_META_MAP.put("quartz_block_lines",    155);

        put("quartz_stairs",        156,    0);
        put("activator_rail",       157,    0);
        put("dropper",              158,    0);
        put("stained_hardened_clay",159,    0); // meta 0–15
        put("stained_glass_pane",   160,    0); // meta 0–15

        // —— 161–192 —— newer fence/doors remapped
        put("leaves2",              161,    0);
        put("log2",                 162,    0);
        // acacia stairs → jungle_stairs (136)
        BLOCK_ID_MAP.put("acacia_stairs", 136); BLOCK_META_MAP.put("acacia_stairs", 0);
        // dark_oak stairs → spruce_stairs (134)
        BLOCK_ID_MAP.put("dark_oak_stairs", 134); BLOCK_META_MAP.put("dark_oak_stairs", 0);

        // wood gates/fences → map to oak variants
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

        // —— 193–211 —— 1.13+ → lookalikes
        // end_rod → torch (50)
        BLOCK_ID_MAP.put("end_rod",        50);  
        // chorus → deadbush (32)
        BLOCK_ID_MAP.put("chorus_plant",   32);
        BLOCK_ID_MAP.put("chorus_flower",  32);
        // purpur → quartz_block/stairs/slabs
        BLOCK_ID_MAP.put("purpur_block",   155);
        BLOCK_ID_MAP.put("purpur_pillar",  155);
        BLOCK_ID_MAP.put("purpur_stairs",  156);
        BLOCK_ID_MAP.put("purpur_double_slab", 43);
        BLOCK_ID_MAP.put("purpur_slab",       44);
        // sea lantern → glowstone (89)
        BLOCK_ID_MAP.put("sea_lantern",    89);
        // hay_block → pumpkin (86)
        BLOCK_ID_MAP.put("hay_block",       86);
        // carpet → wool (35)
        BLOCK_ID_MAP.put("carpet",          35);
        // hardened_clay → stained_hardened_clay (159)
        BLOCK_ID_MAP.put("hardened_clay", 159);
        // coal_block → coal_block (173)
        put("coal_block",        173,    0);
        // packed_ice → ice (79)
        BLOCK_ID_MAP.put("packed_ice",     79);
        // double_plant → tallgrass (31)
        BLOCK_ID_MAP.put("double_plant",   31);
        BLOCK_ID_MAP.put("long_grass",   31);
        // banners → standing_sign (63)
        BLOCK_ID_MAP.put("standing_banner",63);
        BLOCK_ID_MAP.put("wall_banner",    68);
        // red_sandstone → sandstone (24) meta 1
        BLOCK_ID_MAP.put("red_sandstone",    24); BLOCK_META_MAP.put("red_sandstone", 1);
        BLOCK_ID_MAP.put("red_sandstone_stairs",   24); BLOCK_META_MAP.put("red_sandstone_stairs", 1);
        // stone_slab2 (sandstone double slab) → double_stone_slab (43)
        BLOCK_ID_MAP.put("double_stone_slab2", 43);
        // stone_slab2 (sandstone slab) → stone_slab (44)
        BLOCK_ID_MAP.put("stone_slab2",      44);

        // sea_pickle → red_flower (38) meta 8
        BLOCK_ID_MAP.put("sea_pickle",       38); BLOCK_META_MAP.put("sea_pickle", 8);
        // grass_path → dirt (3)
        BLOCK_ID_MAP.put("grass_path",       3);
        // end_bricks → stone_brick (98)
        BLOCK_ID_MAP.put("end_bricks",       98);
        // beetroots → carrots (141)
        BLOCK_ID_MAP.put("beetroots",       141);
        // stonecutter → crafting_table (58)
        BLOCK_ID_MAP.put("stonecutter",      58);
        // glow_lichen → glowstone (89)
        BLOCK_ID_MAP.put("glow_lichen",      89);
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
