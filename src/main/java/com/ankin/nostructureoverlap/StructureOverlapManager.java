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
    private static final Map<String, Set<BlockedAttempt>> blockedAttempts = new ConcurrentHashMap<>();
    private static final long BLOCKED_ATTEMPT_EXPIRY_TIME = 300000; // 5 minutes in milliseconds
    
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
    
    public static class BlockedAttempt {
        public final BlockPos center;
        public final int radius;
        public final ResourceLocation structureId;
        public final long timestamp;
        public final String reason;
        
        public BlockedAttempt(BlockPos center, int radius, ResourceLocation structureId, String reason) {
            this.center = center;
            this.radius = radius;
            this.structureId = structureId;
            this.timestamp = System.currentTimeMillis();
            this.reason = reason;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > BLOCKED_ATTEMPT_EXPIRY_TIME;
        }
        
        public boolean isNearby(BlockPos otherCenter, int otherRadius) {
            double distance = Math.sqrt(center.distSqr(otherCenter));
            double minDistance = Math.max(radius, otherRadius) * 0.5; // Allow some tolerance
            return distance < minDistance;
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
        
        // Check if we've recently blocked a similar structure in this area
        if (Config.preventRepeatedAttempts) {
            if (hasRecentBlockedAttempt(center, radius, structureId)) {
                if (Config.logBlockedStructures) {
                    LOGGER.debug("Blocking structure {} at {} - recent blocked attempt in area", structureId, center);
                }
                return false;
            }
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
                            
                            // Record this blocked attempt
                            if (Config.preventRepeatedAttempts) {
                                recordBlockedAttempt(center, radius, structureId, 
                                    "Overlap with " + existing.structureId + " at distance " + 
                                    String.format("%.1f", Math.sqrt(center.distSqr(existing.center))) + 
                                    " (min: " + effectiveMinDistance + ")");
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
    
    public static int getBlockedAttemptsCount() {
        return blockedAttempts.values().stream().mapToInt(Set::size).sum();
    }
    
    private static boolean hasRecentBlockedAttempt(BlockPos center, int radius, ResourceLocation structureId) {
        String structureIdString = structureId.toString();
        Set<BlockedAttempt> attempts = blockedAttempts.get(structureIdString);
        
        if (attempts == null || attempts.isEmpty()) {
            return false;
        }
        
        // Clean up expired attempts while checking
        attempts.removeIf(BlockedAttempt::isExpired);
        
        // Check if there's a recent blocked attempt nearby
        for (BlockedAttempt attempt : attempts) {
            if (attempt.isNearby(center, radius)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void recordBlockedAttempt(BlockPos center, int radius, ResourceLocation structureId, String reason) {
        String structureIdString = structureId.toString();
        BlockedAttempt attempt = new BlockedAttempt(center, radius, structureId, reason);
        
        blockedAttempts.computeIfAbsent(structureIdString, k -> ConcurrentHashMap.newKeySet()).add(attempt);
        
        if (Config.logBlockedStructures) {
            LOGGER.debug("Recorded blocked attempt for {} at {}: {}", structureId, center, reason);
        }
    }
    
    public static void clearBlockedAttempts() {
        blockedAttempts.clear();
        LOGGER.info("Cleared all blocked attempts");
    }
    
    public static void cleanupExpiredAttempts() {
        int totalBefore = getBlockedAttemptsCount();
        blockedAttempts.values().forEach(attempts -> attempts.removeIf(BlockedAttempt::isExpired));
        int totalAfter = getBlockedAttemptsCount();
        
        if (totalBefore > totalAfter) {
            LOGGER.debug("Cleaned up {} expired blocked attempts", totalBefore - totalAfter);
        }
    }
}
