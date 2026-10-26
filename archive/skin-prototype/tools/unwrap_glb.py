"""Wrap template generator for the body mesh of a car GLB.

Splits body triangles by facing direction (left, right, top, front, rear, bottom), projects each group
orthographically as seen from outside, and lays the views out at one real-world scale, like a vinyl wrap
print sheet. Writes:
  bodyuv.txt      base64 Float32 UVs, one pair per vertex of the non-indexed body geometry (index order)
  template.png    the printable template (panel outlines, labels, scale bar)
  wrap-fleet.png  an example fleet livery drawn on the template (transparent where unwrapped)
Axes of this model: +Y up, +Z front, +X the car's left side.
"""
import base64, json, struct, sys
from PIL import Image, ImageDraw, ImageFont

GLB, OUT = sys.argv[1], sys.argv[2]
PX_PER_M = 400          # 1 px = 2.5 mm
MARGIN = 0.18           # metres between views

b = open(GLB, "rb").read()
clen = struct.unpack("<I", b[12:16])[0]
g = json.loads(b[20:20 + clen])
off = 20 + clen
binc = b[off + 8: off + 8 + struct.unpack("<I", b[off:off + 4])[0]]
CT = {5126: ("f", 4), 5125: ("I", 4), 5123: ("H", 2), 5121: ("B", 1)}
NC = {"SCALAR": 1, "VEC2": 2, "VEC3": 3}


def read(ai):
    a = g["accessors"][ai]
    bv = g["bufferViews"][a["bufferView"]]
    fmt, sz = CT[a["componentType"]]
    n = NC[a["type"]]
    stride = bv.get("byteStride", sz * n)
    start = bv.get("byteOffset", 0) + a.get("byteOffset", 0)
    return [struct.unpack_from("<" + fmt * n, binc, start + i * stride) for i in range(a["count"])]


body_mesh = next(i for i, m in enumerate(g["meshes"])
                 if g["materials"][m["primitives"][0]["material"]]["name"] == "M_Gravity_Gold_Matte")
prim = g["meshes"][body_mesh]["primitives"][0]
pos = read(prim["attributes"]["POSITION"])
idx = [i[0] for i in read(prim["indices"])]
tris = [tuple(pos[k] for k in idx[t:t + 3]) for t in range(0, len(idx), 3)]


def sub(a, c): return (a[0] - c[0], a[1] - c[1], a[2] - c[2])


def cross(a, c): return (a[1] * c[2] - a[2] * c[1], a[2] * c[0] - a[0] * c[2], a[0] * c[1] - a[1] * c[0])


# view -> (axis index, sign) of the outward normal, and the 2D projection as seen from outside (u right, v down)
VIEWS = {
    "left":   lambda p: (-p[2], -p[1]),   # standing at the car's left, front is on your left
    "right":  lambda p: (p[2], -p[1]),    # standing at the car's right, front is on your right
    "top":    lambda p: (p[2], -p[0]),    # from above, front to the right, left side at the top
    "front":  lambda p: (p[0], -p[1]),
    "rear":   lambda p: (-p[0], -p[1]),
    "bottom": lambda p: (p[2], p[0]),
}


def classify(n):
    ax = max(range(3), key=lambda i: abs(n[i]))
    s = n[ax] > 0
    return {0: ("left" if s else "right"), 1: ("top" if s else "bottom"), 2: ("front" if s else "rear")}[ax]


groups = {k: [] for k in VIEWS}
for ti, t in enumerate(tris):
    n = cross(sub(t[1], t[0]), sub(t[2], t[0]))
    groups[classify(n)].append(ti)

proj = {}
bbox = {}
PRINTED = ("left", "right", "top", "front", "rear")
for k, faces in groups.items():
    pts = [VIEWS[k](p) for ti in faces for p in tris[ti]]
    if not pts:
        continue
    us, vs = [p[0] for p in pts], [p[1] for p in pts]
    bbox[k] = (min(us), min(vs), max(us), max(vs))
    proj[k] = faces

w = lambda k: bbox[k][2] - bbox[k][0]
h = lambda k: bbox[k][3] - bbox[k][1]

# Layout in metres: left, right, top stacked; front, rear, bottom on the last row.
place = {}
y = MARGIN + 0.25   # room for the sheet title
for k in ("left", "right", "top"):
    place[k] = (MARGIN, y)
    y += h(k) + MARGIN + 0.12
x = MARGIN
row_h = 0
for k in ("front", "rear"):
    if k in bbox:
        place[k] = (x, y)
        x += w(k) + MARGIN
        row_h = max(row_h, h(k))
sheet_w = max(MARGIN + max(w("left"), w("right"), w("top")) + MARGIN, x)
sheet_h = y + row_h + MARGIN + 0.2
W, H = int(round(sheet_w * PX_PER_M)), int(round(sheet_h * PX_PER_M))


def to_px(k, p):
    u, v = VIEWS[k](p)
    ox, oy = place[k]
    return ((ox + u - bbox[k][0]) * PX_PER_M, (oy + v - bbox[k][1]) * PX_PER_M)


def view_px(k, u, v):
    """Pixel of a point given in the view's own metres (u from its left edge, v from its top edge)."""
    ox, oy = place[k]
    return ((ox + u) * PX_PER_M, (oy + v) * PX_PER_M)


uv = [0.0] * (len(idx) * 2)
for k, faces in proj.items():
    for ti in faces:
        for c in range(3):
            # The underside is never printed: park it on one blank margin pixel so it shows the paint colour.
            px, py = (2, 2) if k == "bottom" else to_px(k, tris[ti][c])
            vi = ti * 3 + c
            uv[vi * 2] = px / W
            uv[vi * 2 + 1] = 1 - py / H     # canvas textures are flipped on upload (flipY)
open(f"{OUT}/bodyuv.txt", "w").write(base64.b64encode(struct.pack(f"<{len(uv)}f", *uv)).decode())

FONT = "/System/Library/Fonts/AppleSDGothicNeo.ttc"
f_title = ImageFont.truetype(FONT, 34)
f_label = ImageFont.truetype(FONT, 26)
f_small = ImageFont.truetype(FONT, 20)
LABEL = {"left": "왼쪽 옆면", "right": "오른쪽 옆면", "top": "위 (보닛·지붕·트렁크)",
         "front": "앞", "rear": "뒤", "bottom": "아래"}
ARROW = {"left": "◀ 앞", "right": "앞 ▶", "top": "앞 ▶"}

tpl = Image.new("RGB", (W, H), (255, 255, 255))
d = ImageDraw.Draw(tpl)
for k in PRINTED:
    for ti in proj[k]:
        d.polygon([to_px(k, p) for p in tris[ti]], fill=(228, 233, 238))
for k in PRINTED:
    for ti in proj[k]:
        d.polygon([to_px(k, p) for p in tris[ti]], outline=(196, 204, 212))
for k in PRINTED:
    lx, ly = view_px(k, 0, -0.1)
    txt = LABEL[k] + ("   " + ARROW[k] if k in ARROW else "")
    d.text((lx, ly - 18), txt, fill=(40, 48, 58), font=f_label)
d.text((MARGIN * PX_PER_M, 18), "차곡 랩 전개도 · IONIQ 5 대체 모델 · 1px = 2.5mm", fill=(20, 24, 30), font=f_title)
sx, sy = W - MARGIN * PX_PER_M - PX_PER_M, H - 0.12 * PX_PER_M
d.line([(sx, sy), (sx + PX_PER_M, sy)], fill=(20, 24, 30), width=4)
d.line([(sx, sy - 10), (sx, sy + 10)], fill=(20, 24, 30), width=3)
d.line([(sx + PX_PER_M, sy - 10), (sx + PX_PER_M, sy + 10)], fill=(20, 24, 30), width=3)
d.text((sx + PX_PER_M / 2 - 18, sy - 40), "1 m", fill=(20, 24, 30), font=f_small)
d.text((MARGIN * PX_PER_M, H - 0.16 * PX_PER_M),
       "회색 면만 차체에 입혀집니다. 유리·램프·그릴은 별도 부품이라 인쇄되지 않습니다. 투명한 곳은 차체 색이 보입니다.",
       fill=(90, 98, 108), font=f_small)
tpl.save(f"{OUT}/template.png")

# Example fleet livery: green band low on the sides, front and rear; company name on both doors.
GREEN = (11, 122, 59, 255)
DARK = (14, 26, 20, 255)
liv = Image.new("RGBA", (W, H), (0, 0, 0, 0))
mask = Image.new("L", (W, H), 0)
md = ImageDraw.Draw(mask)
for k in PRINTED:
    for ti in proj[k]:
        md.polygon([to_px(k, p) for p in tris[ti]], fill=255)
art = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ad = ImageDraw.Draw(art)
f_logo = ImageFont.truetype(FONT, int(0.20 * PX_PER_M))
f_sub = ImageFont.truetype(FONT, int(0.07 * PX_PER_M))
for k in ("left", "right", "front", "rear"):
    x0, y0 = view_px(k, 0, h(k) * 0.66)
    x1, y1 = view_px(k, w(k), h(k) * 0.80)
    ad.rectangle([x0, y0, x1, y1], fill=GREEN)
for k in ("left", "right"):
    cx, cy = view_px(k, w(k) * 0.5, h(k) * 0.50)
    ad.text((cx, cy), "한결물류", fill=DARK, font=f_logo, anchor="mm")
    ad.text((cx, cy + 0.15 * PX_PER_M), "FLEET 07 · 1588-0000", fill=DARK, font=f_sub, anchor="mm")
cx, cy = view_px("top", w("top") * 0.86, h("top") * 0.5)
ad.text((cx, cy), "07", fill=GREEN, font=ImageFont.truetype(FONT, int(0.45 * PX_PER_M)), anchor="mm")
liv.paste(art, (0, 0), mask)
liv.save(f"{OUT}/wrap-fleet.png")

print("faces", len(tris), "verts", len(idx), "sheet", W, "x", H, "px",
      {k: len(v) for k, v in proj.items()}, {k: (round(w(k), 2), round(h(k), 2)) for k in bbox})
