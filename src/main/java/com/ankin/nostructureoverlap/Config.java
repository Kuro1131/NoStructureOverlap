package com.ankin.nostructureoverlap;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = Nostructureoverlap.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_OVERLAP_PREVENTION = BUILDER
            .comment("Enable structure overlap prevention")
            .define("enableOverlapPrevention", true);

    private static final ForgeConfigSpec.BooleanValue LOG_BLOCKED_STRUCTURES = BUILDER
            .comment("Log when structures are blocked due to overlap")
            .define("logBlockedStructures", true);


    private static final ForgeConfigSpec.BooleanValue USE_3D_OVERLAP_DETECTION = BUILDER
            .comment("Use 3D overlap detection instead of 2D (X-Z only). 3D prevents underground structures from blocking surface structures and vice versa.")
            .define("use3DOverlapDetection", true);

    private static final ForgeConfigSpec.IntValue MIN_OVERLAP_DISTANCE = BUILDER
            .comment("Minimum distance between structure centers to prevent overlap (in blocks)")
            .defineInRange("minOverlapDistance", 16, 1, 1000);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_WHITELIST = BUILDER
            .comment("List of structure IDs that should have overlap prevention enabled. If empty, all structures are included.",
                    "Note: Use the actual Minecraft registry names (e.g., minecraft:village_plains, minecraft:stronghold)")
            .define("structureWhitelist", Arrays.asList(), Config::isValidStructureIdList);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLACKLIST = BUILDER
            .comment("List of structure IDs that should have overlap prevention disabled",
                    "Note: Use the actual Minecraft registry names, not display names.",
                    "Common structure names: minecraft:jigsawstructure, minecraft:netherfossilstructure,",
                    "minecraft:fossilstructure, minecraft:jungletemplestructure, minecraft:swamphutstructure,",
                    "minecraft:igloostructure, minecraft:alternatejigsawstructure, etc.")
            .define("structureBlacklist", Arrays.asList(
                "minecraft:jigsawstructure",
                "minecraft:netherfossilstructure",
                "minecraft:fossilstructure",
                "minecraft:jungletemplestructure",
                "minecraft:swamphutstructure",
                "minecraft:igloostructure",
                "minecraft:alternatejigsawstructure"
            ), Config::isValidStructureIdList);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_SPECIFIC_DISTANCES_RAW = BUILDER
            .comment("Per-structure minimum overlap distances. Format: [\"structure_id=distance\", \"another_id=32\"]",
                    "Note: Use actual Minecraft registry names (e.g., \"minecraft:village_plains=64\", \"minecraft:stronghold=32\")")
            .define("structureSpecificDistances", Arrays.asList(), Config::isValidStructureDistanceList);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_SPECIFIC_ENABLED_RAW = BUILDER
            .comment("Per-structure enable/disable settings. Format: [\"structure_id=true\", \"another_id=false\"]",
                    "Note: Use actual Minecraft registry names (e.g., \"minecraft:village_plains=true\", \"minecraft:stronghold=false\")")
            .define("structureSpecificEnabled", Arrays.asList(), Config::isValidStructureBooleanList);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableOverlapPrevention;
    public static boolean logBlockedStructures;
    public static boolean use3DOverlapDetection;
    public static int minOverlapDistance;
    public static Set<String> structureWhitelist;
    public static Set<String> structureBlacklist;
    public static Map<String, Integer> structureSpecificDistances;
    public static Map<String, Boolean> structureSpecificEnabled;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableOverlapPrevention = ENABLE_OVERLAP_PREVENTION.get();
        logBlockedStructures = LOG_BLOCKED_STRUCTURES.get();
        use3DOverlapDetection = USE_3D_OVERLAP_DETECTION.get();
        minOverlapDistance = MIN_OVERLAP_DISTANCE.get();
        structureWhitelist = new HashSet<>(STRUCTURE_WHITELIST.get());
        structureBlacklist = new HashSet<>(STRUCTURE_BLACKLIST.get());

        // Parse structure-specific distances from list format
        structureSpecificDistances = new LinkedHashMap<>();
        for (String entry : STRUCTURE_SPECIFIC_DISTANCES_RAW.get()) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                try {
                    structureSpecificDistances.put(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }

        // Parse structure-specific enabled settings from list format
        structureSpecificEnabled = new LinkedHashMap<>();
        for (String entry : STRUCTURE_SPECIFIC_ENABLED_RAW.get()) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                structureSpecificEnabled.put(parts[0], Boolean.parseBoolean(parts[1]));
            }
        }
    }

    // Validation methods for config values
    private static boolean isValidStructureId(Object obj) {
        if (!(obj instanceof String)) return false;
        String str = (String) obj;
        return str.contains(":") && str.length() > 3; // Basic validation for resource location format
    }

    private static boolean isValidStructureIdList(Object obj) {
        if (!(obj instanceof List)) return false;
        List<?> list = (List<?>) obj;
        for (Object item : list) {
            if (!isValidStructureId(item)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidStructureDistanceList(Object obj) {
        if (!(obj instanceof List)) return false;
        List<?> list = (List<?>) obj;
        for (Object item : list) {
            if (!(item instanceof String)) return false;
            String str = (String) item;
            if (!str.contains("=")) return false;
            String[] parts = str.split("=", 2);
            if (parts.length != 2) return false;
            if (!isValidStructureId(parts[0])) return false;
            try {
                int value = Integer.parseInt(parts[1]);
                if (value < 1 || value > 1000) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidStructureBooleanList(Object obj) {
        if (!(obj instanceof List)) return false;
        List<?> list = (List<?>) obj;
        for (Object item : list) {
            if (!(item instanceof String)) return false;
            String str = (String) item;
            if (!str.contains("=")) return false;
            String[] parts = str.split("=", 2);
            if (parts.length != 2) return false;
            if (!isValidStructureId(parts[0])) return false;
            if (!parts[1].equals("true") && !parts[1].equals("false")) return false;
        }
        return true;
    }

    // Helper methods for checking structure configuration
    public static boolean isStructureEnabled(String structureId) {
        // Check if structure is specifically disabled
        if (structureSpecificEnabled.containsKey(structureId)) {
            return structureSpecificEnabled.get(structureId);
        }

        // Check blacklist
        if (structureBlacklist.contains(structureId)) {
            return false;
        }

        // Check whitelist (if not empty, only whitelisted structures are enabled)
        if (!structureWhitelist.isEmpty()) {
            return structureWhitelist.contains(structureId);
        }

        // Default to enabled if no specific rules
        return true;
    }

    public static int getStructureDistance(String structureId) {
        return structureSpecificDistances.getOrDefault(structureId, minOverlapDistance);
    }
}
