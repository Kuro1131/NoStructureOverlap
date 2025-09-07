package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StructureOverlapManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Set<StructurePlacement>> placedStructures = new ConcurrentHashMap<>();
    
    public static class StructurePlacement {
        public final BlockPos center;
        public final int radius;
        public final ResourceLocation structureId;
        public final long timestamp;
        
        public StructurePlacement(BlockPos center, int radius, ResourceLocation structureId) {
            this.center = center;
            this.radius = radius;
            this.structureId = structureId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean overlaps(StructurePlacement other) {
            double distance = Math.sqrt(center.distSqr(other.center));
            double minDistance = radius + other.radius;
            return distance < minDistance;
        }
        
        public boolean overlapsWithDistance(StructurePlacement other, int minOverlapDistance) {
            double distance = Math.sqrt(center.distSqr(other.center));
            return distance < minOverlapDistance;
        }
        
        public boolean isSmallerThan(StructurePlacement other) {
            return radius < other.radius;
        }
    }
    
    public static boolean canPlaceStructure(BlockPos center, int radius, ResourceLocation structureId) {
        if (!Config.enableOverlapPrevention) {
            return true;
        }
        
        String structureIdString = structureId.toString();
        
        // Check if this specific structure is enabled
        if (!Config.isStructureEnabled(structureIdString)) {
            if (Config.logBlockedStructures) {
                LOGGER.debug("Structure {} is disabled by configuration", structureId);
            }
            return true; // Allow placement if structure is disabled
        }
        
        StructurePlacement newPlacement = new StructurePlacement(center, radius, structureId);
        
        // Get structure-specific distance or use default
        int minDistance = Config.getStructureDistance(structureIdString);
        
        // Check for overlaps in nearby chunks
        int searchRadius = Math.max(radius, minDistance) / 16 + 2; // Convert to chunk radius
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                String nearbyChunkKey = getChunkKey(center.offset(x * 16, 0, z * 16));
                Set<StructurePlacement> nearbyStructures = placedStructures.get(nearbyChunkKey);
                
                if (nearbyStructures != null) {
                    for (StructurePlacement existing : nearbyStructures) {
                        // Check if existing structure is also enabled
                        String existingStructureIdString = existing.structureId.toString();
                        if (!Config.isStructureEnabled(existingStructureIdString)) {
                            continue; // Skip disabled structures
                        }
                        
                        // Use the minimum distance between the two structures
                        int existingMinDistance = Config.getStructureDistance(existingStructureIdString);
                        int effectiveMinDistance = Math.min(minDistance, existingMinDistance);
                        
                        if (newPlacement.overlapsWithDistance(existing, effectiveMinDistance)) {
                            if (Config.logBlockedStructures) {
                                LOGGER.info("Blocking structure {} at {} due to overlap with {} at {} (distance: {:.1f}, min: {})", 
                                    structureId, center, existing.structureId, existing.center,
                                    Math.sqrt(center.distSqr(existing.center)), effectiveMinDistance);
                            }
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    public static void placeStructure(BlockPos center, int radius, ResourceLocation structureId) {
        if (!Config.enableOverlapPrevention) {
            return;
        }
        
        String structureIdString = structureId.toString();
        
        // Only track structures that are enabled
        if (!Config.isStructureEnabled(structureIdString)) {
            if (Config.logBlockedStructures) {
                LOGGER.debug("Not tracking structure {} as it's disabled by configuration", structureId);
            }
            return;
        }
        
        StructurePlacement placement = new StructurePlacement(center, radius, structureId);
        String chunkKey = getChunkKey(center);
        
        placedStructures.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(placement);
        
        if (Config.logBlockedStructures) {
            LOGGER.debug("Placed structure {} at {} with radius {} (min distance: {})", 
                structureId, center, radius, Config.getStructureDistance(structureIdString));
        }
    }
    
    private static String getChunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return chunkX + "," + chunkZ;
    }
    
    public static void clearStructures() {
        placedStructures.clear();
        LOGGER.info("Cleared all structure placements");
    }
    
    public static int getStructureCount() {
        return placedStructures.values().stream().mapToInt(Set::size).sum();
    }
}
