# Changelog

All notable changes to the NoStructureOverlap mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.2] - 2025-09-07

### Fixed
- **Configuration serialization crash** - Fixed TOML serialization issues by changing structure-specific configuration from Map format to List format for better compatibility with NeoForge's configuration system

## [1.2.1] - 2025-09-07

### Fixed
- **Configuration serialization crash** - Fixed `Unsupported value type: class java.util.HashMap` error by using `LinkedHashMap` instead of `HashMap` for structure-specific configuration maps

## [1.2] - 2025-09-07

### Added
- **Structure-specific configuration options** - Users can now configure overlap prevention on a per-structure basis
- **Structure whitelist** - List of structure IDs that should have overlap prevention enabled (if empty, all structures are included)
- **Structure blacklist** - List of structure IDs that should have overlap prevention disabled
- **Per-structure distance settings** - Custom minimum overlap distances for specific structures
- **Per-structure enable/disable settings** - Override enable/disable settings for specific structures
- **New command `/nostructureoverlap structures`** - View all structure-specific configuration settings
- **Enhanced `/nostructureoverlap info` command** - Now shows counts of configured structures
- **Default blacklist for small structures** - Fossil structures and other small structures are ignored by default

### Changed
- **Configuration system** - Expanded from basic global settings to comprehensive structure-specific configuration
- **StructureOverlapManager logic** - Now checks structure-specific configurations before applying overlap prevention
- **Default behavior** - Small structures like fossils, ruins, and temples are now ignored by default

### Configuration
- **New config options in `nostructureoverlap-common.toml`:**
  - `structureWhitelist` - Array of structure IDs to enable overlap prevention for
  - `structureBlacklist` - Array of structure IDs to disable overlap prevention for  
  - `structureSpecificDistances` - Map of structure ID to custom distance settings
  - `structureSpecificEnabled` - Map of structure ID to enable/disable overrides

### Default Blacklisted Structures
The following structures are now ignored by default (can be overridden in config):
- `minecraft:jigsaw_structure`
- `minecraft:nether_fossil`
- `minecraft:desert_pyramid`
- `minecraft:jungle_pyramid`
- `minecraft:swamp_hut`
- `minecraft:igloo`
- `minecraft:shipwreck`
- `minecraft:shipwreck_beached`
- `minecraft:buried_treasure`
- `minecraft:ocean_ruin_cold`
- `minecraft:ocean_ruin_warm`
- `minecraft:ruined_portal` (all variants)
- `minecraft:ancient_city`

### Technical Details
- Added validation methods for configuration values
- Implemented priority system for structure configuration (per-structure settings > blacklist > whitelist > defaults)
- Enhanced logging to show structure-specific distance settings
- Improved command output formatting and information display

## [1.1] - Previous Version

### Features
- Basic structure overlap prevention system
- Mixin-based structure generation interception
- Global configuration options (enable/disable, distance, logging)
- Basic commands for status and control

### Commands
- `/nostructureoverlap status` - Check mod status and tracked structures
- `/nostructureoverlap clear` - Clear all tracked structures
- `/nostructureoverlap toggle` - Toggle overlap prevention on/off
- `/nostructureoverlap info` - Display mod information and settings
