# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

Two Android surfaces: a Pleos Connect car app (AAOS, Kotlin + Jetpack Compose, `apps/android`) and a companion Android phone app. iOS is out of scope.

## Users

Primary: owners of the same car model and their clubs (동호회), mostly 20s to 40s, driving Pleos Connect cars. They already compare fuel efficiency and show off their cars in owner communities, using dashboard photos and posts. They use Moli in two situations:

- In the car: driving (eyes on the road, gloved or hurried touch, glance only) or parked.
- On the phone, after getting out: reading the feed, checking rankings, posting, managing groups.

Secondary, confirmed: friends and family who drive together (group drives, walkie-talkie). Drivers who want cheaper fuel or charging benefit from price sharing.

## Product Purpose

Moli is a driver community where your own car is your identity. The car produces facts: measured efficiency, detected fuel and charge events with real prices, location during a group drive, and the car you customized. The phone app turns those facts into community activity, and the community's information flows back into driving.

Success for the contest: a live emulator demo at the second round (PT on 2026-12-03 to 04) showing the car-to-community loop.

## Positioning

What the car measures cannot be faked by a screenshot. Efficiency rankings use values the car app computes from vehicle signals (idle time excluded), and price reports come from fuel and charge events the car detected. Generic owner communities rely on self-reported photos; car logbook apps rely on manual input; neither can coach you while driving or connect to the car's navigation and cabin lights.

The product deliberately is not a car logbook (차계부). Maintenance items and service intervals were removed on 2026-11-15. Only information drivers are curious about in each other is kept: fuel, charging, efficiency, prices.

## Operating Context

- The car app runs next to the Pleos driving view, in two-thirds of the screen by default, or one-third when two apps share it. Pleos has no app-level bottom navigation; the GNB is system UI.
- While driving, Mobility Services category rules apply: no reading or writing posts in the car; use the ambient light, voice, and at most one card with two buttons.
- Car and phone link once by scanning a QR code on the car screen and logging in on the phone. After that, data syncs both ways through the server.
- The backend is NestJS + PostgreSQL + Redis (`apps/backend`), currently a scaffold.
- The demo is driven through the Pleos Connect Emulator and Virtual Console.

## Capabilities and Constraints

Starting point (2026-11-15): the repository was reset to scaffolds only (Pleos Connect car app, NestJS server) plus the platform research. Nothing below is built in the current tree. The earlier prototype is kept on the local branch `backup/before-reset-2026-11-15` and proved these on the emulator, so they are known to be feasible:
- 3D car customization with the chosen ambient light color applied to the cabin (Vehicle SDK Light).
- Eco-driving coach turning the ambient light orange or red on hard acceleration, hard braking or speeding.
- Charge detection from the charge port and battery energy; fuel detection only via demo data (the emulator car is an EV).
- Adding a charger in the direction of travel as a navigation waypoint (NaviHelper), ranked by preferred brands.

Planned: the car app (my car customization, eco coach, fuel and charge records, group drive list, walkie-talkie), the phone app (feed, ranking, map, groups, my car), QR account link, efficiency league and proof cards, automatic price reports, group live location, push-to-talk walkie-talkie (record while pressed, relayed by the server).

Constraints:
- Third-party apps cannot use the Pleos profile service; accounts come from our own server via the QR link.
- The emulator car is an EV; fuel %, fuel economy and airbag signals are not supported there.
- Location is shared only during a group drive and not kept afterwards. The server receives only consented summaries (car model, weekly average efficiency), price reports, and group-drive location.

Open: whether walkie-talkie is allowed while driving under the category policy.

## Brand Commitments

- Name: **Moli** (몰리), short for Mobility Life. Replaces the working name 차곡, which still appears in code and older docs.
- Korean UI copy in plain, polite sentence style (해요체).
- Design is not bound to the Pleos visual style; follow the Pleos guide's safety and size rules only.

## Evidence on Hand

- Emulator-verified SDK behavior and contest rules: `docs/PLEOS_PLATFORM_RESEARCH.md`; design rules: `docs/UI_DESIGN_GUIDE.md`, `docs/APP_DESIGN_GUIDE.md`, `docs/APP_DESIGN_PRINCIPLES.md`.
- On the local backup branch only (not in the current tree): the community-direction proposal draft (`docs/PROPOSAL.md` v2.0), a reference board of real Pleos screens, and a first phone app design.
- No user research, testimonials, community size figures or install numbers exist yet; do not invent them.

## Product Principles

1. The car states facts; people talk about them. Every number shown to the community comes from the car, with its source visible.
2. Eyes on the road. In the car while driving, speak through light and voice, not text.
3. Your car is you. Identity in the community is the car you customized, not a face or nickname.
4. Share only what drivers want from each other. Fuel, charging, efficiency, prices. No logbook chores.
5. Give back to the drive. Community information returns as a better route, a cheaper station, a friend to follow.

## Accessibility & Inclusion

- Car UI: body text at least 26sp, touch targets sized for gloved fingers, key information readable in one to two seconds, Light and Dark themes (the car switches automatically).
- Text contrast at least 4.5:1 in both themes.
