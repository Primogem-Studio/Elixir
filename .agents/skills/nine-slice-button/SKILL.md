---
name: nine-slice-button
description: Draw or restyle mod GUI buttons in this Elixir project with the UButton nine-slice textures. Use when a screen needs a button, when the button art or text colour changes, or when the button skin looks stretched, misaligned, or unreadable.
---

# Nine-Slice GUI Button

## Spec

Textures live in `assets/elixir/textures/gui/widget/`, 20x25 px each:

| File | State |
| --- | --- |
| `u_button.png` | normal |
| `u_button_press.png` | hover, selected, pressed |
| `u_button_no.png` | disabled |

Insets in source pixels (l/r/t/b) stay unscaled; the centre block stretches in
both axes and the border wraps it:

| Skin | l | r | t | b |
| --- | --- | --- | --- | --- |
| `u_button` | 2 | 2 | 2 | 6 |
| `u_button_press` | 2 | 2 | 2 | 4 |
| `u_button_no` | 2 | 2 | 2 | 6 |

- Label colour is `0xFFDDF9C2`.
- The label centre is the centre of the stretched block, dropped 1 px lower for
  looks, and sunk 1 px further while the button is held.
- Minimum button height is `top + bottom + font.lineHeight`, i.e. 17 px, because
  hover swaps in the press skin. Below that the bottom band is trimmed from the
  outer edge inwards until the label fits, keeping the inner highlight line.

## Constraints

- Never scale a whole button texture. Only its centre block stretches.
- The top and side borders are never trimmed, cropped or stretched.
- Java carries no comments except public API doc.
- Work inside `src/main`; do not scan files outside it unless the task needs them.

## API

`net.per.elixir.client.widget.NineSliceButton` is the only button renderer:

```java
NineSliceButton.draw(g, font, x, y, w, h, text, enabled, hover);
NineSliceButton.draw(g, font, x, y, w, h, text, enabled, hover, selected);
```

Existing callers: `TdpUi.button`, `TdpUi.segButton`, `FurnaceSkinScreen.drawButton`.
