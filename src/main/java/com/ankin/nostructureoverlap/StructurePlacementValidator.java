package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates structure placement to prevent overlaps.
 * This is the core of the new approach - we validate placement decisions
 * before any generation work is done, making it much more efficient.
 */
public class StructurePlacementValidator {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track placed structures by chunk for efficient lookup
    private static final Map<String, Set<StructurePlacement>> placedStructures = new ConcurrentHashMap<>();
    
    // Track structure sets and their exclusion zones (for future use)
    // private static final Map<ResourceLocation, StructureSetInfo> structureSetInfo = new ConcurrentHashMap<>();
    
    public static class StructurePlacement {
        public final BlockPos center;
        public final ResourceLocation structureId;
        public final StructureSet structureSet;
        public final long timestamp;
        public final int exclusionRadius;
        
        public StructurePlacement(BlockPos center, ResourceLocation structureId, StructureSet structureSet) {
            this.center = center;
            this.structureId = structureId;
            this.structureSet = structureSet;
            this.timestamp = System.currentTimeMillis();
            this.exclusionRadius = calculateExclusionRadius(structureId);
        }
        
        public boolean overlaps(StructurePlacement other) {
            if (Config.use3DOverlapDetection) {
                return overlaps3D(other);
            } else {
                return overlaps2D(other);
            }
        }
        
        public boolean overlaps3D(StructurePlacement other) {
            // Calculate 3D distance between structure centers
            double distance = Math.sqrt(center.distSqr(other.center));
            double minDistance = exclusionRadius + other.exclusionRadius;
            return distance < minDistance;
        }
        
        public boolean overlaps2D(StructurePlacement other) {
            // Calculate 2D distance (X-Z only) for horizontal overlap detection
            double dx = center.getX() - other.center.getX();
            double dz = center.getZ() - other.center.getZ();
            double distance2D = Math.sqrt(dx * dx + dz * dz);
            double minDistance2D = exclusionRadius + other.exclusionRadius;
            return distance2D < minDistance2D;
        }
        
        public boolean isInExclusionZone(BlockPos otherCenter, int otherExclusionRadius) {
            if (Config.use3DOverlapDetection) {
                return isInExclusionZone3D(otherCenter, otherExclusionRadius);
            } else {
                return isInExclusionZone2D(otherCenter, otherExclusionRadius);
            }
        }
        
        public boolean isInExclusionZone3D(BlockPos otherCenter, int otherExclusionRadius) {
            // Calculate 3D distance for exclusion zone check
            double distance = Math.sqrt(center.distSqr(otherCenter));
            return distance < Math.max(exclusionRadius, otherExclusionRadius);
        }
        
        public boolean isInExclusionZone2D(BlockPos otherCenter, int otherExclusionRadius) {
            // Calculate 2D distance (X-Z only) for horizontal exclusion zone check
            double dx = center.getX() - otherCenter.getX();
            double dz = center.getZ() - otherCenter.getZ();
            double distance2D = Math.sqrt(dx * dx + dz * dz);
            return distance2D < Math.max(exclusionRadius, otherExclusionRadius);
        }
        
        private int calculateExclusionRadius(ResourceLocation structureId) {
            String structureIdString = structureId.toString();
            
            // Get structure-specific distance or use default
            int baseDistance = Config.getStructureDistance(structureIdString);
            
            // Add some buffer for structure size
            int structureSize = getEstimatedStructureSize(structureId);
            
            return Math.max(baseDistance, structureSize);
        }
        
        private int getEstimatedStructureSize(ResourceLocation structureId) {
            String path = structureId.getPath();
            
            // Estimate structure size based on type
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
                return Config.minOverlapDistance;
            }
        }
    }
    
    public static class StructureSetInfo {
        public final StructureSet structureSet;
        public final int exclusionZoneRadius;
        public final Set<ResourceLocation> conflictingStructures;
        
        public StructureSetInfo(StructureSet structureSet, int exclusionZoneRadius) {
            this.structureSet = structureSet;
            this.exclusionZoneRadius = exclusionZoneRadius;
            this.conflictingStructures = new HashSet<>();
        }
    }
    
    /**
     * Check if a structure can be placed at the given position without overlapping.
     * This is the main validation method called before structure generation.
     */
    public static boolean canPlaceStructureAt(BlockPos center, ResourceLocation structureId, StructureSet structureSet) {
        if (!Config.enableOverlapPrevention) {
            return true;
        }
        
        String structureIdString = structureId.toString();
        
        // Check if this specific structure is enabled
        if (!Config.isStructureEnabled(structureIdString)) {
            return true; // Allow placement if structure is disabled
        }
        
        StructurePlacement newPlacement = new StructurePlacement(center, structureId, structureSet);
        
        // Check for overlaps in nearby chunks
        int searchRadius = newPlacement.exclusionRadius / 16 + 2; // Convert to chunk radius
        ChunkPos centerChunk = new ChunkPos(center);
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                ChunkPos nearbyChunk = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                String chunkKey = getChunkKey(nearbyChunk);
                Set<StructurePlacement> nearbyStructures = placedStructures.get(chunkKey);
                
                if (nearbyStructures != null) {
                    for (StructurePlacement existing : nearbyStructures) {
                        // Check if existing structure is also enabled
                        String existingStructureIdString = existing.structureId.toString();
                        if (!Config.isStructureEnabled(existingStructureIdString)) {
                            continue; // Skip disabled structures
                        }
                        
                        // Check for overlap
                        if (newPlacement.overlaps(existing)) {
                            if (Config.logBlockedStructures) {
                                LOGGER.info("Blocked structure {} at {} due to overlap with {} at {} (distance: {:.1f})", 
                                    structureId, center, existing.structureId, existing.center,
                                    Math.sqrt(center.distSqr(existing.center)));
                            }
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Register a structure placement after validation passes.
     * This is called when a structure is actually placed.
     */
    public static void registerStructurePlacement(BlockPos center, ResourceLocation structureId, StructureSet structureSet) {
        if (!Config.enableOverlapPrevention) {
            return;
        }
        
        String structureIdString = structureId.toString();
        
        // Only track structures that are enabled
        if (!Config.isStructureEnabled(structureIdString)) {
            return;
        }
        
        StructurePlacement placement = new StructurePlacement(center, structureId, structureSet);
        ChunkPos chunkPos = new ChunkPos(center);
        String chunkKey = getChunkKey(chunkPos);
        
        placedStructures.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(placement);
        
        if (Config.logBlockedStructures) {
            LOGGER.debug("Registered structure {} at {} with exclusion radius {}", 
                structureId, center, placement.exclusionRadius);
        }
    }
    
    /**
     * Get the chunk key for a ChunkPos.
     */
    private static String getChunkKey(ChunkPos chunkPos) {
        return chunkPos.x + "," + chunkPos.z;
    }
    
    /**
     * Clear all tracked structure placements.
     */
    public static void clearStructures() {
        placedStructures.clear();
        LOGGER.info("Cleared all structure placements");
    }
    
    /**
     * Get the total number of tracked structures.
     */
    public static int getStructureCount() {
        return placedStructures.values().stream().mapToInt(Set::size).sum();
    }
    
    /**
     * Get structure count for a specific structure type.
     */
    public static int getStructureCount(ResourceLocation structureId) {
        return (int) placedStructures.values().stream()
            .flatMap(Set::stream)
            .filter(placement -> placement.structureId.equals(structureId))
            .count();
    }
    
    /**
     * Clean up old structure placements to prevent memory leaks.
     * This should be called periodically.
     */
    public static void cleanupOldStructures() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 24 * 60 * 60 * 1000; // 24 hours
        
        int totalBefore = getStructureCount();
        
        placedStructures.values().forEach(placements -> 
            placements.removeIf(placement -> currentTime - placement.timestamp > maxAge));
        
        int totalAfter = getStructureCount();
        
        if (totalBefore > totalAfter) {
            LOGGER.debug("Cleaned up {} old structure placements", totalBefore - totalAfter);
        }
    }
}
