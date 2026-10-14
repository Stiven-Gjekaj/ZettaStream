<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Roadmap

Each task here is a choice of the owner. A task leaves this list when its
release is out. The reason for each choice is in
[docs/decisions.md](docs/decisions.md).

## Release 1.0.0

1.0.0 means: the app runs each day on the Hitachi TV with no known fault,
each function operates with the remote control only, and the documents agree
with the app.

### Phase 1: tests on the TV (the owner)

- [ ] The TV switches to 24 Hz for a 24 fps film.
- [ ] The app installs its own update, from 0.10.0 to the next version.
- [ ] Voice search fills the search with the spoken title.
- [ ] Torrents start in a short time and do not stop.
- [ ] The error near the end of an episode does not come back. If it does,
      send the diagnostics page.
- [ ] A season pack plays the correct episode.
- [ ] The automatic intro skip operates on an anime episode with AniSkip
      times.
- [ ] The app operates on a phone.

### Phase 2: hardening

- [ ] Review the code that changed since the last review.
- [ ] Compare versions such as `1.0.0-rc1` and `1.0.0` correctly, or use
      0.11.x for test releases.
- [ ] Measure the size of the release APK and the memory on the TV.
- [ ] Remove the logs that only debug builds need.

### Phase 3: interface overhaul

- [ ] Follow the layout of Miruro 2.0, and keep the black and white colors.

### Phase 4: release

- [ ] Stop new features. Release the last fixes as 0.11.x.
- [ ] Update the readme with screens of the TV, write the 1.0.0 changelog,
      add the version rule to the decisions, and check the security policy.
- [ ] Tag v1.0.0, check the APK and its signature, and update to it from
      0.10.0 inside the app.
