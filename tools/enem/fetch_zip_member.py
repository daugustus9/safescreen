"""Extract members from a remote ZIP using HTTP range requests (no full download).

usage: python -I fetch_zip_member.py URL OUT_DIR [NAME_SUBSTRING ...]
With no substrings, lists the archive.
"""
import io
import sys
import urllib.request
import zipfile


class HttpRangeFile(io.RawIOBase):
    def __init__(self, url):
        self.url = url
        self.pos = 0
        req = urllib.request.Request(url, method="HEAD")
        with urllib.request.urlopen(req, timeout=60) as r:
            self.size = int(r.headers["Content-Length"])

    def seekable(self):
        return True

    def readable(self):
        return True

    def tell(self):
        return self.pos

    def seek(self, offset, whence=0):
        if whence == 0:
            self.pos = offset
        elif whence == 1:
            self.pos += offset
        else:
            self.pos = self.size + offset
        return self.pos

    def read(self, n=-1):
        if n is None or n < 0:
            n = self.size - self.pos
        if n == 0 or self.pos >= self.size:
            return b""
        end = min(self.pos + n, self.size) - 1
        for attempt in range(5):
            try:
                req = urllib.request.Request(self.url, headers={"Range": f"bytes={self.pos}-{end}"})
                with urllib.request.urlopen(req, timeout=120) as r:
                    data = r.read()
                break
            except Exception:
                if attempt == 4:
                    raise
        self.pos += len(data)
        return data

    def readinto(self, b):
        data = self.read(len(b))
        b[: len(data)] = data
        return len(data)


def main():
    url, out_dir, *wanted = sys.argv[1:]
    raw = io.BufferedReader(HttpRangeFile(url), buffer_size=1 << 20)
    with zipfile.ZipFile(raw) as z:
        for info in z.infolist():
            hit = any(w.lower() in info.filename.lower() for w in wanted)
            print(("* " if hit else "  ") + f"{info.filename}  {info.file_size}")
            if hit:
                z.extract(info, out_dir)


if __name__ == "__main__":
    main()
