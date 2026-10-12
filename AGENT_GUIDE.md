# FlipFrame Studio project format

A `.flipframe` project is a ZIP archive containing `project.json`, `animation.flip`, and `AGENT_GUIDE.md`.

Supported starter commands:
```text
canvas 640 360
fps 8
background #ffffff
frame 1
rect 40 40 200 100 #ff5500
circle 320 180 45 #1769ff
line 10 10 630 350 #222222
text 30 300 28 Hello_world #111111
```

Colors are `#RRGGBB`. Rect: x y width height color. Circle: center-x center-y radius color. Line: x1 y1 x2 y2 color. Text: x y font-size words color; underscores represent spaces.

For a coding agent: read `project.json` and `animation.flip`, modify the script with supported commands, preserve valid syntax, and return the whole `.flipframe` ZIP. This early prototype does not yet support object transforms, tweening, physics, imported 3D models, audio, or MP4 export. Hand-drawn strokes are not currently serialized into script commands.
