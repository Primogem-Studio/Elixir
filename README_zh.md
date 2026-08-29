# 丹 Elixir — 数据包文档

> 一个中国风炼丹模组（Minecraft NeoForge 1.21.1）。

**[:us: English](./README.MD)**

## 总览

这是一个以中国风炼丹为主题的模组。

## 玩法

* 玩家收集材料制作四象炉与葵扇，将药材投入炉中，用打火石点火起炉，以葵扇调控炉温直至成丹。
* 丹药的外观会根据使用的主料混合出对应颜色。
* 炉温过低会炼出报废丹，服用后获得随机效果。
* 丹药可服用也可投掷，能施加绝大多数原版状态效果以及模组自定义效果。

## 其他

* 新的丹料可通过数据包注册。
* 用 Java 实现额外自定义效果，效果强度可按丹料数据包中配置的数值缩放。

---

# 数据包内容

以下所有内容均可通过数据包自定义。

**数据包根目录结构：**

| 路径 | 内容 |
| --- | --- |
| `data/elixir/elixir/material/` | 丹料注册（主料/辅料） |
| `data/elixir/elixir/furnace_visual/` | 炉子外观（模型自定义） |

---

## 1. 丹料注册

**注册表：`elixir:material`（数据包注册表）。丹料分为两类：主料 `main` 与辅料 `off`。**

| 目录 | 作用 |
| --- | --- |
| `data/elixir/elixir/material/main/<id>.json` | 主料：决定丹药的主效果（药效/属性/自定义效果） |
| `data/elixir/elixir/material/off/<id>.json` | 辅料：用计算器调整最终药理值 |

### 1.1 主料

主料决定丹药吃什么效果。

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

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `item` | 物品注册名 | 该丹料对应的物品，如 `minecraft:amethyst_shard`。 |
| `effect` | 效果注册名 | 主料专属。注册于 `elixir:action` 注册表的效果，如 `elixir:true_lightning`。 |
| `pharm` | 整数 | 药理值，正负决定效果方向，绝对值决定强度。 |
| `stability` | 小数 | 稳定性，影响炼丹成败。 |
| `colors` | 整数数组 | 丹药混合颜色（ARGB 十进制）。 |
| `description` | 语言键（可选） | 材料描述。 |

### 1.2 辅料

辅料不决定效果，而是用 `calc` 计算器调整最终药理。

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

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `item` | 物品注册名 | 该丹料对应的物品。 |
| `calc` | 计算器注册名 | 辅料专属。注册于 `elixir:calculator` 注册表的计算器，如 `elixir:plus`。 |
| `prefix` | 语言键（可选） | 名称前缀，如「微增」。 |
| `pharm` | 整数 | 药理值。 |
| `base` | 小数 | 传给计算器的基数。 |
| `stability` | 小数 | 稳定性。 |
| `description` | 语言键（可选） | 材料描述。 |

> **主料与辅料的区别：** 主料带 `effect` + `colors`，决定丹药效果与颜色；辅料带 `calc` + `base` + `prefix`，只调整药理计算。同一物品（如泥土、烈焰粉）可以在 `main` 和 `off` 下同时存在，靠目录区分。

### 1.3 现有丹料列表

**主料（32）**

| 文件 | 物品 | 效果 | 药理 | 稳定性 |
| --- | --- | --- | --- | --- |
| amethyst_shard | `minecraft:amethyst_shard` | `elixir:true_lightning` 真雷 | -20 | -50 |
| anvil | `minecraft:anvil` | `elixir:gravity` 引 | 5 | -8 |
| apple | `minecraft:apple` | `elixir:health_boost` 命效 | 3 | 6 |
| blaze_powder | `minecraft:blaze_powder` | `elixir:strength` 攻 | 7 | 7 |
| cobblestone | `minecraft:cobblestone` | `elixir:infested` 蠹效 | 2 | -4 |
| cobweb | `minecraft:cobweb` | `elixir:weaving` 织效 | 3 | -2 |
| cod | `minecraft:cod` | `elixir:dolphins_grace` 泳效 | 3 | 4 |
| dandelion | `minecraft:dandelion` | `elixir:dig_speed_slowdown` 掘 | 0 | -10 |
| dead_bush | `minecraft:dead_bush` | `elixir:unluck` 厄效 | 2 | -6 |
| dirt | `minecraft:dirt` | `elixir:poison_wither` 害 | 1 | 1 |
| echo_shard | `minecraft:echo_shard` | `elixir:darkness` 暗效 | 5 | -7 |
| ender_eye | `minecraft:ender_eye` | `elixir:entity_interaction_range` 互距 | 6 | 1 |
| ender_pearl | `minecraft:ender_pearl` | `elixir:invisibility_glowing` 匿 | 5 | -5 |
| feather | `minecraft:feather` | `elixir:slow_falling` 缓降 | 0 | -1 |
| ghast_tear | `minecraft:ghast_tear` | `elixir:regeneration_poison` 补血 | 20 | -20 |
| glistering_melon_slice | `minecraft:glistering_melon_slice` | `elixir:instant_health_damage` 瞬效 | 10 | -10 |
| golden_apple | `minecraft:golden_apple` | `elixir:damage_resistance` 抗效 | 1 | 10 |
| golden_carrot | `minecraft:golden_carrot` | `elixir:night_vision_blindness` 目 | 8 | 5 |
| gold_ingot | `minecraft:gold_ingot` | `elixir:absorption` 吸收 | 4 | 8 |
| gunpowder | `minecraft:gunpowder` | `elixir:explode` 爆 | 1 | 5 |
| lapis_lazuli | `minecraft:lapis_lazuli` | `elixir:lucks` 运 | 5 | 3 |
| magma_cream | `minecraft:magma_cream` | `elixir:fire_resistance` 炎抗 | 3 | -5 |
| nautilus_shell | `minecraft:nautilus_shell` | `elixir:conduit_power` 涌效 | 7 | 3 |
| pufferfish | `minecraft:pufferfish` | `elixir:water_breathing` 水息效 | 4 | -3 |
| rabbit_foot | `minecraft:rabbit_foot` | `elixir:jump_boost` 跃效 | 6 | -9 |
| red_mushroom | `minecraft:red_mushroom` | `elixir:lightning` 电 | 5 | 1 |
| rotten_flesh | `minecraft:rotten_flesh` | `elixir:hunger` 饥效 | 3 | -8 |
| slime_ball | `minecraft:slime_ball` | `elixir:oozing` 浆效 | 4 | -1 |
| spider_eye | `minecraft:spider_eye` | `elixir:confusion` 惑效 | 2 | -3 |
| sugar | `minecraft:sugar` | `elixir:speed_slowdown` 速 | 1 | -10 |
| tnt | `minecraft:tnt` | `elixir:stereo_explode` 四方爆 | 1 | -30 |
| wind_charge | `minecraft:wind_charge` | `elixir:wind_charged` 风效 | 4 | -2 |

**辅料（4）**

| 文件 | 物品 | 计算器 | 药理 | 基数 | 稳定性 |
| --- | --- | --- | --- | --- | --- |
| blaze_powder | `minecraft:blaze_powder` | `elixir:plus` 微增 | 10 | 20 | 7 |
| dirt | `minecraft:dirt` | `elixir:mul` 倍浅 | 1 | 1.2 | -20 |
| empty | `minecraft:air` | `elixir:empty` | 0 | 0 | 0 |
| redstone | `minecraft:redstone` | `elixir:plus` 微增 | 0 | 75 | 100 |

---

## 2. 丹料效果注册方式

自定义效果需要用 Java 注册，分为两种：**主料效果** 与 **辅料计算器**，各自独立注册表。

| 注册表 | 接口 | 注册类 | 用途 |
| --- | --- | --- | --- |
| `elixir:action` | `IElixirAction` | `ElixirActions` | 主料效果：服用/投掷丹药时对实体施加什么 |
| `elixir:calculator` | `IElixirCalc` | `ElixirCalculators` | 辅料计算器：如何用 `base` 调整药理和 |

### 2.1 主料效果 IElixirAction

接口签名：

```java
public interface IElixirAction {
    void onAction(int pharm, int time, ItemStack stack, Level level, LivingEntity entity);
}
```

在 `ElixirActions.java` 中注册，返回一个 lambda 即可，无需新写类。

```java
ACTIONS.register("my_effect", () -> (pharm, time, stack, level, entity) -> {
    // pharm 为药理值（可为负），time 为效果时长 tick
    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.max(time, 200), (int) (pharm / effectDilute)));
});
```

常用写法（都在 `ElixirActions.java` 内）：

* `effect(MobEffect)` — 施加原版状态效果，正药理正向、负药理反向（如加速/减速、治疗/伤害）。
* `effect(ef, ef2)` — 正药理用 `ef`、负药理用 `ef2`。
* `cappedEffect(effect, overflow, cap)` — 效果等级封顶，溢出部分转为另一个效果。
* `modifier(name, attribute)` — 按 `pharm / attributeModifierDilute` 施加属性修饰符。
* 自由 lambda — 任意自定义逻辑（爆炸、闪电、实体交互等）。

### 2.2 辅料计算器 IElixirCalc

接口签名：

```java
public interface IElixirCalc {
    int calc(int sum, double base);
}
```

在 `ElixirCalculators.java` 中注册。

```java
CALCULATORS.register("my_calc", () -> (sum, base) -> (int) (sum + base));
```

内置计算器：`empty`（不变）、`plus`（加 base）、`mul`（乘 base）。

---

## 3. 模型自定义（大型丹炉外观）

炉子外观完全由数据包驱动。渲染器在每次重载时从 `elixir:furnace_visual` 数据包注册表解析 `elixir:default` 条目，覆盖该文件即可改变所有大型丹炉的外观。

### 注册文件

在你的数据包中覆盖 `data/elixir/elixir/furnace_visual/default.json`，或在同目录下添加其他条目。

### 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `model` | 资源路径 | 炉体模型，如 `elixir:block/elixir_furnace`，缺省用默认炉体模型。 |
| `texture` | 资源路径 | 强制替换炉体贴图，如 `elixir:block/elixir_furnace`，缺省用模型自带贴图。 |
| `cover_model` | 资源路径 | 上鼎模型，缺省用默认上鼎。 |
| `cover_texture` | 资源路径 | 上鼎贴图，缺省用模型自带。 |
| `active_texture` | 资源路径 | 丹火动态纹理（见下文「丹火」），缺省为 `minecraft:block/lava_flow`。 |
| `active_color` | 颜色 | 丹火染色（见下文「丹火」），缺省为白色（不染色）。 |
| `tiers` | 尺寸→变体映射 | 按丹炉尺寸（奇数 3/5/7…）分级覆盖，值为相同字段的变体。 |
| `random` | 布尔 | 为 `true` 时按炉子位置确定性随机选一个 `options`，缺省 `false`。 |
| `fixed` | 整数 | 非随机时使用的 `options` 下标，缺省 0。 |
| `options` | 变体列表 | 候选变体，每项支持与 `tiers` 相同的字段。 |

### 丹火

炼丹运行时，炉口上方会叠加一层动态的**丹火**，由内置遮罩模型 `elixir:block/elixir_furnace_mask` 渲染，其贴图与颜色均可自定义：

* `active_texture` — 丹火的动态纹理，缺省为 `minecraft:block/lava_flow`。常用取值：`minecraft:block/lava_flow`（岩浆）、`minecraft:block/water_still`（水）或方块图集中的任意动态纹理。
* `active_color` — 叠加在丹火贴图上的染色，支持 `#RRGGBB`、`0xRRGGBB` 或十进制整数，缺省为白色（不染色）。

```json
{
  "active_texture": "minecraft:block/water_still",
  "active_color": "#3F76E4"
}
```

### 变体选择

每个炉子的外观按以下顺序解析：

1. 若 `options` 非空，先选出一个变体——`random` 为 `false` 时按下标 `fixed`，为 `true` 时按炉子位置确定性随机。
2. 将选中的变体并入条目：变体中存在的字段覆盖父级，缺失字段继承父级。
3. 若 `tiers` 中存在对应炉子尺寸的条目，再按同样方式并入。

### 示例

默认外观（空对象）：
```json
{}
```

分级模型、固定选项：
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

按炉随机外观池：
```json
{
  "random": true,
  "options": [
    { "model": "elixir:block/elixir_furnace", "texture": "elixir:block/elixir_furnace" },
    { "model": "elixir:block/elixir_furnace_cover", "texture": "elixir:block/elixir_furnace_cover" }
  ]
}
```

综合示例：炉子外观按位置随机取皮，大炉随尺寸升级炉体与丹火颜色：
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

**现有默认配置（`default.json`）：** 7 格炉使用 `elixir_furnace2` 贴图与蓝色 `water_still` 丹火；31 格炉使用 `elixir_furnace_test` 模型与 `elixir_furnace31` 贴图。

模型与贴图路径指向 assets，自定义模型/贴图需由资源包提供。`assets/elixir/textures/block/` 下的贴图会自动收入方块图集，资源包在此目录添加文件即可直接被 `texture` 引用。
