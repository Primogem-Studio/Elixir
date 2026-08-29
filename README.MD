# Overview
This is a mod themed around Chinese-style alchemy (dan refinement).
# Gameplay
* Players can gather materials to craft a Dan Furnace and a Palm-Leaf Fan. Place the required ingredients into the furnace and use Flint and Steel to begin the refining process. Control the temperature with the fan until the pill is successfully formed.
* The appearance of the pill will blend colors based on the primary ingredient used.
* If the temperature drops too low, the pill becomes a Waste Pill, which grants random effects upon use.
* Pills can be consumed or thrown, inflicting most vanilla status effects as well as custom effects on the target.
# Others
* New alchemical ingredients can be registered via data packs.
* Use Java to implement additional custom effects. Individual effects can be scaled in intensity based on values configured within the ingredient data pack.

# Large Furnace Visual Framework
The visual of the Large Furnace (multi-block dan furnace) can be fully customized through a data pack. The renderer resolves the entry `elixir:default` from the `elixir:furnace_visual` data pack registry on every reload, so overriding that file changes the appearance of every Large Furnace.

## Registry file
Override `data/elixir/elixir/furnace_visual/default.json` in your data pack, or add other entries under the same folder.

## Fields
| Field | Type | Description |
| --- | --- | --- |
| `model` | resource location | Model used for the furnace body, e.g. `elixir:block/elixir_furnace`. Falls back to the default body model when absent. |
| `texture` | resource location | Texture forced onto the body model, e.g. `elixir:block/elixir_furnace`. Falls back to the model's own textures when absent. |
| `cover_model` | resource location | Model used for the furnace cover. Falls back to the default cover when absent. |
| `cover_texture` | resource location | Texture forced onto the cover model. Falls back to the model's own textures when absent. |
| `active_texture` | resource location | Animated texture drawn over the furnace while alchemy is running (the small furnace's lava mask). Falls back to `minecraft:block/lava_flow` when absent. |
| `active_color` | color | Optional color override applied to `active_texture`, e.g. `"#3F76E4"` for vanilla water blue. Accepts `#RRGGBB`, `0xRRGGBB` or a decimal int. Falls back to the texture's own colors when absent. |
| `tiers` | map of size to variant | Per-grade overrides keyed by the furnace size (odd numbers 3, 5, 7, ...). Each value is a variant with the same `model`/`texture`/`cover_model`/`cover_texture`/`active_texture`/`active_color` fields. |
| `random` | boolean | When true, one of `options` is picked deterministically per furnace position. Defaults to false. |
| `fixed` | int | Index of the option to use when `random` is false. Defaults to 0. |
| `options` | list of variants | Candidate variants; each entry supports the same pointing fields as `tiers`. |

## Examples
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
Model and texture locations refer to assets, so custom models and textures must be shipped by a resource pack. Textures under `assets/elixir/textures/block/` are collected into the block atlas automatically, so a resource pack that adds a file there can be referenced by `texture` right away.