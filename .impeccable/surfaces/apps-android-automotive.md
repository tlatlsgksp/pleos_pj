---
version: 1
slug: "apps-android-automotive"
primary_target: "apps/android/automotive"
related_targets: []
---

# Car app (Pleos Connect) — surface brief

Scope: the Moli car app, every screen; first surface built is Home. Mode: Operate.
Audience: same-model owners and clubs driving Pleos Connect cars; glance while driving, full use when parked.
Constraints: app sits beside the Pleos driving view (2/3 window, 1/3 when two apps share). Touch targets for gloves, body >= 26sp. Light and Dark follow the car. UX copy follows docs/UX_WRITING.md.
Quality bar (canon): the Pleos Connect system UI and Android Automotive (Google built-in) apps, at full craft.

## Direction contract

THESIS: The category standard executed cleanly: neutral grey ground, white cards, ink buttons, nothing decorative. It refuses only one default: things on the road ahead are not shown as generic chips but as real road guide signs.

OWN-WORLD: Pleos grayscale tokens (ground #F5F6F8 / dark #131417, cards white / #1D1E21, ink #1D1E21), SUIT type on the Pleos type scale, 16dp card radius, no shadows except the floating menu. Kick point: guide-sign plates with a white inner border and bold arrow, green #0B6B45 for destinations, blue #1C5BA8 for waypoints and stations, amber #F2C230 with ink for reports; used only when an item lies on the route.

STORY: The driver sees their car, their group ride and four one-tap actions; anything waiting on the road reads as a sign before they reach it.

FIRST VIEWPORT: Home in the 2/3 window. Row 1: my-car card (customised car above, nickname, model, weekly efficiency and rank below) and group-riding card (state, members, one or two buttons, destination as a green sign). Row 2: four quick-action cards: report (amber sign when reports lie ahead), charger (blue sign for the suggested station), eco coach, mood light. Floating vertical menu bottom-left over the cards, folding to a handle after 5 s or on scroll.

DRIVER SIDE: left-hand drive, and the app window sits on the right two-thirds of the display, so the window's left edge is nearest the driver. Primary actions, close buttons, the menu and segmented controls sit left; the right edge carries values and secondary information only; no CTA bottom-right. Modals rise from the bottom as sheets with an explicit close button on the left.

FORM: canon (category standard), user's choice; road-sign kick point added at the user's request ("플랫폼 표준에 킥포인트로 도로표지판"). seed:66758aa70dc6a172

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
