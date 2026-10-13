# FlipFrame Studio

FlipFrame Studio is a 2D frame-by-frame animation app for Android, built with Kotlin.
Draw frames with your finger, play them back, tune the frame rate, and save real,
reopenable projects as `.flipframe` files. The UI is a dark, touch-friendly editor.

## Features

- Freehand drawing on a full-screen canvas (variable-width brush, many colors, hex picker)
- Eraser, undo/redo, clear frame
- Frame timeline: add, duplicate, delete, move frames, prev/next navigation
- Playback with configurable frame rate (1–60 fps)
- Onion-skin ghosting of the previous and next frames while drawing
- Save/open `.flipframe` projects (ZIP archive, safe round trips of every stroke)
- Export the current frame as PNG into the app's Documents folder
- FlipScript: an editable text language describing the animation, embedded in every project
- FlipScript Browser screen that shows the script of each saved project

## Build the APK yourself

### On a computer / CI (recommended)

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17, Android SDK 34 (see `local.properties` or set `ANDROID_HOME`).

### Fully automatic with GitHub Actions

1. Push this repository to GitHub.
2. The included workflow (`.github/workflows/build-apk.yml`) runs unit tests, then
   `assembleDebug` on every push to `main`/`master` (or on demand via *Run workflow*).
3. Download the `FlipFrame-debug-apk` artifact from the Actions run and install it
   on your phone (Settings → allow installing from that source).

## Installing on an Android phone

1. Copy `app-debug.apk` to the phone, open it from Files.
2. Android will ask to allow installs from that app once; accept.
3. It is a debug-signed APK, so it installs side-loaded on any device with Android 7.0+.

## `.flipframe` project format

A `.flipframe` file is a regular ZIP archive:

| Entry            | Description                                       |
|------------------|---------------------------------------------------|
| `project.json`   | Project metadata + stroke data (name, size, fps). |
| `animation.flip` | Editable FlipScript source of the animation.      |
| `AGENT_GUIDE.md` | Instructions for AI coding agents.                |

Example FlipScript:

```text
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

Commands: `project`, `canvas`, `fps`, `background`, `frame`, `rect`, `circle`,
`line`, `text`, `stroke`. Full grammar is documented in `AGENT_GUIDE.md` and inside
every saved project so any coding agent can read and edit it without special tooling.

## Current limitations

- Layers are not yet separated; a frame is a flat list of strokes.
- No audio, no MP4 export yet (PNG per-frame export only).
- No camera/keyframe tweening in the script language.
- Low-memory devices: the undo stack is capped at 100 operations.

## Project layout

```
app/src/main/java/com/flipframe/studio/
  model/       Frame, Stroke, Project, FlipDocument
  flipscript/  FlipScriptParser, FlipScriptRenderer
  project/     .flipframe ZIP serializer, embedded AGENT_GUIDE
  draw/        DrawingView, UndoStack, onion-skin renderer
  ui/          dialogs (color, brush, fps, project picker)
  MainActivity.kt / BrowseActivity.kt
app/src/test/  Unit tests: parser, renderer, serialization round trips
```
