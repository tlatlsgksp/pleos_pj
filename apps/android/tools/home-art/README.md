# Home card art

`res/drawable-nodpi/home_art_*.webp` are photographs from Unsplash, free under the
[Unsplash License](https://unsplash.com/license) (commercial use allowed, attribution not required but kept here).
None is an Unsplash+ image.

| File | Photo | Photographer |
|---|---|---|
| home_art_ride.webp | https://unsplash.com/photos/a-foggy-highway-filled-with-lots-of-traffic-mN_eQJdRDdY | Ivan Bogdanov |
| home_art_report.webp | https://unsplash.com/photos/orange-and-white-traffic-cone-on-gray-asphalt-road-during-daytime--Pda66VYkNI | Nathan Dumlao |
| home_art_charger.webp | https://unsplash.com/photos/xfaYAsMV1p8 | CHUTTERSNAP |
| home_art_eco.webp | https://unsplash.com/photos/a-foggy-road-winds-through-a-dark-forest-NnzwWtZW3_k | Raymond Petrik |
| home_art_light.webp | https://unsplash.com/photos/SvY5byRf-Iw | Tim Bernhard |

Image ids: ride `photo-1555689502-e6d568a741e9`, report `photo-1584839401475-4060f658feab`,
charger `photo-1593941707882-a5bba14938c7`, eco `photo-1759660019333-a59e47a1e1fa`,
light `photo-1772555429170-be39986f4d99`.

## Grade

All five get the same treatment so they read as one set: a 1200x800 (1.5:1) crop from Unsplash, then
saturation 60%, contrast 96%, brightness 104%, a cool gray-blue tint (`#7E93B0` at 14% in `color` blend
plus `#8FA3BD` at 7%), rendered with headless Chrome and converted with `cwebp -q 82`.

```sh
curl -o ride.src.jpg "https://images.unsplash.com/photo-1555689502-e6d568a741e9?w=1200&h=800&fit=crop&q=85&fm=jpg"
# ride.html: <img src="ride.src.jpg" style="filter:saturate(.6) contrast(.96) brightness(1.04)"> under the two tint layers above
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --hide-scrollbars --window-size=1200,800 \
  --screenshot=ride.png "file://$PWD/ride.html"
cwebp -q 82 ride.png -o ../../automotive/src/main/res/drawable-nodpi/home_art_ride.webp
```

`render.html` is the earlier three.js renderer of the bundled Ioniq 5 model; the cards no longer use its output.
