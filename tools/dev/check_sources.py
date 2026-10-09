"""Checks each source in a ZettaStream source list.

For an addon, it reads the manifest, asks three catalogs, one meta, the
streams of a few known titles, and the subtitles that the addon declares, and
opens the first bytes of two stream links. For a playlist, it counts the
channels and opens a sample of them. For a guide, it counts the programmes
that have not ended.

Run: python3 -I tools/dev/check_sources.py private/sources-ready.txt
"""
import concurrent.futures as cf
import gzip
import json
import random
import re
import ssl
import sys
import time
import urllib.parse
import urllib.request

UA = "ZettaStream/0.5.0"
CTX = ssl.create_default_context()
SAMPLES = {
    "movie": ["tt0111161", "tt1375666"],
    "series": ["tt0903747:1:1", "tt0944947:1:1"],
    "anime": ["kitsu:46043:1", "kitsu:7442:1"],
}


def fetch(url, timeout=25, headers=None, limit=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA, **(headers or {})})
    t = time.time()
    with urllib.request.urlopen(req, timeout=timeout, context=CTX) as r:
        body = r.read(limit) if limit else r.read()
        return r.status, body, time.time() - t, r.headers


def text(body):
    if body[:2] == b"\x1f\x8b":
        body = gzip.decompress(body)
    return body.decode("utf-8", "replace")


def base(manifest_url):
    return manifest_url.rsplit("/manifest.json", 1)[0]


def res_url(b, kind, typ, id_, extra=""):
    return f"{b}/{kind}/{urllib.parse.quote(typ, safe='')}/{urllib.parse.quote(id_, safe=':')}{extra}.json"


def probe_media(url, headers):
    """Gets the first bytes of a stream URL. A video or an HLS playlist is a pass."""
    try:
        # The player encodes a space in a link, so do the same here.
        url = url.replace(" ", "%20")
        st, body, dt, h = fetch(url, timeout=20, headers={**headers, "Range": "bytes=0-2047"}, limit=2048)
        ct = (h.get("Content-Type") or "").lower()
        head = body[:64]
        if b"#EXTM3U" in body[:512]:
            return "ok hls"
        if b"<html" in body[:512].lower() or "text/html" in ct:
            return "html page"
        if head[:4] == b"\x1aE\xdf\xa3" or b"ftyp" in head[:12] or head[:1] == b"G" or "video" in ct or "audio" in ct or "octet" in ct:
            return "ok video"
        return f"unknown {ct or head[:12]!r}"
    except Exception as e:
        return f"fail {type(e).__name__} {str(e)[:50]}"


def resources(m):
    out = set()
    for r in m.get("resources", []):
        out.add(r if isinstance(r, str) else r.get("name"))
    return out


def supports(m, kind, typ, id_):
    for r in m.get("resources", []):
        if isinstance(r, str):
            if r != kind:
                continue
            types, prefixes = m.get("types", []), m.get("idPrefixes")
        else:
            if r.get("name") != kind:
                continue
            types, prefixes = r.get("types", m.get("types", [])), r.get("idPrefixes", m.get("idPrefixes"))
        if typ in types and (not prefixes or any(id_.startswith(p) for p in prefixes)):
            return True
    return False


def check_addon(url):
    notes, status = [], "ok"
    try:
        st, body, dt, _ = fetch(url)
        m = json.loads(text(body))
    except Exception as e:
        return "DEAD", [f"manifest: {type(e).__name__} {str(e)[:60]}"]
    name = m.get("name", "?")
    res = resources(m)
    notes.append(f"{name} v{m.get('version', '?')} [{', '.join(sorted(r for r in res if r))}] {dt:.1f}s")
    b = base(url)
    first_meta = None
    # Catalogs: the first three that need no extra.
    cats = [c for c in m.get("catalogs", []) if not any(e.get("isRequired") for e in c.get("extra", []))]
    good = bad = 0
    for c in cats[:3]:
        try:
            st, body, dt, _ = fetch(res_url(b, "catalog", c["type"], c["id"]))
            metas = json.loads(text(body)).get("metas", [])
            if metas:
                good += 1
                first_meta = first_meta or (c["type"], metas[0].get("id"))
            else:
                bad += 1
                notes.append(f"catalog {c['id']}: empty")
        except Exception as e:
            bad += 1
            notes.append(f"catalog {c['id']}: {type(e).__name__} {str(e)[:40]}")
    if cats:
        notes.append(f"catalogs {good}/{min(3, len(cats))} answer (of {len(m.get('catalogs', []))})")
        if good == 0:
            status = "BROKEN"
    # Meta for the first catalog item.
    if "meta" in res and first_meta and first_meta[1]:
        try:
            st, body, dt, _ = fetch(res_url(b, "meta", *first_meta))
            meta = json.loads(text(body)).get("meta") or {}
            notes.append(f"meta ok: {str(meta.get('name'))[:30]} ({len(meta.get('videos') or [])} videos)")
        except Exception as e:
            notes.append(f"meta: {type(e).__name__}")
            status = "WARN" if status == "ok" else status
    # Streams.
    if "stream" in res:
        tried, total, http, tor, probes = [], 0, 0, 0, []
        ids = [(t, i) for t, l in SAMPLES.items() for i in l if supports(m, "stream", t, i)]
        if not ids and first_meta:
            typ, mid = first_meta
            # A live channel or a single video uses its own ID.
            try:
                st, body, dt, _ = fetch(res_url(b, "meta", typ, mid))
                meta = json.loads(text(body)).get("meta") or {}
                vids = meta.get("videos") or []
                ids = [(typ, vids[0]["id"] if vids else mid)]
            except Exception:
                ids = [(typ, mid)]
        for typ, sid in ids[:3]:
            try:
                st, body, dt, _ = fetch(res_url(b, "stream", typ, sid), timeout=40)
                streams = json.loads(text(body)).get("streams", [])
                total += len(streams)
                tried.append(f"{sid}:{len(streams)} ({dt:.0f}s)")
                for s in streams:
                    u = s.get("url") or ""
                    if u.startswith("http"):
                        http += 1
                        if len(probes) < 2:
                            hdr = ((s.get("behaviorHints") or {}).get("proxyHeaders") or {}).get("request") or {}
                            probes.append(probe_media(u, hdr))
                    elif s.get("infoHash"):
                        tor += 1
            except Exception as e:
                tried.append(f"{sid}: {type(e).__name__} {str(e)[:30]}")
        notes.append("streams " + ", ".join(tried) + f" -> {http} direct, {tor} torrent")
        if probes:
            notes.append("links: " + "; ".join(probes))
        if total == 0:
            status = "BROKEN" if ids else "WARN"
        elif probes and all(not p.startswith("ok") for p in probes):
            status = "WARN" if status == "ok" else status
    sub_ids = [(t, i) for t, l in SAMPLES.items() for i in l if supports(m, "subtitles", t, i)]
    if "subtitles" in res and sub_ids:
        try:
            st, body, dt, _ = fetch(res_url(b, "subtitles", *sub_ids[0]))
            subs = json.loads(text(body)).get("subtitles", [])
            notes.append(f"subtitles {len(subs)}")
            if not subs:
                status = "WARN"
        except Exception as e:
            notes.append(f"subtitles: {type(e).__name__}")
            status = "BROKEN"
    return status, notes


def check_playlist(url):
    try:
        st, body, dt, _ = fetch(url, timeout=40)
    except Exception as e:
        return "DEAD", [f"{type(e).__name__} {str(e)[:60]}"]
    t = text(body)
    if "#EXTM3U" not in t[:200]:
        return "BROKEN", ["not an M3U file"]
    urls = [l.strip() for l in t.splitlines() if l.strip() and not l.startswith("#")]
    sample = random.Random(1).sample(urls, min(8, len(urls)))
    with cf.ThreadPoolExecutor(8) as ex:
        res = list(ex.map(lambda u: probe_media(u.split("|")[0], {}), sample))
    ok = sum(r.startswith("ok") for r in res)
    status = "ok" if ok >= len(sample) / 2 else ("WARN" if ok else "BROKEN")
    return status, [f"{len(urls)} channels, {len(body) // 1024} KB, {dt:.1f}s", f"sample of {len(sample)}: {ok} answer"]


def check_guide(url):
    try:
        st, body, dt, _ = fetch(url, timeout=90)
    except Exception as e:
        return "DEAD", [f"{type(e).__name__} {str(e)[:60]}"]
    t = text(body)
    chans = len(re.findall(r"<channel ", t))
    stops = re.findall(r'<programme[^>]*stop="(\d{12})', t)
    now = time.strftime("%Y%m%d%H%M", time.gmtime())
    future = sum(1 for s in stops if s > now)
    status = "ok" if chans and future else ("WARN" if chans else "BROKEN")
    return status, [f"{chans} channels, {len(stops)} programmes, {future} not ended, {len(body) // 1024} KB packed, {dt:.1f}s"]


def check(line):
    url = line.strip()
    if url.endswith("manifest.json"):
        kind, fn = "addon", check_addon
    elif re.search(r"\.m3u8?$", url):
        kind, fn = "playlist", check_playlist
    else:
        kind, fn = "guide", check_guide
    status, notes = fn(url)
    return url, kind, status, notes


def main():
    lines = [l.strip() for l in open(sys.argv[1]) if l.strip() and not l.startswith("#")]
    with cf.ThreadPoolExecutor(12) as ex:
        results = list(ex.map(check, lines))
    for url, kind, status, notes in results:
        host = urllib.parse.urlparse(url).netloc
        print(f"{status:6} {kind:8} {host}")
        for n in notes:
            print(f"         {n}")


if __name__ == "__main__":
    main()
