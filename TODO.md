<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Roadmap

Each task here is a choice of the owner. A task leaves this list when its
release is out. The reason for each choice is in
[docs/decisions.md](docs/decisions.md).

## Night shift, 2026-10-08

### Faults

- [ ] A stream near the end of an episode loads and stops about every four
      seconds.
- [ ] A torrent downloads more than it needs before playback starts.
- [ ] Some titles do not render fully. Add a small space to them.

### Checks on the emulator

- [ ] The files of a torrent go away after playback.
- [ ] A season pack plays the correct episode.
- [ ] The intro skip operates in the app.

### The list of streams

- [ ] Read the seeder count from the format of each addon, and sort torrents
      by seeders.
- [ ] Show quality badges: resolution, HDR, dual audio.
- [ ] Add a "Reload streams" action for slow addons.
- [ ] Add a setting that puts direct links above torrent links.

### Watching

- [ ] The next episode tries to match the resolution, the codec, and the
      other data of the current source, not only its name.
- [ ] Show a "Next episode" countdown at the end of an episode.
- [ ] Add a setting that skips intros automatically.
- [ ] Skipping the ending marks the episode as watched.

### The interface

- [ ] Text that is too long scrolls, so that the viewer can read all of it.
- [ ] Refine the home rows: add rows such as "Newly released", and remove
      rows that do not help.
- [ ] Make the remote control keys universal, so that a remote with no number
      keys and no Menu key can use each function.

### Quality

- [ ] Cut network data and make the app faster.
- [ ] Review the code for faults and leftovers.
