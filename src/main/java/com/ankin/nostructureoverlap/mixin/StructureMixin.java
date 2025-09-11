package com.ankin.nostructureoverlap.mixin;

import com.ankin.nostructureoverlap.Config;
import com.ankin.nostructureoverlap.StructurePlacementValidator;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Structure.class)
public class StructureMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Intercept structure generation at the Structure level.
     * This uses the correct method signature for NeoForge 21.1.208.
     */
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void onGenerate(net.minecraft.core.RegistryAccess registryAccess,
                          net.minecraft.world.level.chunk.ChunkGenerator chunkGenerator,
                          net.minecraft.world.level.biome.BiomeSource biomeSource,
                          net.minecraft.world.level.levelgen.RandomState randomState,
                          net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager structureTemplateManager,
                          long seed,
                          net.minecraft.world.level.ChunkPos chunkPos,
                          int references,
                          net.minecraft.world.level.LevelHeightAccessor levelHeightAccessor,
                          java.util.function.Predicate<net.minecraft.world.level.levelgen.structure.Structure> structurePredicate,
                          CallbackInfoReturnable<StructureStart> cir) {
        if (!Config.enableOverlapPrevention) {
            return;
        }
        
        // Get the structure position
        BlockPos center = chunkPos.getMiddleBlockPosition(0);
        
        // Get structure ID
        ResourceLocation structureId = getStructureId((Structure) (Object) this);
        
        // Check if this structure is enabled for overlap prevention
        if (!Config.isStructureEnabled(structureId.toString())) {
            return;
        }
        
        // Validate structure placement before any generation work
        if (!StructurePlacementValidator.canPlaceStructureAt(center, structureId, null)) {
            if (Config.logBlockedStructures) {
                LOGGER.debug("Blocked structure {} at chunk ({}, {}) due to overlap prevention", 
                    structureId, chunkPos.x, chunkPos.z);
            }
            
            // Return invalid start to prevent structure placement
            cir.setReturnValue(StructureStart.INVALID_START);
            return;
        }
        
        // If placement is allowed, register it for future overlap checks
        StructurePlacementValidator.registerStructurePlacement(center, structureId, null);
    }
    
    /**
     * Get the ResourceLocation for a structure.
     * Uses class name as fallback for API compatibility.
     */
    private ResourceLocation getStructureId(Structure structure) {
        // Try to get the registry name first using reflection
        try {
            // Use reflection to avoid API compatibility issues
            Object structureType = structure.getClass().getMethod("getType").invoke(structure);
            Object registryName = structureType.getClass().getMethod("getRegistryName").invoke(structureType);
            if (registryName != null) {
                return (ResourceLocation) registryName;
            }
        } catch (Exception e) {
            // Fallback to class name approach
        }
        
        // Fallback to class name approach
        String structureName = structure.getClass().getSimpleName().toLowerCase();
        return ResourceLocation.fromNamespaceAndPath("minecraft", structureName);
    }
}