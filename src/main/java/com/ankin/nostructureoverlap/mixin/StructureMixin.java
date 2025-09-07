package com.ankin.nostructureoverlap.mixin;

import com.ankin.nostructureoverlap.Config;
import com.ankin.nostructureoverlap.StructureOverlapManager;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Structure.class)
public class StructureMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void onStructureGenerate(net.minecraft.core.RegistryAccess registryAccess, 
                                   net.minecraft.world.level.chunk.ChunkGenerator chunkGenerator,
                                   net.minecraft.world.level.biome.BiomeSource biomeSource,
                                   net.minecraft.world.level.levelgen.RandomState randomState,
                                   net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager structureTemplateManager,
                                   long seed,
                                   net.minecraft.world.level.ChunkPos chunkPos,
                                   int chunkY,
                                   net.minecraft.world.level.LevelHeightAccessor heightAccessor,
                                   java.util.function.Predicate<net.minecraft.world.level.levelgen.structure.Structure.GenerationStub> predicate,
                                   CallbackInfoReturnable<StructureStart> cir) {
        if (!Config.enableOverlapPrevention) {
            return;
        }
        
        // Get the structure from the context
        Structure structure = (Structure) (Object) this;
        // Use the structure's class name as identifier
        String structureName = structure.getClass().getSimpleName().toLowerCase();
        ResourceLocation structureId = ResourceLocation.fromNamespaceAndPath("minecraft", structureName);
        
        BlockPos center = chunkPos.getMiddleBlockPosition(0);
        int radius = getEstimatedRadius(structureId);
        
        // Check if this structure can be placed without overlapping
        if (!StructureOverlapManager.canPlaceStructure(center, radius, structureId)) {
            if (Config.logBlockedStructures) {
                LOGGER.info("Blocked structure {} at {} due to overlap", structureId, center);
            }
            cir.setReturnValue(StructureStart.INVALID_START);
            return;
        }
        
        // If placement is allowed, register it
        StructureOverlapManager.placeStructure(center, radius, structureId);
    }
    
    private int getEstimatedRadius(ResourceLocation structureId) {
        String path = structureId.getPath();
        
        // Estimate radius based on structure type
        if (path.contains("village")) {
            return 64; // Villages are large
        } else if (path.contains("mansion")) {
            return 80; // Woodland mansions are very large
        } else if (path.contains("monument")) {
            return 60; // Ocean monuments are large
        } else if (path.contains("fortress")) {
            return 50; // Nether fortresses are medium-large
        } else if (path.contains("bastion")) {
            return 70; // Bastion remnants are large
        } else if (path.contains("outpost")) {
            return 30; // Pillager outposts are medium
        } else if (path.contains("ruined_portal")) {
            return 20; // Ruined portals are small
        } else if (path.contains("desert_pyramid")) {
            return 25; // Desert pyramids are medium
        } else if (path.contains("jungle_pyramid")) {
            return 25; // Jungle temples are medium
        } else if (path.contains("witch_hut")) {
            return 15; // Witch huts are small
        } else if (path.contains("igloo")) {
            return 10; // Igloos are small
        } else if (path.contains("shipwreck")) {
            return 15; // Shipwrecks are small
        } else if (path.contains("buried_treasure")) {
            return 5; // Buried treasures are very small
        } else if (path.contains("mineshaft")) {
            return 40; // Mineshafts are medium-large
        } else if (path.contains("stronghold")) {
            return 30; // Strongholds are medium
        } else if (path.contains("end_city")) {
            return 50; // End cities are large
        } else if (path.contains("end_gateway")) {
            return 10; // End gateways are small
        } else if (path.contains("ancient_city")) {
            return 100; // Ancient cities are very large
        } else if (path.contains("trail_ruins")) {
            return 20; // Trail ruins are small-medium
        } else if (path.contains("trial_chambers")) {
            return 30; // Trial chambers are medium
        } else {
            // Default radius for unknown structures
            return Config.minOverlapDistance;
        }
    }
}
