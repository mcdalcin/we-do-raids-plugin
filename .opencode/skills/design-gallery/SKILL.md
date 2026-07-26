---
name: design-gallery
description: Render and inspect the We Do Raids side panel to verify any UI change in this repo. Use this skill whenever you touch anything under src/main/java/com/wedoraids/panel, /host, or /ui, or whenever a task involves the panel's appearance, layout, spacing, colour, contrast, button states, empty or error states, or how a state looks at different client heights. Also use it before claiming any visual change works, before answering "does this look right", when adding a new panel state to review, or when measuring contrast for accessibility. Swing renders nothing until it is laid out, so judging a change by reading Java is guessing - render it and look.
---

# Design gallery

This repo renders every side-panel state off-screen so a change can be seen instead of imagined. Use
it before you claim a UI change works. Swing computes nothing until a component is laid out, so
reasoning about appearance from source is unreliable in a specific and expensive way: preferred sizes,
HTML wrap widths and clipping only exist once something is rendered. Two real bugs in this panel were
invisible in code review and obvious in a capture — a host form that overflowed its container at
503px, and a countdown label that had never rendered at all because it was pinned to zero height.

## Render it

```bash
./gradlew gallery --args="--capture 503"     # writes every state to build/gallery
```

Then read the PNGs. They are small (242px wide) so reading several at once is cheap.

Heights matter, and 503 is the one that finds bugs: it is RuneLite's fixed-mode sidebar and the
tightest real case. `720`, `1080` and `1440` cover resizable clients. If a change affects height,
scrolling or anything that can overflow, capture 503 **and** 1080 — a layout that survives 1080 can
still break at 503.

Panel width is `PANEL_WIDTH + SCROLLBAR_WIDTH` = 242. That is not adjustable; it is what the client
gives a plugin, and it is why density decisions in this panel are so tight.

Back-to-back Gradle invocations here can fail transiently. `org.gradle.jvmargs` in
`gradle.properties` forces a single-use daemon that is stopped at the end of each build, so a second
invocation launched while the first is still exiting can lose a lock race. If a capture fails for no
visible reason, run it again before investigating.

## Look at the window yourself

Capture cannot show hover, press or focus, because those need a real pointer and focus owner. For
those, open the window:

```bash
tmux new-session -d -s gallery -c "$PWD" './gradlew gallery'
```

Use tmux or `setsid nohup ... & disown`. A plain background launch dies with exit 143 when the shell
call that started it times out, taking the window with it — the JVM is a child of that shell.

Three things that will waste your time otherwise:

- **Give it ~40s before checking.** Gradle compiles first, so the window does not exist immediately.
  Confirm with `ps -eo pid,etime,args | grep [D]esignGallery`, not by checking too early and
  concluding it failed.

- **WSLg windows cannot be screenshotted.** `java.awt.Robot` returns an all-black image, so you
  cannot verify the window programmatically and you cannot confirm it even appeared. Report the PID
  and let the human look.
- **Hover and focus are worth asking a human to check.** If a change touches those states, say so
  explicitly rather than implying you verified them.

## Verify with numbers, not impressions

Rendering tells you what it looks like; measuring tells you whether it is correct. Both matter, and
the numbers catch what the eye forgives. Read pixels straight out of the capture:

```python
from PIL import Image
im = Image.open("build/gallery/00-feed-demo-calls-503.png").convert("RGB")
print(im.getpixel((60, 186)))          # exact rendered colour, no guessing from source
```

Useful moves, all of which have found real defects here:

- **Contrast against the surface a thing actually sits on.** This is the trap. A colour can pass on
  one surface and fail on another: the panel canvas is `(40,40,40)` and cards are `(30,30,30)`, and a
  fill measured only against the card once passed at 3.04:1 while failing at 2.69:1 on the canvas,
  where the largest instance of it lived. Check every surface a role renders on, and remember
  `setOpaque(false)` means the component draws on whatever is behind it.
- **Scan a column to find bands and gaps.** Walking `getpixel((x, y))` down a fixed x and noting
  where the colour changes gives you exact element heights and gap sizes. That is how you check a
  spacing claim instead of asserting one.
- **Diff two captures.** `ImageChops.difference(a, b).getbbox()` localises a change to a pixel box,
  which turns "did that break anything?" into a yes or no. Note that two host states are *not*
  byte-stable between runs: the party hub passphrase is randomly generated, so expect a small diff on
  the `ph:` row and confirm the bbox is confined to it.
- **Simulate colour vision deficiency** before claiming two colours collide or don't. A suspected
  red/green collision in this panel turned out to be a non-issue once simulated, and the fix would
  have been wasted work.

## Add a new state

States live in `src/test/java/com/wedoraids/DesignGallery.java`. Feed, empty, auth and blocked states
need nothing special because `WeDoRaidsPanel` already exposes `setEntries`, `setVerified`, `setBanned`,
`setLoggedIn` and `setBridgeStatus` publicly:

```java
states.add(new State("Feed · my case", panel ->
{
    live(panel);
    panel.setEntries(myEntries(), 0);
}));
```

Host-form states go through the preview drivers:

```java
PanelPreview.expandHostForm(panel, 0);          // open the form on a raid tab
PanelPreview.livePost(panel, "+2", "mdps", false);
PanelPreview.promptInactivity(panel);
```

**Do not add reflection.** The harness used to drive the panel through private fields, and it cost
more than it saved: reflection survives renames silently, so it compiles and then fails at runtime, or
worse, renders a state the real panel cannot produce. Both happened. If you need to reach something
new, add a package-private seam to the production class and a matching method to the driver beside it:

- `src/test/java/com/wedoraids/panel/PanelPreview.java` — sits beside `WeDoRaidsPanel`, gets the host
  form from it
- `src/test/java/com/wedoraids/host/HostPreview.java` — sits beside the host classes, calls their
  package-private methods

Two classes rather than one because Java's package-private access is per-package, so a single driver
cannot reach both. Prefer a one-line delegate on the owning class over an accessor that hands out a
child: the panel should be asked to do something, not opened up.

**States must be reachable.** A state the real panel cannot produce is worse than no state, because it
looks authoritative and quietly invalidates whatever you concluded from it. This bit us: a live post
was rendered above a *collapsed* form toggle, because the harness flipped visibility directly instead
of expanding the form the way a submit does. Reviews of those captures were reviewing a fiction. When
you add a state, walk the user's actual path to it.

## Shipping

This is test-source only, so it never reaches the plugin jar. There is no branch to juggle and nothing
to keep out of a commit — that was only ever needed to hide the reflection, and the reflection is gone.
The harness is normal test tooling now; treat it as part of the repo.
