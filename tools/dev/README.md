# Tools for development

These tools help to test the app on the Android TV emulator. They are not
part of the app.

| File | Use |
| ---- | --- |
| `test_addon.py` | A Stremio addon that gives a public HLS test stream and a legal test torrent. The emulator reaches it at `http://10.0.2.2:7799/manifest.json`. |
| `tap.py` | Taps a view by its text, through `adb`. |

The test media is the Mux public test stream and the Big Buck Bunny torrent,
which the Blender Foundation publishes under CC BY 3.0.
