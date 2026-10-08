"""A small Stremio addon for tests on the emulator.

It gives three streams for each movie and series ID: a public HLS test stream
from Mux, the Big Buck Bunny torrent (Blender Foundation, CC BY 3.0), and a
link to a web page that is not a video, to test the player error.
The emulator reaches it at http://10.0.2.2:7799/manifest.json.

Run: python3 tools/dev/test_addon.py
"""
import http.server
import json

MANIFEST = {
    "id": "test.zetta", "name": "Test streams", "version": "1.0.0",
    "types": ["movie", "series"], "idPrefixes": ["tt", "kitsu"],
    "resources": ["stream"], "catalogs": [],
}
STREAMS = {"streams": [
    {"url": "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
     "name": "Test HLS\n720p", "title": "Mux public test stream"},
    {"infoHash": "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
     "name": "Big Buck Bunny\nTorrent 👤 150", "title": "Big Buck Bunny (Blender, CC BY)",
     "sources": ["tracker:udp://tracker.opentrackr.org:1337/announce"]},
    {"url": "http://10.0.2.2:7799/page.html",
     "name": "Web page\nNot a video", "title": "A link that gives HTML"},
]}


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/page.html":
            self.send_response(200)
            self.send_header("Content-Type", "text/html")
            self.end_headers()
            self.wfile.write(b"<html><body>Download</body></html>")
            return
        if self.path.endswith("/manifest.json"):
            body = MANIFEST
        elif self.path.startswith("/stream/"):
            body = STREAMS
        else:
            body = None
        self.send_response(200 if body else 404)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        if body:
            self.wfile.write(json.dumps(body).encode())

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    http.server.ThreadingHTTPServer(("0.0.0.0", 7799), Handler).serve_forever()
