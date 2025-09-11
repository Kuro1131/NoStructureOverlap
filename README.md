# No Structure Overlap v2.0.0

An advanced Minecraft mod for NeoForge that prevents structure bounding boxes from overlapping during world generation using early interception and 3D detection.

## 🚀 Key Features

- **Early Interception**: Prevents overlaps before any generation work is done, improving performance
- **3D Overlap Detection**: Properly handles structures at different Y levels (underground vs surface)
- **2D/3D Toggle**: Configurable overlap detection mode for different use cases
- **Smart Exclusion Zones**: Intelligent structure placement validation
- **Per-Structure Configuration**: Customize settings for individual structure types
- **Universal Compatibility**: Works with all structures, including those from other mods

## 🎯 How It Works

The mod uses a revolutionary early interception approach at the `ChunkGenerator.findStructurePositions()` level, preventing structure overlaps before any generation work begins. This eliminates wasted computation and provides significantly better performance compared to traditional late interception methods.

### 3D Detection
- **Surface vs Underground**: Underground structures no longer incorrectly block surface structures
- **Vertical Spacing**: Proper 3D distance calculations prevent false overlaps
- **Configurable**: Toggle between 3D and 2D detection modes as needed

## ⚙️ Configuration

### Core Settings
- `enableOverlapPrevention`: Enable/disable the overlap prevention system (default: true)
- `use3DOverlapDetection`: Enable 3D overlap detection (default: true)
- `logBlockedStructures`: Log when structures are blocked due to overlap (default: true)
- `minOverlapDistance`: Minimum distance between structure centers in blocks (default: 16)

### Advanced Settings
- `structureWhitelist`: List of structures to enable overlap prevention for
- `structureBlacklist`: List of structures to disable overlap prevention for
- `structureSpecificDistances`: Custom distances for specific structures
- `structureSpecificEnabled`: Per-structure enable/disable overrides

## 🎮 Commands

- `/nostructureoverlap status` - Check mod status and tracked structures
- `/nostructureoverlap clear` - Clear all tracked structures
- `/nostructureoverlap cleanup` - Clean up old structure placements
- `/nostructureoverlap toggle` - Toggle overlap prevention on/off
- `/nostructureoverlap toggle3D` - Toggle between 3D and 2D detection
- `/nostructureoverlap info` - Display detailed mod information
- `/nostructureoverlap structures` - Show structure configuration

## 📦 Installation

1. Install NeoForge for Minecraft 1.21.1
2. Download the mod JAR file
3. Place it in your mods folder
4. Start the game

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.207+
- Compatible with other mods that add structures

## 🔧 Technical Details

### Architecture
- **Early Interception**: Uses `ChunkGenerator.findStructurePositions()` mixin for maximum efficiency
- **3D Distance Calculation**: Full 3D coordinate system for accurate overlap detection
- **Chunk-Based Tracking**: Efficient structure placement tracking by chunk
- **Automatic Cleanup**: Built-in memory management with configurable cleanup intervals

### Performance
- **Zero Wasted Computation**: Structures are blocked before any generation work begins
- **Memory Efficient**: Optimized data structures with automatic cleanup
- **Scalable**: Handles large numbers of structures without performance degradation

### Compatibility
- **Universal**: Works with all structure types and mods
- **Configurable**: Per-structure settings for maximum flexibility
- **Backward Compatible**: Existing configurations work with new defaults

## License

This mod is provided as-is for educational and personal use.
