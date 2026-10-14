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
- [ ] The app operates on a phone. (It operates on a phone emulator in
      0.11.1. Check it on a real phone.)

### Phase 2: hardening

- [x] Review the code that changed since the last review (0.8.0 to
      0.11.0). The fixes are in 0.11.0.
- [x] Compare versions such as `1.0.0-rc1` and `1.0.0` correctly.
- [x] Measure the size of the release APK and the memory. On the emulator,
      0.11.0 is 14.1 MB. It uses 58 MB on Home and 146 MB while it plays
      720p (PSS). Measure again on the TV in phase 1.
- [x] Remove the logs that only debug builds need.

### Phase 3: interface overhaul

- [x] Follow the layout of Miruro 2.0, and keep the black and white colors.

### Phase 4: release

- [ ] Stop new features. Release the last fixes as 0.11.x. (0.11.0 is out.)
- [ ] Update the readme with screens of the TV, write the 1.0.0 changelog,
      add the version rule to the decisions, and check the security policy.
      (Done for 0.11.0: the screens, the version rule, and the policy. The
      1.0.0 changelog waits for the release.)
- [ ] Tag v1.0.0, check the APK and its signature, and update to it from
      0.10.0 inside the app.
