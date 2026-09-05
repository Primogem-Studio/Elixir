# Elixir (丹) — Datapack Documentation

> A Chinese-style alchemy mod for Minecraft NeoForge 1.21.1.

**[:cn: 中文版 Chinese](./README_zh.md)**

## Overview

This is a mod themed around Chinese-style alchemy (dan refinement).

## Gameplay

* Players can gather materials to craft a Dan Furnace and a Palm-Leaf Fan. Place the required ingredients into the furnace and use Flint and Steel to begin the refining process. Control the temperature with the fan until the pill is successfully formed.
* The appearance of the pill will blend colors based on the primary ingredient used.
* If the temperature drops too low, the pill becomes a Waste Pill, which grants random effects upon use.
* Pills can be consumed or thrown, inflicting most vanilla status effects as well as custom effects on the target.

## Others

* New alchemical ingredients can be registered via data packs.
* Use Java to implement additional custom effects. Individual effects can be scaled in intensity based on values configured within the ingredient data pack.

---

# Datapack Content

Everything below is data-driven and can be overridden by a datapack.

**Datapack folder layout:**

| Path | Content |
| --- | --- |
| `data/elixir/elixir/material/` | Material registration (main / off) |
| `data/elixir/elixir/furnace_visual/` | Furnace visual (model customization) |

---

## 1. Material Registration

**Registry: `elixir:material` (a datapack registry). Materials are split into two kinds: main materials `main` and off materials `off`.**

| Directory | Role |
| --- | --- |
| `data/elixir/elixir/material/main/<id>.json` | Main materials: decide the pill's primary effect |
| `data/elixir/elixir/material/off/<id>.json` | Off materials: adjust the final pharm value via a calculator |

### 1.1 Main Material

Main materials decide which effect the pill applies.

```json
{
  "item": "minecraft:amethyst_shard",
  "effect": "elixir:true_lightning",
  "pharm": -20,
  "stability": -50,
  "colors": [4284904191],
  "description": "item.elixir.material.desc.elixir.main.amethyst_shard"
}
```

| Field | Type | Description |
| --- | --- | --- |
| `item` | item id | The vanilla/mod item this material maps to, e.g. `minecraft:amethyst_shard`. |
| `effect` | effect id | Main-only. An effect registered in the `elixir:action` registry, e.g. `elixir:true_lightning`. |
| `pharm` | int | Pharm value; sign controls effect direction, magnitude controls strength. |
| `stability` | double | Stability, affects refining success. |
| `colors` | int array | Pill colors (ARGB decimal). One value = layered single-color pill (bloom & material mixing apply). Four values = a custom four-color material: only when it is the sole color source does the pill show the four layer colors as-is, skipping bloom and mixing; when refined together with other main materials its colors take no part in the blend, and the other materials decide the appearance. |
| `description` | lang key (optional) | Material description. |

### 1.2 Off Material

Off materials do not decide the effect; they adjust the final pharm through a `calc` calculator.

```json
{
  "item": "minecraft:blaze_powder",
  "calc": "elixir:plus",
  "prefix": "item.elixir.prefix.plus",
  "pharm": 10,
  "base": 20,
  "stability": 7,
  "description": "item.elixir.material.desc.elixir.off.blaze_powder"
}
```

| Field | Type | Description |
| --- | --- | --- |
| `item` | item id | The item this material maps to. |
| `calc` | calc id | Off-only. A calculator registered in the `elixir:calculator` registry, e.g. `elixir:plus`. |
| `prefix` | lang key (optional) | Name prefix. |
| `pharm` | int | Pharm value. |
| `base` | double | Base value passed to the calculator. |
| `stability` | double | Stability. |
| `description` | lang key (optional) | Material description. |

> **Main vs off:** Main materials carry `effect` + `colors` and decide the pill's effect and color; off materials carry `calc` + `base` + `prefix` and only adjust the pharm math. The same item (e.g. dirt, blaze powder) can exist in both folders, distinguished by the directory.

### 1.3 Existing Materials

**Main (31)**

| File | Item | Effect | Pharm | Stability |
| --- | --- | --- | --- | --- |
| amethyst_shard | `minecraft:amethyst_shard` | `elixir:true_lightning` | -20 | -50 |
| anvil | `minecraft:anvil` | `elixir:gravity` | 5 | -8 |
| apple | `minecraft:apple` | `elixir:health_boost` | 3 | 6 |
| blaze_powder | `minecraft:blaze_powder` | `elixir:strength` | 7 | 7 |
| cobblestone | `minecraft:cobblestone` | `elixir:infested` | 2 | -4 |
| cobweb | `minecraft:cobweb` | `elixir:weaving` | 3 | -2 |
| cod | `minecraft:cod` | `elixir:dolphins_grace` | 3 | 4 |
| dandelion | `minecraft:dandelion` | `elixir:dig_speed_slowdown` | 0 | -10 |
| dirt | `minecraft:dirt` | `elixir:poison_wither` | 1 | 1 |
| echo_shard | `minecraft:echo_shard` | `elixir:darkness` | 5 | -7 |
| ender_eye | `minecraft:ender_eye` | `elixir:entity_interaction_range` | 6 | 1 |
| ender_pearl | `minecraft:ender_pearl` | `elixir:invisibility_glowing` | 5 | -5 |
| feather | `minecraft:feather` | `elixir:slow_falling_levitation` | 0 | -1 |
| ghast_tear | `minecraft:ghast_tear` | `elixir:regeneration_poison` | 20 | -20 |
| glistering_melon_slice | `minecraft:glistering_melon_slice` | `elixir:instant_health_damage` | 10 | -10 |
| golden_apple | `minecraft:golden_apple` | `elixir:damage_resistance` | 1 | 10 |
| golden_carrot | `minecraft:golden_carrot` | `elixir:night_vision_blindness` | 8 | 5 |
| gold_ingot | `minecraft:gold_ingot` | `elixir:absorption` | 4 | 8 |
| gunpowder | `minecraft:gunpowder` | `elixir:explode` | 1 | 5 |
| lapis_lazuli | `minecraft:lapis_lazuli` | `elixir:luck_unluck` | 5 | 3 |
| magma_cream | `minecraft:magma_cream` | `elixir:fire_resistance` | 3 | -5 |
| nautilus_shell | `minecraft:nautilus_shell` | `elixir:conduit_power` | 7 | 3 |
| pufferfish | `minecraft:pufferfish` | `elixir:water_breathing` | 4 | -3 |
| rabbit_foot | `minecraft:rabbit_foot` | `elixir:jump_boost` | 6 | -9 |
| red_mushroom | `minecraft:red_mushroom` | `elixir:lightning` | 5 | 1 |
| rotten_flesh | `minecraft:rotten_flesh` | `elixir:saturation_hunger` | 3 | -8 |
| slime_ball | `minecraft:slime_ball` | `elixir:oozing` | 4 | -1 |
| spider_eye | `minecraft:spider_eye` | `elixir:confusion` | 2 | -3 |
| sugar | `minecraft:sugar` | `elixir:speed_slowdown` | 1 | -10 |
| tnt | `minecraft:tnt` | `elixir:stereo_explode` | 1 | -30 |
| wind_charge | `minecraft:wind_charge` | `elixir:wind_charged` | 4 | -2 |

**Off (4)**

| File | Item | Calc | Pharm | Base | Stability |
| --- | --- | --- | --- | --- | --- |
| blaze_powder | `minecraft:blaze_powder` | `elixir:plus` | 10 | 20 | 7 |
| dirt | `minecraft:dirt` | `elixir:mul` | 1 | 1.2 | -20 |
| empty | `minecraft:air` | `elixir:empty` | 0 | 0 | 0 |
| redstone | `minecraft:redstone` | `elixir:plus` | 0 | 75 | 100 |

---

## 2. Effect Registration (Java)

Custom effects require Java registration in two kinds: **main effects** and **off calculators**, each with its own registry.

| Registry | Interface | Registration class | Purpose |
| --- | --- | --- | --- |
| `elixir:action` | `IElixirAction` | `ElixirActions` | Main effect: what happens to the entity when the pill is consumed/thrown |
| `elixir:calculator` | `IElixirCalc` | `ElixirCalculators` | Calculator: how to adjust the running pharm sum with `base` |

### 2.1 Main Effect — IElixirAction

Interface signature:

```java
public interface IElixirAction {
    void onAction(int pharm, int time, ItemStack stack, Level level, LivingEntity entity);
}
```

Register in `ElixirActions.java`; just return a lambda, no new class needed.

```java
ACTIONS.register("my_effect", () -> (pharm, time, stack, level, entity) -> {
    // pharm is the pharm value (may be negative), time is the duration in ticks
    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.max(time, 200), (int) (pharm / effectDilute)));
});
```

Common patterns (all inside `ElixirActions.java`):

* `effect(MobEffect)` — Apply a vanilla effect; positive pharm uses the effect, negative reverses it.
* `effect(ef, ef2)` — Positive pharm → `ef`, negative pharm → `ef2`.
* `cappedEffect(effect, overflow, cap)` — Caps the amplifier, overflow spills into another effect.
* `modifier(name, attribute)` — Applies an attribute modifier scaled by `pharm / attributeModifierDilute`.
* Free lambda — Any custom logic (explosions, lightning, entity interactions).

### 2.2 Off Calculator — IElixirCalc

Interface signature:

```java
public interface IElixirCalc {
    int calc(int sum, double base);
}
```

Register in `ElixirCalculators.java`.

```java
CALCULATORS.register("my_calc", () -> (sum, base) -> (int) (sum + base));
```

Built-in calculators: `empty` (identity), `plus` (add `base`), `mul` (multiply by `base`).

---

## 3. Large Furnace Visual Framework

The visual of the Large Furnace is fully data-driven. The renderer resolves the entry `elixir:default` from the `elixir:furnace_visual` datapack registry on every reload, so overriding that file changes the appearance of every Large Furnace.

### Registry file

Override `data/elixir/elixir/furnace_visual/default.json` in your datapack, or add other entries under the same folder.

### Fields

| Field | Type | Description |
| --- | --- | --- |
| `model` | resource location | Model used for the furnace body, e.g. `elixir:block/elixir_furnace`. Falls back to the default body model when absent. |
| `texture` | resource location | Texture forced onto the body model, e.g. `elixir:block/elixir_furnace`. Falls back to the model's own textures when absent. |
| `cover_model` | resource location | Model used for the furnace cover. Falls back to the default cover when absent. |
| `cover_texture` | resource location | Texture forced onto the cover model. Falls back to the model's own textures when absent. |
| `active_texture` | resource location | Animated texture of the dan fire (see below). Falls back to `minecraft:block/lava_flow` when absent. |
| `active_color` | color | Optional color override for the dan fire (see below). Falls back to white when absent. |
| `tiers` | map of size to variant | Per-grade overrides keyed by the furnace size (odd numbers 3, 5, 7, ...). Each value is a variant with the same fields. |
| `random` | boolean | When true, one of `options` is picked deterministically per furnace position. Defaults to false. |
| `fixed` | int | Index of the option to use when `random` is false. Defaults to 0. |
| `options` | list of variants | Candidate variants; each entry supports the same fields. |

### Dan fire 丹火

While alchemy is running, an animated **dan fire** is drawn over the furnace mouth. It is rendered by the built-in mask model `elixir:block/elixir_furnace_mask`, whose texture and color can be customized:

* `active_texture` — the animated texture of the dan fire. Falls back to `minecraft:block/lava_flow` when absent. Typical values: `minecraft:block/lava_flow` (lava), `minecraft:block/water_still` (water), or any animated texture from the block atlas.
* `active_color` — a color tint multiplied over the dan fire texture. Accepts `#RRGGBB`, `0xRRGGBB` or a decimal int. Falls back to white (no tint) when absent.

```json
{
  "active_texture": "minecraft:block/water_still",
  "active_color": "#3F76E4"
}
```

### Variant selection 变体选择

A visual entry is resolved per furnace in this order:

1. If `options` is non-empty, pick one option — the index `fixed` when `random` is false, or a deterministic pick seeded by the furnace position when `random` is true.
2. Merge the chosen option into the entry; fields present in the option override the parent, missing fields inherit.
3. If `tiers` contains an entry for the furnace size, merge it on top the same way.

### Examples

Default appearance (identity):
```json
{}
```

Tiered models, fixed options:
```json
{
  "model": "elixir:block/elixir_furnace",
  "tiers": {
    "3": { "texture": "elixir:block/elixir_furnace" },
    "5": { "texture": "elixir:block/elixir_furnace_fantasy_brick" },
    "7": { "model": "elixir:block/elixir_furnace_core" }
  }
}
```

Random skin pool per furnace:
```json
{
  "random": true,
  "options": [
    { "model": "elixir:block/elixir_furnace", "texture": "elixir:block/elixir_furnace" },
    { "model": "elixir:block/elixir_furnace_cover", "texture": "elixir:block/elixir_furnace_cover" }
  ]
}
```

Complete example: a furnace whose skin is picked randomly per position, while larger furnaces get upgraded bodies and dan fire colors:
```json
{
  "model": "elixir:block/elixir_furnace",
  "texture": "elixir:block/elixir_furnace",
  "cover_model": "elixir:block/elixir_furnace_cover",
  "cover_texture": "elixir:block/elixir_furnace_cover",
  "active_texture": "minecraft:block/lava_flow",
  "active_color": "#FF6B2A",
  "random": true,
  "options": [
    { "texture": "elixir:block/elixir_furnace" },
    { "texture": "elixir:block/elixir_furnace_fantasy_brick" }
  ],
  "tiers": {
    "5": { "active_texture": "minecraft:block/water_still", "active_color": "#3F76E4" },
    "7": {
      "model": "elixir:block/elixir_furnace_test",
      "texture": "elixir:block/elixir_furnace31",
      "active_texture": "minecraft:block/water_still",
      "active_color": "#3F76E4"
    }
  }
}
```

**Current default (`default.json`):** 7-size furnaces use the `elixir_furnace2` texture with blue `water_still` dan fire; 31-size furnaces use the `elixir_furnace_test` model with the `elixir_furnace31` texture.

Model and texture locations refer to assets, so custom models and textures must be shipped by a resource pack. Textures under `assets/elixir/textures/block/` are collected into the block atlas automatically, so a resource pack that adds a file there can be referenced by `texture` right away.
