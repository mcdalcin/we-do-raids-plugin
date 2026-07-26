# Design gallery

Renders every side-panel state off-screen so a UI change can be looked at instead of imagined. Use it
before claiming a UI change works.

Swing computes nothing until a component is laid out, so reading the source tells you very little:
preferred sizes, HTML wrap widths and clipping only exist once something is rendered. Two bugs here
were invisible in code review and obvious in a capture. A host form overflowed its container at
503px, and a countdown label pinned to zero height had never rendered at all.

## Capture

```bash
./gradlew gallery --args="--capture 503"   # writes every state to build/gallery
```

The PNGs are 242px wide, so reading several at once is cheap.

Height matters, and 503 is the one that finds bugs: RuneLite's fixed-mode sidebar, the tightest real
case. 720, 1080 and 1440 cover resizable clients. If a change affects height, scrolling or anything
that can overflow, capture 503 and 1080. A layout that survives 1080 can still break at 503.

Panel width is `PANEL_WIDTH + SCROLLBAR_WIDTH` = 242 and is not adjustable. It is what the client
gives a plugin, and it is why density decisions here are tight.

Back-to-back Gradle invocations can fail transiently. `org.gradle.jvmargs` forces a single-use daemon,
so a second invocation launched while the first is exiting can lose a lock race. If a capture fails
for no visible reason, run it again before investigating.

## Open the window

Capture cannot show hover, press or focus, which need a real pointer and focus owner.

```bash
tmux new-session -d -s gallery -c "$PWD" './gradlew gallery'
```

Use tmux or `setsid nohup ... & disown`. A plain background launch dies with exit 143 when the shell
call that started it times out, taking the window with it.

- Give it about 40s. Gradle compiles first. Confirm with
  `ps -eo pid,etime,args | grep [D]esignGallery` rather than deciding too early that it failed.
- WSLg windows cannot be screenshotted. `java.awt.Robot` returns an all-black image, so an agent
  cannot check the window itself. Report the PID and let a human look.
- Hover and focus need a human. If a change touches those states, say so rather than implying they
  were checked.

## Measure the pixels

Read values out of the capture instead of judging by eye:

```python
from PIL import Image
im = Image.open("build/gallery/00-feed-demo-calls-503.png").convert("RGB")
print(im.getpixel((60, 186)))          # exact rendered colour
```

Each of these has found a real defect here:

- Check contrast against the surface a thing actually sits on. A colour can pass on one surface and
  fail on another. The canvas is `(40,40,40)` and cards are `(30,30,30)`, and a fill measured only
  against the card passed at 3.04:1 while failing at 2.69:1 on the canvas, where its largest instance
  lived. `setOpaque(false)` means the component draws on whatever is behind it.
- Scan a column for bands and gaps. Walking `getpixel((x, y))` down a fixed x and noting where the
  colour changes gives exact element heights and gaps, so a spacing claim can be checked rather than
  asserted.
- Diff two captures. `ImageChops.difference(a, b).getbbox()` localises a change to a pixel box. Four
  host states are not byte-stable between runs because the party hub passphrase is randomly
  generated, so expect a small diff confined to the `ph:` row.
- Simulate colour vision deficiency before claiming two colours collide. A suspected red/green
  collision here turned out to be a non-issue once simulated, and the fix would have been wasted work.

## Add a state

States live in `DesignGallery.java`. Feed, empty, auth and blocked states need nothing special,
because `WeDoRaidsPanel` exposes `setEntries`, `setVerified`, `setBanned`, `setLoggedIn` and
`setBridgeStatus` publicly:

```java
states.add(new State("Feed · my case", panel ->
{
    live(panel);
    panel.setEntries(myEntries(), 0);
}));
```

Host-form states go through `HostPreview`, which clicks the real controls:

```java
HostPreview.expandHostForm(panel, 0);              // open the form on a raid tab
HostPreview.livePost(panel, "+2", "mdps", false);
HostPreview.promptInactivity(panel);
```

Drive controls rather than setting state. `SwingProbe` finds a control by its label and clicks it,
and fails on a miss or an ambiguous match. Two earlier approaches cost more than they saved:
reflection survived renames silently, so it compiled and then failed at runtime; and setting state
directly once rendered a live post above a collapsed toggle, which no host can reach, so anything
concluded from those captures was worthless.

Prefer a real click. Where no control can reach a state, add a package-private seam on the production
class and a matching method on `HostPreview` beside it, and say in its javadoc why no click will do.
Four exist today: entering a live post and offering an undo both need a bridge reply the gallery
never sends, and the idle prompt waits out a seven-minute timer.

When you add a state, walk the user's actual path to it. A state the panel cannot produce is worse
than no state, because it still looks authoritative.

## Shipping

Test sources only, so none of it reaches the plugin jar. The Plugin Hub packages
`sourceSets.main.output`, and with `build=standard` it replaces this build file outright. Treat it as
normal test tooling.
