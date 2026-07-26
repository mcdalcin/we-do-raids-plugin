---
name: design-gallery
description: Render and inspect the We Do Raids side panel to check a UI change. Use when touching anything under src/main/java/com/wedoraids/panel, /host or /ui, when a task involves the panel's appearance, layout, spacing, colour, contrast, button states or empty and error states, and before answering whether a visual change looks right. Swing computes nothing until it is laid out, so judging a change by reading Java is guessing.
---

# Design gallery

Every side-panel state can be rendered off-screen and read as a PNG:

```bash
./gradlew gallery --args="--capture 503"   # writes every state to build/gallery
```

Read [`src/test/java/com/wedoraids/gallery/README.md`](../../../src/test/java/com/wedoraids/gallery/README.md)
before using it. That file is the reference and this one only points at it. It covers which heights
to capture, how to measure colour and spacing from the pixels, how to add a state, and why a state
has to be reachable the way a user reaches it.
