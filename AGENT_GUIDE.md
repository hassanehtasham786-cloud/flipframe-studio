This guide ships twice: inside every `.flipframe` archive (embeddable via `app/src/main/java/com/flipframe/studio/project/AgentGuide.kt`) and in the repository root.

# FlipFrame Studio — AGENT_GUIDE.md

A `.flipframe` project is a ZIP archive with three entries:

- `project.json` — project metadata and full stroke data (authoritative for drawing).
- `animation.flip` — editable FlipScript source (authoritative for editing).
- `AGENT_GUIDE.md` — this file.

## Editing workflow for AI coding agents

1. Unzip the archive. Read `project.json` for name, canvas size, fps, background.
2. Edit `animation.flip` using only the commands documented below.
3. Validate: every shape command must appear after a `frame` line; colors are `#RRGGBB`.
4. Repack all three entries into a ZIP renamed with the `.flipframe` extension.
5. Never drop stroke data you did not generate — `project.json` is the restore path for freehand strokes.

Supported starter commands:
```text
# One command per line. `#` starts a comment. Coordinates are document pixels.
project "Race Scene"
canvas 640 360
fps 12
background #87CEEB
frame 1
rect 40 260 180 80 #E53935
circle 320 180 45 #1769FF
line 0 340 640 340 #222222 4
text 30 60 32 Hello_world
stroke 6 false 100,100 140,120 180,110
frame 2
circle 360 180 45 #1769FF
```

| Command | Meaning |
|---------|---------|
| `project "Name"` | Sets project name. |
| `canvas <w> <h>` | Canvas size, 32..4096 per side. |
| `fps <n>` | Playback speed, 1..60. |
| `background <#RRGGBB>` | Page color of every frame. |
| `frame <n>` | Starts frame n (1-based). Frame-number gaps create empty frames. |
| `rect <x> <y> <w> <h> <#RRGGBB>` | Outline rectangle. |
| `circle <cx> <cy> <r> <#RRGGBB>` | Outline circle. |
| `line <x1> <y1> <x2> <y2> <#RRGGBB> [<width>]` | Straight line, default width 4. |
| `text <x> <y> <fontSize> <words> [#RRGGBB]` | Text, `_` becomes a space. |
| `stroke <width> <true\|false> <x,y> ...` | Freehand polyline; `true` = eraser. |

Errors report line numbers: unknown commands, malformed numbers, or shape commands
before any `frame` command all fail validation.

Colors are `#RRGGBB`. Rect is `x y width height color`. Circle is `center-x center-y radius color`. Line is `x1 y1 x2 y2 color [width]`. This version does not yet support object transforms, tweening, physics, imported 3D models, audio, or MP4 export.
