package com.ankin.nostructureoverlap;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

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

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableOverlapPrevention;
    public static boolean logBlockedStructures;
    public static int minOverlapDistance;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableOverlapPrevention = ENABLE_OVERLAP_PREVENTION.get();
        logBlockedStructures = LOG_BLOCKED_STRUCTURES.get();
        minOverlapDistance = MIN_OVERLAP_DISTANCE.get();
    }
}
