package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

@Mod(Nostructureoverlap.MODID)
public class Nostructureoverlap {
    public static final String MODID = "nostructureoverlap";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Nostructureoverlap() {
        // Register the commonSetup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NoStructureOverlap mod loaded - structure overlap prevention system ready");
        LOGGER.info("Structure overlap prevention is active with early interception at ChunkGenerator level");
        LOGGER.info("This approach prevents overlaps before any generation work is done, improving performance");
        LOGGER.info("Use /nostructureoverlap status to check mod status and tracked structures");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NoStructureOverlap: Server starting - structure overlap prevention active");
        LOGGER.info("NoStructureOverlap: 3D overlap detection is " + (Config.use3DOverlapDetection ? "enabled" : "disabled"));
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nostructureoverlap")
            .then(Commands.literal("status")
                .executes(context -> {
                    int structureCount = StructurePlacementValidator.getStructureCount();
                    context.getSource().sendSuccess(() -> Component.literal("NoStructureOverlap Status: " +
                        (Config.enableOverlapPrevention ? "Enabled" : "Disabled") +
                        " | Tracked Structures: " + structureCount), false);
                    return structureCount;
                }))
            .then(Commands.literal("clear")
                .executes(context -> {
                    StructurePlacementValidator.clearStructures();
                    context.getSource().sendSuccess(() -> Component.literal("Cleared all tracked structures"), true);
                    return 1;
                }))
            .then(Commands.literal("cleanup")
                .executes(context -> {
                    StructurePlacementValidator.cleanupOldStructures();
                    int structureCount = StructurePlacementValidator.getStructureCount();
                    context.getSource().sendSuccess(() -> Component.literal("Cleaned up old structures. Remaining: " + structureCount), true);
                    return 1;
                }))
            .then(Commands.literal("toggle")
                .executes(context -> {
                    Config.enableOverlapPrevention = !Config.enableOverlapPrevention;
                    context.getSource().sendSuccess(() -> Component.literal("Overlap prevention " +
                        (Config.enableOverlapPrevention ? "enabled" : "disabled")), true);
                    return 1;
                }))
            .then(Commands.literal("toggle3D")
                .executes(context -> {
                    Config.use3DOverlapDetection = !Config.use3DOverlapDetection;
                    context.getSource().sendSuccess(() -> Component.literal("3D overlap detection " +
                        (Config.use3DOverlapDetection ? "enabled" : "disabled (using 2D only)")), true);
                    return 1;
                }))
            .then(Commands.literal("info")
                .executes(context -> {
                    StringBuilder info = new StringBuilder();
                    info.append("NoStructureOverlap Info:\n");
                    info.append("• Overlap Prevention: ").append(Config.enableOverlapPrevention ? "Enabled" : "Disabled").append("\n");
                    info.append("• 3D Overlap Detection: ").append(Config.use3DOverlapDetection ? "Enabled" : "Disabled (2D only)").append("\n");
                    info.append("• Min Overlap Distance: ").append(Config.minOverlapDistance).append(" blocks\n");
                    info.append("• Log Blocked Structures: ").append(Config.logBlockedStructures ? "Enabled" : "Disabled").append("\n");
                    info.append("• Tracked Structures: ").append(StructurePlacementValidator.getStructureCount()).append("\n");

                    if (!Config.structureWhitelist.isEmpty()) {
                        info.append("• Whitelisted Structures: ").append(Config.structureWhitelist.size()).append("\n");
                    }
                    if (!Config.structureBlacklist.isEmpty()) {
                        info.append("• Blacklisted Structures: ").append(Config.structureBlacklist.size()).append("\n");
                    }
                    if (!Config.structureSpecificDistances.isEmpty()) {
                        info.append("• Custom Distance Structures: ").append(Config.structureSpecificDistances.size()).append("\n");
                    }
                    if (!Config.structureSpecificEnabled.isEmpty()) {
                        info.append("• Custom Enabled/Disabled Structures: ").append(Config.structureSpecificEnabled.size()).append("\n");
                    }

                    context.getSource().sendSuccess(() -> Component.literal(info.toString()), false);
                    return 1;
                }))
            .then(Commands.literal("structures")
                .executes(context -> {
                    StringBuilder structures = new StringBuilder();
                    structures.append("Structure Configuration:\n");

                    if (!Config.structureWhitelist.isEmpty()) {
                        structures.append("Whitelisted: ").append(String.join(", ", Config.structureWhitelist)).append("\n");
                    }
                    if (!Config.structureBlacklist.isEmpty()) {
                        structures.append("Blacklisted: ").append(String.join(", ", Config.structureBlacklist)).append("\n");
                    }
                    if (!Config.structureSpecificDistances.isEmpty()) {
                        structures.append("Custom Distances:\n");
                        Config.structureSpecificDistances.forEach((id, distance) ->
                            structures.append("  ").append(id).append(": ").append(distance).append(" blocks\n"));
                    }
                    if (!Config.structureSpecificEnabled.isEmpty()) {
                        structures.append("Custom Enabled/Disabled:\n");
                        Config.structureSpecificEnabled.forEach((id, enabled) ->
                            structures.append("  ").append(id).append(": ").append(enabled ? "Enabled" : "Disabled").append("\n"));
                    }

                    if (structures.toString().equals("Structure Configuration:\n")) {
                        structures.append("No structure-specific configurations set (using defaults)");
                    }

                    context.getSource().sendSuccess(() -> Component.literal(structures.toString()), false);
                    return 1;
                }))
        );
    }
}
