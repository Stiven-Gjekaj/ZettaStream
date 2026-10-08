<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Getting help

## Read first

- [README.md](README.md) says what ZettaStream is, how to install it, and how
  to add sources.
- [docs/architecture.md](docs/architecture.md) explains how the parts fit
  together.
- [docs/decisions.md](docs/decisions.md) says why the app works the way it
  does. Look here before you report that a feature is missing.

## The home screen is empty

ZettaStream comes with no sources. Open Settings, then Sources, and add your
list. The home screen shows the catalogs of your metadata addons, for example
Cinemeta for movies and series.

## An addon does not answer

The Sources screen shows the state of each line. "Does not answer" means that
the app could not read the manifest. Open the same URL in a browser. If the
browser cannot open it either, the addon is down or the URL is wrong. A line
that says "needs configuration" needs its personal URL from the configure page
of the addon.

## A title has no playable stream

ZettaStream plays HTTP streams only. A stream that is only a torrent is
hidden, and the streams screen says how many it hid. A torrent addon gives HTTP
streams when you put a debrid key in its settings. Some addons also give
streams only for some kinds of ID.

## A stream does not play

Press Yellow or Back, and choose a different stream. A stream link can stop
working at any time. The error line on the player gives the reason that the
player received.

## A live channel does not play

Many free channels are down for hours, or work only in one country. Press
Channel up or Channel down to go to the next channel.

## The phone cannot open the pairing page

- The phone and the TV must be on the same network.
- Some routers keep the devices on a guest network apart. Use the main
  network.
- The URL under the QR code is the same page. You can type it on the phone.
- Each QR code works one time. After a list arrives, the TV shows a new code.

## A remote control button does nothing

Open Settings, then Remote control test, and press the button. If the button
does not show in the list, the TV keeps it for itself, and no app can use it.
Report the name and the code of a button that shows but does the wrong thing.

## Ask a question or report a fault

- Look through the
  [issues](https://github.com/Stiven-Gjekaj/ZettaStream/issues) first.
- Open a bug report for a fault, or a feature request for something new.

Say which version you use, on which device and Android version, what you did,
and what happened. Do not paste a configured addon URL: it can hold your key.

Do not use the issue tracker for a security problem. See
[SECURITY.md](SECURITY.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).
