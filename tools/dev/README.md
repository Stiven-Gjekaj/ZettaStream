# Tools for development

These tools help to test the app on the Android TV emulator. They are not
part of the app.

| File | Use |
| ---- | --- |
| `test_addon.py` | A Stremio addon that gives a public HLS test stream, a legal test torrent, and a link to a web page that is not a video. The emulator reaches it at `http://10.0.2.2:7799/manifest.json`. |
| `tap.py` | Taps a view by its text, through `adb`. `~text` matches a part of the text. `above` taps 150 px above the text, for example on a poster. |

A tap puts the emulator in touch mode, and in touch mode no item has the
focus. To test the focus, use only keys, for example
`adb shell input keyevent KEYCODE_DPAD_DOWN`.

The test media is the Mux public test stream and the Big Buck Bunny torrent,
which the Blender Foundation publishes under CC BY 3.0.
