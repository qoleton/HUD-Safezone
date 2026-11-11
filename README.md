# HUD Safezone

A console-style **HUD safe zone** for Minecraft. Pick a frame, and the HUD and menus stay inside it.

It's made for **ultrawide monitors** and for **recording or streaming**. If you play on a 32:9 or 21:9 screen but record a 21:9 or 16:9 crop, your hotbar, chat, minimap and item lists stay inside the part that ends up in the video.

**Minecraft 1.21.1 · NeoForge · client-side only**

## Features

- **Safe zone editor** with console-style corner brackets and a preview of where the HUD will land
  - Presets: **Full screen**, **21:9 (3440×1440)**, **16:9 (2560×1440)**
  - Width and height sliders, or drag the edges of the frame
  - Shows the exact frame size in pixels so you can match it to an OBS crop
- **HUD:** hotbar, health, chat, effects, bossbars, scoreboard and most mod overlays move inside the frame
  - The crosshair stays in the centre of the screen
  - Full-screen effects (vignette, pumpkin overlay, portal) still cover the whole screen
- **Menus:** inventories, EMI/JEI, settings and other screens are laid out inside the frame, and mouse input is shifted to match
- **Menu background:** dark background on the whole screen, inside the frame only, or off
- **Blur slider** for the background behind menus
- Hover any button in the editor for a short explanation

## How to use

1. Open **Options → HUD Safezone...** (top-left corner). You can also go through **Mods → HUD Safezone → Config**.
2. Pick a preset, or drag the frame edges until the corner brackets sit where you want the HUD to stop.
3. Turn **HUD** and **Menus** on or off as you like, then click **Done**.

Settings are saved in `config/hudsafezone.json`.

### Tips for recording

- Play in **fullscreen (F11)** so the frame lines up exactly with your recording crop.
- **OBS, 21:9 on a 32:9 monitor:** use the **21:9 (3440)** preset and crop the game capture to the middle 3440×1440 (840 px off each side).
- **16:9:** use the **16:9 (2560)** preset and crop to the middle 2560×1440.

## Compatibility

Tested with: **EMI**, **JEI**, **Jade**, **Xaero's Minimap**, **Gnetum**, **AntOS**, **Sodium/Iris**, **ImmediatelyFast**.

- **Gnetum:** works, but Gnetum's HUD caching has little effect while a frame is active.
- **Other mods:** a mod that draws its HUD in an unusual way may ignore the frame. Please open an issue and name the mod.

## Building

You need Java 21.

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

[MIT](LICENSE)
