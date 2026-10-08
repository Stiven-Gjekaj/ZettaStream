# ZettaStream brand assets

This folder holds the app icon, the Android TV banner and the wordmark.

## Design

The mark is a bold geometric Z. The mark carries a gradient from white to grey. The gradient shows the flow of a stream. The background is the dark app colour with a soft grey glow.

## Colours

| Name | Hex | Use |
| --- | --- | --- |
| Background | `#000000` | Icon and banner background. Same as the app background. |
| Glow | `#2a2a2a` | Centre of the radial glow behind the mark. |
| White | `#ffffff` | Start of the mark gradient. |
| Grey | `#e0e0e0` | Middle of the mark gradient. Monochrome icon colour. |
| Dark grey | `#9a9a9a` | End of the mark gradient. |

## SVG sources

| File | Use |
| --- | --- |
| `icon-foreground.svg` | Adaptive icon foreground layer. 108x108 viewBox. The mark stays inside the central 66x66 safe zone. |
| `icon-background.svg` | Adaptive icon background layer. 108x108 viewBox. |
| `icon-monochrome.svg` | Monochrome layer for Android 13 themed icons. Same geometry as the foreground. |
| `icon-full.svg` | The complete square icon with rounded corners. Use it for previews and the README. |
| `banner.svg` | Android TV launcher banner. 16:9, 320x180 viewBox. The Z is in the center, on a grey glow. |
| `wordmark.svg` | The mark and the name side by side on a transparent background. It is light, so use it on a dark background only. |

## PNG renders

| File | Size | Use |
| --- | --- | --- |
| `banner-xhdpi.png` | 320x180 | `res/drawable-xhdpi/banner.png` |
| `banner-xxhdpi.png` | 480x270 | `res/drawable-xxhdpi/banner.png` |
| `icon-512.png` | 512x512 | Previews and the release page. |
| `ic_launcher-mdpi.png` | 48x48 | `res/mipmap-mdpi/ic_launcher.png` |
| `ic_launcher-hdpi.png` | 72x72 | `res/mipmap-hdpi/ic_launcher.png` |
| `ic_launcher-xhdpi.png` | 96x96 | `res/mipmap-xhdpi/ic_launcher.png` |
| `ic_launcher-xxhdpi.png` | 144x144 | `res/mipmap-xxhdpi/ic_launcher.png` |
| `ic_launcher-xxxhdpi.png` | 192x192 | `res/mipmap-xxxhdpi/ic_launcher.png` |
| `ic_launcher_round-<density>.png` | Same sizes | `res/mipmap-<density>/ic_launcher_round.png`. The round variant has a circular clip. |

## How to render again

Use `rsvg-convert` from the SVG sources. Example:

    rsvg-convert -w 320 -h 180 banner.svg -o banner-xhdpi.png
    rsvg-convert -w 48 -h 48 icon-full.svg -o ic_launcher-mdpi.png

The round launcher icon uses `icon-full.svg` with the rounded rectangle clip replaced by a circle of radius 54.

## Font

The wordmark uses Inter Bold, converted to paths. Inter has the SIL Open Font License. No font file is needed at build time or at run time.
