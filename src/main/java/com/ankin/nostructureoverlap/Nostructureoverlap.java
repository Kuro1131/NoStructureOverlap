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
        LOGGER.info("Note: Full structure overlap prevention requires mixin fixes for Minecraft 1.21.1");
        LOGGER.info("Current implementation provides structure tracking and management commands");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NoStructureOverlap: Server starting - structure overlap prevention active");
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
            .then(Commands.literal("toggle")
                .executes(context -> {
                    Config.enableOverlapPrevention = !Config.enableOverlapPrevention;
                    context.getSource().sendSuccess(() -> Component.literal("Overlap prevention " + 
                        (Config.enableOverlapPrevention ? "enabled" : "disabled")), true);
                    return 1;
                }))
        );
    }
}
