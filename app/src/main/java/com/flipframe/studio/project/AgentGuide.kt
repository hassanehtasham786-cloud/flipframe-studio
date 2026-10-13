package com.flipframe.studio.project

/**
 * AI coding-agent instructions embedded into every .flipframe archive. Kept single-source
 * so app builds and documentation never drift apart.
 */
object AgentGuide {
    val MARKDOWN: String = """
# FlipFrame Studio — AGENT_GUIDE.md

You are reading the embedded agent guide of a `.flipframe` project archive.
A `.flipframe` file is a plain ZIP with these entries:

| Entry           | Purpose                                     |
|-----------------|---------------------------------------------|
| `project.json`  | Project metadata and full stroke data.      |
| `animation.flip`| Human-readable FlipScript source.           |
| `AGENT_GUIDE.md`| This file.                                  |

## FlipScript reference

One command per line. `#` starts a comment. Coordinates are document pixels.

```
project "Race Scene"
canvas 640 360
fps 8
background #FFFFFF
frame 1
rect 40 40 200 100 #FF5500
circle 320 180 45 #1769FF
line 10 10 630 350 #222222 4
text 30 300 28 Hello_world
stroke 6 false 100,100 140,120 180,110
frame 2
circle 360 180 45 #1769FF
```

- `project "Name"`       - sets the project name.
- `canvas <w> <h>`       - canvas size, 32..4096 per side.
- `fps <n>`              - playback speed, 1..60.
- `background <#RRGGBB>` - page color of every frame.
- `frame <n>`            - starts frame n (1-based). Gaps create empty frames.
- `rect <x> <y> <w> <h> <#RRGGBB>` - outline rectangle.
- `circle <cx> <cy> <r> <#RRGGBB>` - outline circle.
- `line <x1> <y1> <x2> <y2> <#RRGGBB> [<width>]` - straight line, default width 4.
- `text <x> <y> <fontSize> <words> [#RRGGBB]` - text, use `_` for spaces.
- `stroke <width> <true|false> <x,y> ...` - freehand path; eraser=true erases.

Everything after `frame` until the next `frame` belongs to that frame.

## For coding agents

1. Read `project.json` for name, canvas size, fps, and stroke data.
2. Edit or generate `animation.flip` using only the commands in this guide.
3. Validate your new script: every shape line needs a color, and shape commands
   must appear after a `frame` line.
4. Repack: write `project.json`, your new `animation.flip`, and this guide back
   into a ZIP renamed with the `.flipframe` extension.
5. Never delete or corrupt stroke data you do not understand - the app needs
   `project.json` to restore freehand strokes.
""".trim()
}
