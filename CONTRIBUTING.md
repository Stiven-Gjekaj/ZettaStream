<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Contributing to ZettaStream

Thank you for your interest. ZettaStream is an Android app for TVs and phones
that plays streams from the sources that each user adds. Reports of a fault,
corrections to the documents, and new code are all welcome.

## What this project is, and what it is not

ZettaStream is a player and nothing more. It speaks the Stremio addon protocol
and reads IPTV playlists. It contains no source, and it never will.

A pull request that adds a source, a list of sources, or a link to a stream
site is refused. So is a change that makes the app read a source list from a
server of the project. The reasons are in
[docs/decisions.md](docs/decisions.md).

## Ways to help

- Report a fault or ask for a feature. Open an issue.
- Correct the documents or the readme.
- Test the app on a TV that is not tested yet, and report each remote control
  button with the Remote control test screen.

Open an issue before you start a large piece of work, so that we agree on the
way to do it before you write it.

## Development setup

You need JDK 21 and the Android SDK. On macOS:

    brew install openjdk@21 gradle
    brew install --cask android-commandlinetools

Then:

    git clone https://github.com/Stiven-Gjekaj/ZettaStream
    cd ZettaStream
    ./gradlew testDebugUnitTest assembleDebug

Put the path of the SDK in `local.properties` as `sdk.dir`, or set
`ANDROID_HOME`.

## Run it on an emulator

An Android TV emulator has a D-pad, so you can test the focus without a TV:

    sdkmanager "system-images;android-31;android-tv;arm64-v8a" "emulator"
    avdmanager create avd -n ZettaTV -k "system-images;android-31;android-tv;arm64-v8a" -d tv_1080p
    emulator -avd ZettaTV
    ./gradlew installDebug

Send keys with `adb shell input keyevent`, for example `DPAD_LEFT`,
`PROG_RED`, or `KEYCODE_5`. Note that `input keyevent 5` sends key code 5,
which is not the digit 5.

[tools/dev](tools/dev/README.md) has a test addon with legal test media, and a
helper that taps a view by its text.

## Where a change lives

| Change | Files |
| ------ | ----- |
| The Stremio addon protocol | `addon/` |
| The torrent engine and its local server | `torrent/` |
| The viewer settings | `settings/` |
| IPTV playlists and TV guides | `iptv/` |
| The source list and its storage | `source/` |
| The pairing page for the phone | `pairing/` |
| What a remote control button does | `remote/RemoteKeys.kt`, then `docs/decisions.md` |
| A screen | `ui/screens/` |
| Navigation, the menu, and key routing | `ui/AppState.kt` and `MainActivity.kt` |

All paths are under `app/src/main/kotlin/io/github/stivengjekaj/zettastream/`.

## Rules that this project holds to

- **Code and its tests go in one commit. Documents go in their own.**
- **A commit carries no version prefix and changes no version.** The version
  in `app/build.gradle.kts` moves only when something is released.
- **Each owner choice goes into `docs/decisions.md`.** Add a new entry. Do not
  remove an old one; mark it as replaced.
- **A test builds its own state.** It does not read a file that a person edits.
- **Run it. Do not conclude that it works.** Say so plainly when a measurement
  does not support the conclusion.

## Before you open a pull request

Run these, exactly as the workflow does:

    ./gradlew testDebugUnitTest lintDebug assembleDebug

Add tests for what you change. Lint must report no error and no warning.

## Style

- Match the code around you. Small functions and clear names.
- Add a dependency only with a reason.
- Write documents and comments in ASD-STE100 Simplified Technical English:
  short sentences, the active voice, the present tense. Use no em-dash and no
  emoji in source, documents, commit messages, or examples.

## Commit messages and pull requests

- Write a present-tense subject that describes the change.
- Keep one logical change in one commit. Split a feature into steps.
- In the pull request, say what changed, why, and how you tested it, and on
  which device.

## Reporting a security problem

Do not open a public issue. See [SECURITY.md](SECURITY.md).

## Code of conduct

By taking part you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).
