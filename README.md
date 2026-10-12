# FlipFrame Studio — Android starter

FlipFrame Studio is an early Android/Kotlin flipbook drawing prototype for mobile. It includes finger drawing, frame navigation, add/clear/undo, playback, a small FlipScript editor, `.flipframe` ZIP project files, and a GitHub Actions APK build workflow.

## Build a debug APK with GitHub Actions
1. Upload the contents of this folder to the root of a GitHub repository.
2. Open the **Actions** tab and enable workflows if GitHub prompts you.
3. Push a commit to `main`/`master`, or manually run **Build FlipFrame Android APK**.
4. When the run succeeds, open its **Artifacts** section and download `FlipFrame-debug-apk`.

The workflow provisions Gradle 8.9, uses Java 17, builds `assembleDebug`, and uploads `app-debug.apk`.

## `.flipframe` projects and FlipScript
A `.flipframe` file is a ZIP archive with `project.json`, `animation.flip`, and `AGENT_GUIDE.md`. A coding agent can inspect and modify `animation.flip` using this documented command syntax:

```text
canvas 640 360
fps 8
background #ffffff
frame 1
rect 40 40 200 100 #ff5500
circle 320 180 45 #1769ff
line 10 10 630 350 #222222 4
text 30 300 28 Hello_world #111111
frame 2
circle 360 180 45 #1769ff
```

Rect is `rect x y width height color`; circle is `circle center-x center-y radius color`; line is `line x1 y1 x2 y2 color [width]`; text is `text x y size words color`. Colors use `#RRGGBB`. Use underscores for spaces in text. Frame numbers range from 1 to 300.

## Current limitations
This is an early prototype, not a full FlipaClip replacement. Shapes and hand-drawn lines are serialized in the script format, but freehand strokes are converted to line segments and may be larger script files. Brush selection, layers, onion skin, audio, physics, imported 3D assets, camera animation, and MP4 export are not implemented. The APK must still be built and tested on an Android device.
