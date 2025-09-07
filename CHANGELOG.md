# Changelog

All notable changes to the NoStructureOverlap mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.1] - 2025-01-07

### Fixed
- **Structure Name Configuration** - Fixed structure names in default blacklist to use correct Minecraft registry names
- **Configuration Comments** - Added helpful comments to all structure-related config options explaining the correct naming format

### Changed
- **Default Blacklist** - Updated to use correct structure names:
  - `minecraft:jigsawstructure` (was `minecraft:jigsaw_structure`)
  - `minecraft:netherfossilstructure` (was `minecraft:nether_fossil`)
  - `minecraft:fossilstructure` (was `minecraft:desert_pyramid`)
  - `minecraft:jungletemplestructure` (was `minecraft:jungle_pyramid`)
  - `minecraft:swamphutstructure` (was `minecraft:swamp_hut`)
  - `minecraft:igloostructure` (was `minecraft:igloo`)
  - `minecraft:alternatejigsawstructure` (new addition)

## [1.3.0] - 2025-01-07

### Added
- **Blocked Attempts Tracking** - Prevents repeated attempts to place structures in the same area after blocking
- **Smart Retry Prevention** - World generation no longer wastes time repeatedly trying to place blocked structures
- **Automatic Cleanup** - Expired blocked attempts are automatically cleaned up to prevent memory leaks
- **New Configuration Option** - `preventRepeatedAttempts` setting to control blocked attempts behavior (enabled by default)
- **New Commands**:
  - `/nostructureoverlap clearBlocked` - Clear all blocked attempts
  - `/nostructureoverlap cleanup` - Manually clean up expired blocked attempts
- **Enhanced Status Commands** - `/nostructureoverlap info` now shows blocked attempts count

### Changed
- **Performance Optimization** - Significantly reduced unnecessary overlap calculations for repeatedly blocked structures
- **Memory Management** - Blocked attempts expire after 5 minutes to prevent memory accumulation
- **World Generation Efficiency** - Game no longer repeatedly attempts to place structures in known blocked areas

### Technical Details
- Added `BlockedAttempt` class to track failed structure placement attempts
- Implemented proximity-based blocked attempt detection with configurable tolerance
- Enhanced `StructureOverlapManager` with blocked attempts tracking and cleanup
- Added automatic cleanup during blocked attempt checks to maintain performance
- Improved logging to show blocked attempt reasons and cleanup statistics

### Configuration
- **New config option in `nostructureoverlap-common.toml`:**
  - `preventRepeatedAttempts` - Enable/disable blocked attempts tracking (default: true)

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
- `minecraft:jigsawstructure`
- `minecraft:netherfossilstructure`
- `minecraft:fossilstructure`
- `minecraft:jungletemplestructure`
- `minecraft:swamphutstructure`
- `minecraft:igloostructure`
- `minecraft:alternatejigsawstructure`

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
