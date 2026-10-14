<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Roadmap

Each task here is a choice of the owner. A task leaves this list when its
release is out. The reason for each choice is in
[docs/decisions.md](docs/decisions.md).

## Checks on the TV after 1.0.0 (the owner)

1.0.0 is out. These tests on the Hitachi TV and on a phone are still open.
A fault that one of them finds goes into a 1.0.x release.

- [ ] The TV switches to 24 Hz for a 24 fps film.
- [ ] The app installs its own update, from 0.11.1 to 1.0.0.
- [ ] Voice search fills the search with the spoken title.
- [ ] Torrents start in a short time and do not stop.
- [ ] The error near the end of an episode does not come back. If it does,
      send the diagnostics page.
- [ ] A season pack plays the correct episode.
- [ ] The automatic intro skip operates on an anime episode with AniSkip
      times.
- [ ] The app operates on a real phone. It operates on a phone emulator.
- [ ] Measure the memory on the TV. On the emulator, 1.0.0 uses about 58 MB
      on Home and 146 MB while it plays 720p (PSS).
