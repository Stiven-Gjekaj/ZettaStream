<div align="center">
  <a href="../README.md"><img src="../assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Architecture

ZettaStream is one Android app in one Gradle module, `app`. One APK operates
on a TV and on a phone. The code is Kotlin with Jetpack Compose, and the
player is Media3 ExoPlayer.

## The path of a stream

```
source list -> addons -> catalog -> title -> streams -> player
```

1. **The source list.** The user pastes URLs, one on each line. `SourceList`
   reads each line and finds its kind from its shape: an addon manifest, an
   M3U playlist, or an XMLTV guide. `SourceStore` keeps the list in the
   private storage of the app.
2. **The addons.** `AddonRepository` reads the manifest of each addon line.
   An addon that does not answer goes into the failures, and the Sources
   screen shows it.
3. **The catalog.** The home screen shows one row for each catalog that needs
   no argument. A metadata addon such as Cinemeta gives these rows.
4. **The title.** The title screen asks the first addon that gives `meta` for
   this kind of ID. A series gets its seasons and episodes from the answer.
5. **The streams.** The streams screen asks each addon that gives `stream`
   for the ID. Each group shows as its addon answers. A stream with no HTTP
   URL is hidden, and the screen counts it.
6. **The player.** ExoPlayer plays the URL. It sends the headers that the
   addon gave, and it loads the subtitles of the stream and of each subtitle
   addon.

Live TV takes a shorter path. `LiveRepository` reads each playlist and each
guide, and the live screen lists the channels with the programme that is on
now.

## Packages

```
app/src/main/kotlin/io/github/stivengjekaj/zettastream/
  addon/       the Stremio addon protocol: manifest, client, repository
  iptv/        M3U playlists, XMLTV guides, and the live repository
  source/      the source list and its storage
  library/     the watchlist and the watch history
  pairing/     the page that a phone opens to send the source list
  remote/      what each remote control button does
  update/      the check for a newer release on GitHub
  net/         the HTTP client
  ui/          the screen state, the theme, the components, and the screens
  MainActivity.kt   the key routing and the root of the interface
  AppContainer.kt   the objects that live as long as the app
```

## Keys

A key goes through three steps in `MainActivity.dispatchKeyEvent`:

1. The player and the remote control test take every key first.
2. On a TV, Left on an item at the left edge of the screen opens the menu.
   The focus ring tells the app where the focused item is.
3. Compose gets the key. A key that Compose does not use goes to
   `AppState.onUnhandledKey`, which runs the action from `RemoteKeys`.

A screen can give its own action to a colour button through
`AppState.screenActions`. The title screen does this for Red, Green, and
Yellow.

## Phone pairing

On a TV, the Sources screen starts `PairingServer` on port 8765 of the local
network, and shows its URL as a QR code. The URL holds a random token. The
server accepts one list with that token, then makes a new token, and the
screen shows a new QR code. The list goes from the phone to the TV directly.

## Storage

Two files in the private storage of the app hold all the state:
`sources.txt` and `library.json`. No file goes to a backup or to a new
device, because a configured addon URL can hold a key.

## Why it is built this way

[decisions.md](decisions.md) records each choice and its reason.
