package com.ankin.nostructureoverlap;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;

@EventBusSubscriber(modid = Nostructureoverlap.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_OVERLAP_PREVENTION = BUILDER
            .comment("Enable structure overlap prevention")
            .define("enableOverlapPrevention", true);

    private static final ModConfigSpec.BooleanValue LOG_BLOCKED_STRUCTURES = BUILDER
            .comment("Log when structures are blocked due to overlap")
            .define("logBlockedStructures", true);

    private static final ModConfigSpec.IntValue MIN_OVERLAP_DISTANCE = BUILDER
            .comment("Minimum distance between structure centers to prevent overlap (in blocks)")
            .defineInRange("minOverlapDistance", 16, 1, 1000);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_WHITELIST = BUILDER
            .comment("List of structure IDs that should have overlap prevention enabled. If empty, all structures are included.")
            .define("structureWhitelist", Arrays.asList(), Config::isValidStructureIdList);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLACKLIST = BUILDER
            .comment("List of structure IDs that should have overlap prevention disabled")
            .define("structureBlacklist", Arrays.asList(
                "minecraft:jigsaw_structure",
                "minecraft:nether_fossil",
                "minecraft:desert_pyramid",
                "minecraft:jungle_pyramid",
                "minecraft:swamp_hut",
                "minecraft:igloo",
                "minecraft:shipwreck",
                "minecraft:shipwreck_beached",
                "minecraft:buried_treasure",
                "minecraft:ocean_ruin_cold",
                "minecraft:ocean_ruin_warm",
                "minecraft:ruined_portal",
                "minecraft:ruined_portal_desert",
                "minecraft:ruined_portal_jungle",
                "minecraft:ruined_portal_mountain",
                "minecraft:ruined_portal_nether",
                "minecraft:ruined_portal_ocean",
                "minecraft:ruined_portal_swamp",
                "minecraft:ancient_city"
            ), Config::isValidStructureIdList);

    private static final ModConfigSpec.ConfigValue<Map<String, Integer>> STRUCTURE_SPECIFIC_DISTANCES = BUILDER
            .comment("Per-structure minimum overlap distances. Format: \"structure_id=distance\"")
            .define("structureSpecificDistances", new HashMap<>(), Config::isValidStructureDistanceMap);

    private static final ModConfigSpec.ConfigValue<Map<String, Boolean>> STRUCTURE_SPECIFIC_ENABLED = BUILDER
            .comment("Per-structure enable/disable settings. Format: \"structure_id=true/false\"")
            .define("structureSpecificEnabled", new HashMap<>(), Config::isValidStructureBooleanMap);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableOverlapPrevention;
    public static boolean logBlockedStructures;
    public static int minOverlapDistance;
    public static Set<String> structureWhitelist;
    public static Set<String> structureBlacklist;
    public static Map<String, Integer> structureSpecificDistances;
    public static Map<String, Boolean> structureSpecificEnabled;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableOverlapPrevention = ENABLE_OVERLAP_PREVENTION.get();
        logBlockedStructures = LOG_BLOCKED_STRUCTURES.get();
        minOverlapDistance = MIN_OVERLAP_DISTANCE.get();
        structureWhitelist = new HashSet<>(STRUCTURE_WHITELIST.get());
        structureBlacklist = new HashSet<>(STRUCTURE_BLACKLIST.get());
        structureSpecificDistances = new HashMap<>(STRUCTURE_SPECIFIC_DISTANCES.get());
        structureSpecificEnabled = new HashMap<>(STRUCTURE_SPECIFIC_ENABLED.get());
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

    private static boolean isValidStructureDistanceMap(Object obj) {
        if (!(obj instanceof Map)) return false;
        Map<?, ?> map = (Map<?, ?>) obj;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Integer)) {
                return false;
            }
            String key = (String) entry.getKey();
            Integer value = (Integer) entry.getValue();
            if (!isValidStructureId(key) || value < 1 || value > 1000) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidStructureBooleanMap(Object obj) {
        if (!(obj instanceof Map)) return false;
        Map<?, ?> map = (Map<?, ?>) obj;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Boolean)) {
                return false;
            }
            if (!isValidStructureId(entry.getKey())) {
                return false;
            }
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
