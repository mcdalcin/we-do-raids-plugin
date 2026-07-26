# We Do Raids plugin

A RuneLite Plugin Hub plugin. `./gradlew build` compiles, runs checkstyle and runs the tests. All
three are expected to pass before anything is proposed as done.

## Check UI changes by rendering them

Anything under `src/main/java/com/wedoraids/panel`, `/host` or `/ui` is Swing, and Swing computes
nothing until it is laid out. Preferred sizes, HTML wrap widths and clipping only exist once
something is rendered, so judging a visual change by reading Java is guessing.

Render it and look:

```bash
./gradlew gallery --args="--capture 503"   # writes every side-panel state to build/gallery
```

Read [`src/test/java/com/wedoraids/gallery/README.md`](src/test/java/com/wedoraids/gallery/README.md)
before touching panel appearance, adding a state, or answering whether something looks right. It
covers which heights to capture, how to measure colour and spacing from the pixels, and why a state
has to be reachable the way a user reaches it.
