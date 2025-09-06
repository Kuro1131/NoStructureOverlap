# No Structure Overlap

A Minecraft mod for NeoForge that prevents structure bounding boxes from overlapping during world generation.

## Features

- **Overlap Prevention**: Automatically prevents structures from generating in overlapping areas
- **Size-Based Priority**: When two structures would overlap, the smaller one is blocked from generating
- **Configurable**: Toggle overlap prevention on/off and adjust minimum overlap distance
- **Logging**: Optional logging when structures are blocked due to overlap
- **Universal**: Works with all structures, including those from other mods

## How It Works

The mod uses mixins to intercept structure generation and checks for overlaps before allowing structures to generate. It maintains a registry of placed structures and their bounding boxes, preventing new structures from generating if they would overlap with existing ones.

## Configuration

The mod includes several configuration options:

- `enableOverlapPrevention`: Enable/disable the overlap prevention system (default: true)
- `logBlockedStructures`: Log when structures are blocked due to overlap (default: true)
- `minOverlapDistance`: Minimum distance between structure centers to prevent overlap in blocks (default: 16)

## Installation

1. Install NeoForge for Minecraft 1.21.1
2. Download the mod JAR file
3. Place it in your mods folder
4. Start the game

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.207+
- Compatible with other mods that add structures

## Technical Details

The mod uses a chunk-based system to efficiently track placed structures and check for overlaps. It estimates structure radii based on structure type and uses distance calculations to determine if structures would overlap.

## License

This mod is provided as-is for educational and personal use.
