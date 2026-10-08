"""Taps the first view on the emulator whose text is the argument.

Run: python3 tools/dev/tap.py "Play"
Add "above" to tap 150 px above the text, for example on a poster.
A text that starts with "~" matches a part of the text of the view.
"""
import re
import subprocess
import sys


def main():
    text = sys.argv[1]
    partial = text.startswith("~")
    if partial:
        text = text[1:]
    above = len(sys.argv) > 2 and sys.argv[2] == "above"
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
    xml = subprocess.run(["adb", "shell", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout
    for node in re.findall(r"<node [^>]*>", xml):
        t = re.search(r'text="([^"]*)"', node)
        if t and (text in t.group(1) if partial else t.group(1) == text):
            x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node).groups())
            y = y1 - 150 if above else (y1 + y2) // 2
            subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str(y)])
            print("tapped", text)
            return
    print("not found", text)
    sys.exit(1)


if __name__ == "__main__":
    main()
