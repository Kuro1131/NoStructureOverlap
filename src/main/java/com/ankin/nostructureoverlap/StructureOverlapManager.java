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
            double distance = center.distSqr(other.center);
            double minDistance = (radius + other.radius) * (radius + other.radius);
            return distance < minDistance;
        }
        
        public boolean isSmallerThan(StructurePlacement other) {
            return radius < other.radius;
        }
    }
    
    public static boolean canPlaceStructure(BlockPos center, int radius, ResourceLocation structureId) {
        if (!Config.enableOverlapPrevention) {
            return true;
        }
        
        StructurePlacement newPlacement = new StructurePlacement(center, radius, structureId);
        
        // Check for overlaps in nearby chunks
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                String nearbyChunkKey = getChunkKey(center.offset(x * 16, 0, z * 16));
                Set<StructurePlacement> nearbyStructures = placedStructures.get(nearbyChunkKey);
                
                if (nearbyStructures != null) {
                    for (StructurePlacement existing : nearbyStructures) {
                        if (newPlacement.overlaps(existing)) {
                            if (Config.logBlockedStructures) {
                                LOGGER.info("Blocking structure {} at {} due to overlap with {} at {}", 
                                    structureId, center, existing.structureId, existing.center);
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
        
        StructurePlacement placement = new StructurePlacement(center, radius, structureId);
        String chunkKey = getChunkKey(center);
        
        placedStructures.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(placement);
        
        if (Config.logBlockedStructures) {
            LOGGER.debug("Placed structure {} at {} with radius {}", structureId, center, radius);
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
