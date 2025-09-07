package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

@Mod(Nostructureoverlap.MODID)
public class Nostructureoverlap {
    public static final String MODID = "nostructureoverlap";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Nostructureoverlap(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NoStructureOverlap mod loaded - structure overlap prevention system ready");
        LOGGER.info("Structure overlap prevention is now active with mixin-based interception");
        LOGGER.info("Use /nostructureoverlap status to check mod status and tracked structures");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NoStructureOverlap: Server starting - structure overlap prevention active");
        if (Config.preventRepeatedAttempts) {
            LOGGER.info("NoStructureOverlap: Repeated attempt prevention enabled - blocked attempts will be tracked and cleaned up automatically");
        }
    }
    
    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            LOGGER.info("NoStructureOverlap: Client level loaded - structure overlap prevention ready");
        } else {
            LOGGER.info("NoStructureOverlap: Server level loaded - structure overlap prevention active");
        }
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nostructureoverlap")
            .then(Commands.literal("status")
                .executes(context -> {
                    int structureCount = StructureOverlapManager.getStructureCount();
                    context.getSource().sendSuccess(() -> Component.literal("NoStructureOverlap Status: " + 
                        (Config.enableOverlapPrevention ? "Enabled" : "Disabled") + 
                        " | Tracked Structures: " + structureCount), false);
                    return structureCount;
                }))
            .then(Commands.literal("clear")
                .executes(context -> {
                    StructureOverlapManager.clearStructures();
                    context.getSource().sendSuccess(() -> Component.literal("Cleared all tracked structures"), true);
                    return 1;
                }))
            .then(Commands.literal("clearBlocked")
                .executes(context -> {
                    StructureOverlapManager.clearBlockedAttempts();
                    context.getSource().sendSuccess(() -> Component.literal("Cleared all blocked attempts"), true);
                    return 1;
                }))
            .then(Commands.literal("cleanup")
                .executes(context -> {
                    StructureOverlapManager.cleanupExpiredAttempts();
                    int blockedCount = StructureOverlapManager.getBlockedAttemptsCount();
                    context.getSource().sendSuccess(() -> Component.literal("Cleaned up expired attempts. Remaining blocked attempts: " + blockedCount), true);
                    return 1;
                }))
            .then(Commands.literal("toggle")
                .executes(context -> {
                    Config.enableOverlapPrevention = !Config.enableOverlapPrevention;
                    context.getSource().sendSuccess(() -> Component.literal("Overlap prevention " + 
                        (Config.enableOverlapPrevention ? "enabled" : "disabled")), true);
                    return 1;
                }))
            .then(Commands.literal("info")
                .executes(context -> {
                    StringBuilder info = new StringBuilder();
                    info.append("NoStructureOverlap Info:\n");
                    info.append("• Overlap Prevention: ").append(Config.enableOverlapPrevention ? "Enabled" : "Disabled").append("\n");
                    info.append("• Prevent Repeated Attempts: ").append(Config.preventRepeatedAttempts ? "Enabled" : "Disabled").append("\n");
                    info.append("• Min Overlap Distance: ").append(Config.minOverlapDistance).append(" blocks\n");
                    info.append("• Log Blocked Structures: ").append(Config.logBlockedStructures ? "Enabled" : "Disabled").append("\n");
                    info.append("• Tracked Structures: ").append(StructureOverlapManager.getStructureCount()).append("\n");
                    info.append("• Blocked Attempts: ").append(StructureOverlapManager.getBlockedAttemptsCount()).append("\n");
                    
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
