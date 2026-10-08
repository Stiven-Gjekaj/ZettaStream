<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Security Policy

## Versions that get a fix

ZettaStream is early. A security fix goes to the newest release. No older
version is maintained.

## How to report a problem

Report a security problem in private. Do not open a public issue.

- Best: open a private advisory with the "Report a vulnerability" button on the
  Security tab of the repository.
- Or write to the maintainer at stivenagostingjekaj@gmail.com.

Say how to make the problem happen again, which version you used, on which
device, and what you believe the effect is. You can expect a first answer
within a few days. Your report is named in the fix unless you ask to stay
anonymous.

## What is in scope

ZettaStream reads data that other people write: addon manifests and answers in
JSON, M3U playlists, and XMLTV guides. These are in scope:

- A fault that lets one of these files run code, read a file of the app, or
  stop the app every time that it starts.
- A fault in the pairing page that the TV serves on the local network. The
  page must accept a list only with the token of the current QR code, and only
  one time.
- A fault that shows or sends the source list, which can hold keys, to a
  person or a server that the user did not choose.

## What is out of scope

- The content of a source. A source is a service of another party.
- A device on the same local network that can see the pairing page while the
  Sources screen is open. Without the token, it cannot send a list.
- A rooted device, or a person with physical access to the unlocked device.
