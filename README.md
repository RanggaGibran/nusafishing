# NusaFishing

An advanced, feature-rich Minecraft fishing system plugin designed for modern **Spigot** and **Paper** servers (1.21.x through 26.2+). NusaFishing brings RPG-style depth to fishing with custom rods, specialty baits, tiered catches, dynamic supply-and-demand markets, tournaments, a mystic fish collector, and seamless integration with custom item texture systems (**Nexo**, **Oraxen**, and **ItemsAdder**).

---

## Table of Contents

- [Features](#features)
- [Server & Java Compatibility](#server--java-compatibility)
  - [Version Support](#version-support)
  - [Java Multi-Release Architecture](#java-multi-release-architecture)
- [Custom Item / Texture Integration](#custom-item--texture-integration)
  - [Supported Providers](#supported-providers)
  - [Configuration Fields](#configuration-fields)
  - [Examples](#examples)
  - [Defensive Fallback Mechanism](#defensive-fallback-mechanism)
- [Gameplay Mechanics](#gameplay-mechanics)
  - [Fish Tiers & Weight Scaling](#fish-tiers--weight-scaling)
  - [Custom Rods & Baits](#custom-rods--baits)
  - [Dynamic Economy](#dynamic-economy)
  - [Tournaments & Events](#tournaments--events)
  - [Mystic Collector](#mystic-collector)
- [Commands & Permissions](#commands--permissions)
- [Configuration Files](#configuration-files)
- [Developer & Build Guide](#developer--build-guide)

---

## Features

- **Multi-Engine Custom Textures**: Native integration with **Nexo**, **Oraxen**, and **ItemsAdder**, with seamless fallback to vanilla materials and custom model data.
- **Modern Item Component Support**: Automatically adapts to Paper 1.21.4+ `CustomModelDataComponent` while maintaining legacy integer custom model data on earlier 1.21 releases.
- **Multi-Release JAR**: Single binary artifact supporting both Java 21 (1.21.x baseline) and Java 25 (26.2+ baseline) without user intervention.
- **Dynamic Fish Generation**: Caught fish feature procedural weights (from grams to tons), quality ratings (1–100), and dynamic tier elevations.
- **Stamina & Mini-Game System**: Engaging mini-game mechanics during fish bites to test player reflexes.
- **Market Saturation & Economy**: Built-in dynamic pricing system preventing farm abuse through supply-and-demand price decay and periodic market recovery (Vault economy support).
- **Tournaments & Community Events**: Competitive leaderboards, timed events, and customizable rewards.
- **Placeholders**: Integrated PlaceholderAPI support for player stats, stamina, and tournament rankings.

---

## Server & Java Compatibility

### Version Support

| Minecraft Version | Server Fork | Minimum Java Version | Bytecode Target |
|---|---|---|---|
| **1.21.0 – 1.21.3** | Spigot / Paper / Purpur | Java 21 | Class version 65.0 |
| **1.21.4 – 1.21.x** | Spigot / Paper / Purpur | Java 21 | Class version 65.0 |
| **26.2+** | Paper / Purpur | Java 25 | Class version 69.0 |

### Java Multi-Release Architecture

NusaFishing uses Java's standard **Multi-Release JAR (MR-JAR)** capability (JEP 238):

- **Single JAR Distribution**: Server administrators download and deploy a single artifact (`nusafishing-<version>.jar`).
- **Base Classes (`release 21`)**: Compiled with `--release 21`. Servers running on Java 21 (1.21.x) load base classes without encountering `UnsupportedClassVersionError`.
- **Java 25 Classes (`release 25`)**: Stored in `META-INF/versions/25/` with manifest flag `Multi-Release: true`. Servers running on Java 25 (e.g., Paper 26.2+) automatically take advantage of version 25 optimizations and APIs.

---

## Custom Item / Texture Integration

NusaFishing features a pluggable, defensive texture provider abstraction (`ItemTextureProvider`).

### Supported Providers

| Provider Key | Target Plugin | Integration API | Readiness Event |
|---|---|---|---|
| `vanilla` | Built-in / Resource Pack | Bukkit Material + `CustomModelData` | Always ready |
| `nexo` | [Nexo](https://nexomc.com) | `NexoItems.itemFromId(id).build()` | `NexoItemsLoadedEvent` |
| `oraxen` | [Oraxen](https://oraxen.com) | `OraxenItems.getItemById(id).build()` | `OraxenItemsLoadedEvent` |
| `itemsadder` | [ItemsAdder](https://itemsadder.dev) | `CustomStack.getInstance(id).getItemStack()` | `ItemsAdderLoadDataEvent` |

### Configuration Fields

Each fish in `fishing.yml` (or event fish in `config.yml`) can specify:

- `texture_provider`: The texture system to resolve (`vanilla`, `nexo`, `oraxen`, or `itemsadder`). Defaults to `vanilla`.
- `texture_id`: The identifier used by the custom texture plugin (e.g. `nexo_id`, `oraxen_id`, or `namespace:item_id` for ItemsAdder).
- `material`: Bukkit material used if `vanilla` is chosen or as a fallback if the custom plugin is absent or fails to load. Defaults to `COD`.
- `custom_model_data`: Integer custom model data value for vanilla resource pack models. Automatically mapped to `CustomModelDataComponent` on 1.21.4+ servers.

### Examples

#### Custom Fishing Rod Configuration (`fishing.yml`)

```yaml
fishing_rods:
  # Nexo Custom Rod
  nexo_carbon_rod:
    name: "Pancingan Carbon Nexo"
    description: "Pancingan serat karbon dari Nexo"
    texture_provider: nexo
    texture_id: "carbon_rod"
    material: FISHING_ROD       # Fallback jika Nexo tidak aktif
    custom_model_data: 1010     # Fallback CMD
    durability: 400
    stamina_modifier: 0.45
    tier: ADVANCED
    special_ability: true
    ability_description: "15% kesempatan mendapat ikan tier lebih tinggi"
    price: 35000
    lure_level: 3
    max_weight_capacity: 750.0

  # Oraxen Custom Rod
  oraxen_mythic_rod:
    name: "Pancingan Mistik Oraxen"
    description: "Pancingan bertuah dari Oraxen"
    texture_provider: oraxen
    texture_id: "mythic_rod"
    material: FISHING_ROD       # Fallback jika Oraxen tidak aktif
    custom_model_data: 1020     # Fallback CMD
    durability: 600
    stamina_modifier: 0.35
    tier: EXPERT
    special_ability: true
    ability_description: "25% kesempatan double catch"
    price: 90000
    lure_level: 4
    max_weight_capacity: 3500.0

  # ItemsAdder Custom Rod
  ia_poseidon_rod:
    name: "Pancingan Poseidon ItemsAdder"
    description: "Pancingan legendaris dari ItemsAdder"
    texture_provider: itemsadder
    texture_id: "nusafishing:poseidon_rod"   # format namespace:id
    material: FISHING_ROD                    # Fallback jika ItemsAdder tidak aktif
    custom_model_data: 1030                  # Fallback CMD
    durability: 1200
    stamina_modifier: 0.25
    tier: MASTER
    special_ability: true
    ability_description: "10% kesempatan mendapat ikan legendaris"
    price: 300000
    lure_level: 5
    max_weight_capacity: 15000.0
```

#### Custom Fish Configuration (`fishing.yml`)

```yaml
fishes:
  # 1. Nexo Custom Item
  nexo_koi:
    name: "Koi Emas Nexo"
    description: "Ikan koi dengan tekstur custom dari Nexo"
    texture_provider: nexo
    texture_id: "koi_fish"
    material: COD               # Fallback jika Nexo tidak aktif
    custom_model_data: 3010     # Fallback CMD
    min_weight: 1.0
    max_weight: 15.0
    min_quality: 30
    max_quality: 95
    base_price: 35.0
    tier: UNCOMMON

  # 2. Oraxen Custom Item
  oraxen_bass:
    name: "Bass Mistis Oraxen"
    description: "Ikan bass langka dengan tekstur dari Oraxen"
    texture_provider: oraxen
    texture_id: "mystic_bass"
    material: SALMON            # Fallback jika Oraxen tidak aktif
    custom_model_data: 3020     # Fallback CMD
    min_weight: 5.0
    max_weight: 40.0
    min_quality: 40
    max_quality: 100
    base_price: 75.0
    tier: RARE

  # 3. ItemsAdder Custom Item
  ia_abyssal_trout:
    name: "Trout Palung ItemsAdder"
    description: "Ikan bercahaya dari ItemsAdder"
    texture_provider: itemsadder
    texture_id: "nusafishing:abyssal_trout"   # format namespace:id
    material: PUFFERFISH                      # Fallback jika ItemsAdder tidak aktif
    custom_model_data: 3030                   # Fallback CMD
    min_weight: 15.0
    max_weight: 120.0
    min_quality: 60
    max_quality: 100
    base_price: 150.0
    tier: EPIC

  # 4. Standard Vanilla Custom Model Data
  vanilla_salmon:
    name: "Salmon Emas"
    description: "Ikan salmon dengan Custom Model Data vanilla"
    texture_provider: vanilla
    material: SALMON
    custom_model_data: 3001
    min_weight: 2.0
    max_weight: 10.0
    min_quality: 20
    max_quality: 90
    base_price: 25.0
    tier: COMMON
```

### Defensive Fallback Mechanism

- **Optional Soft-Dependencies**: If Nexo, Oraxen, or ItemsAdder are not present on the server, NusaFishing skips their hook registration cleanly without `NoClassDefFoundError` or console spam.
- **Asynchronous Data Handling**: When custom item plugins reload (e.g., `/oraxen reload`, `/nexo reload`, `/iareload`), NusaFishing listens to their respective reload events and automatically updates availability.
- **Graceful Fallback**: If a plugin is uninstalled, disabled, or an item ID is mistyped, NusaFishing automatically falls back to the configured `material` and `custom_model_data`.
- **Metadata Preservation**: When an item is constructed from Nexo, Oraxen, or ItemsAdder, all underlying NBT and PDC tags created by the provider are preserved while NusaFishing adds its tracking keys (`fish_id`, `fish_weight`, `fish_quality`, `fish_tier`).

---

## Gameplay Mechanics

### Fish Tiers & Weight Scaling

Catches are categorized into five tiers:
- **COMMON** (Gray `§7`, 1.0x price multiplier)
- **UNCOMMON** (Green `§a`, 1.5x price multiplier)
- **RARE** (Blue `§9`, 2.0x price multiplier)
- **EPIC** (Purple `§5`, 3.0x price multiplier)
- **LEGENDARY** (Gold `§6`, 5.0x price multiplier)

Fish weight uses dynamic diminishing-return formulas:
- Linear scaling up to 10 kg.
- Logarithmic scaling from 10 kg to 100 kg.
- Square-root scaling beyond 100 kg up to several tons.

### Custom Rods & Baits

- **Rods**: Configurable tiers (`BASIC`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`, `MASTER`), each featuring durability, stamina consumption modifiers, lure levels, maximum weight capacities, and special passive abilities (e.g., higher tier bonus, double catch chance).
- **Baits**: Custom lures specifying attraction multipliers, tier boosts, uses per item, and targeted fish affinities.

### Dynamic Economy

- **Market Saturation**: Repeatedly selling the same fish species temporarily lowers its market purchase price (down to a configurable threshold).
- **Price Recovery**: An automated background task gradually restores market prices over time.
- **Durability Repair**: Players can repair custom rods through `/nusafishing repair` based on tier multipliers and lost durability.

---

## Commands & Permissions

Aliases: `/nusafishing`, `/nf`, `/fishing`

| Command | Description | Permission | Default |
|---|---|---|---|
| `/nf help` | Displays available commands | None | Everyone |
| `/nf shop` | Opens the graphical rod, bait, and fish market | `nusafishing.shop` | `true` |
| `/nf sell` | Sells the fish currently held in main hand | `nusafishing.sell` | `true` |
| `/nf sell all` | Sells all fish in player's inventory | `nusafishing.sell` | `true` |
| `/nf repair` | Repairs the held custom fishing rod | `nusafishing.repair` | `true` |
| `/nf stats [player]` | Views personal or another player's fishing statistics | None | Everyone |
| `/nf collector` | Opens the Mystic Fish Collector GUI | `nusafishing.collector` | `true` |
| `/nf rod <id> [amount]` | Gives a custom fishing rod | `nusafishing.rod` | `op` |
| `/nf bait <id> [amount]` | Gives a custom fishing bait | `nusafishing.bait` | `op` |
| `/nf reload` | Reloads all configuration files | `nusafishing.admin.reload` | `op` |
| `/nf location <list\|tp\|create\|delete>` | Manages special fishing zones | `nusafishing.admin` | `op` |
| `/nf event <start\|stop\|info>` | Manages server-wide fishing events | `nusafishing.admin` | `op` |
| `/nf tournament <start\|stop\|info>` | Manages fishing tournaments | `nusafishing.admin.tournament` | `op` |

---

## Configuration Files

- **`config.yml`**: Main plugin settings including economy repair costs, dynamic price decay rates, stamina regeneration, and event fish pools.
- **`fishing.yml`**: Configuration for fishing rods, baits, standard fish species, custom texture hooks, and special fishing locations.
- **`rewards.yml`**: Tournament and event reward definitions.

---

## Developer & Build Guide

### Prerequisites

- **Java JDK 25** (or JDK 21+ with Java 25 installed for MR-JAR compilation).
- Gradle wrapper is included (`./gradlew`).

### Building

To build the plugin JAR:

```bash
./gradlew build
```

The compiled multi-release JAR will be located at:
```
build/libs/nusafishing-1.0-SNAPSHOT.jar
```

### Running Tests

To run the unit test suite:

```bash
./gradlew test
```

### Inspecting JAR Multi-Release Bytecode

You can inspect the generated JAR structure and verify multi-release classes:

```bash
# View JAR manifest
unzip -p build/libs/nusafishing-1.0-SNAPSHOT.jar META-INF/MANIFEST.MF

# Check base bytecode class version (major 65 = Java 21)
javap -verbose -cp build/libs/nusafishing-1.0-SNAPSHOT.jar id.nusafishing.NusaFishing | grep "major version"

# Check version 25 bytecode class version (major 69 = Java 25)
unzip -p build/libs/nusafishing-1.0-SNAPSHOT.jar META-INF/versions/25/id/nusafishing/compat/PlatformHelper.class > /tmp/PlatformHelper25.class
javap -verbose /tmp/PlatformHelper25.class | grep "major version"
```
