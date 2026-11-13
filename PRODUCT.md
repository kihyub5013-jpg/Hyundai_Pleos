# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

Android Automotive OS on Pleos Connect (Hyundai Motor Group). Runs in the Pleos app window next to the fixed driving view; must work in a single portrait window, a landscape window spanning two app areas, and the narrow half of a split screen (Connect-S 1:1, Connect-W 2:1 / 1:2).

## Users

Primary: drivers aged 60+ who keep driving (licence-return rate stays near 2.5%). In the driver's seat they barely look at the screen; the app talks to them. They look at the screen for longer only while parked: choosing their promises, the pre-departure brief, and the monthly record.

Secondary: their adult children (30s–50s), who see only a one-line monthly summary when the driver chooses to share it.

Evaluators: Pleos contest judges, who see it in the Pleos Connect emulator with Virtual Console.

## Product Purpose

A safe-driving companion that keeps the conditions the driver set for themself (no night driving, avoid highways, near places only in rain, rest every hour, night only with a companion), and in a risky moment tells them by voice which pedal they are pressing. Success: the driver keeps driving with fewer risky conditions and catches pedal confusion in parking lots and right after starting off.

## Positioning

Not a licence-return nudge and not a driving score. The driver writes their own conditions ("self-conditional driving") and the car keeps them, using signals a phone cannot read: brake pedal, gear, speed, turn signal, wiper, low beam, navigation turn and caution data. Pedal guidance works at any speed, unlike hardware pedal-misapplication devices that work only below ~15 km/h.

## Operating Context

- In-car, glance time 1–2 s while driving; all guidance is voice-first (on-device TTS), screen is secondary.
- While driving, CarUxRestrictions limit the app to the "today" screen with large controls and voice.
- Pedal alerts appear full-screen and interrupt speech; in reverse the system rear camera covers the app, so voice carries the alert.
- Day and night use; Pleos requires day/night mode support.

## Capabilities and Constraints

- Features: pedal-check guidance (gas instead of brake, brake not hard enough, lurch from stop), surprise-spot memory with proximity alerts, five self-set promises, pre-departure brief (car status, arrival vs sunset, route preview), in-drive coaching (left turn/roundabout preview, turn-signal helper, caution points, rain, dusk headlight, hourly rest), parking reminder and parked location, voice commands, monthly promise record, family-share preview.
- The app never controls the vehicle. No cognitive scoring or diagnosis.
- Pleos SDK: Vehicle, NaviHelper, TextToSpeech, SpeechToText, FusedLocation; AAOS PERF_VEHICLE_SPEED for speed.
- Family-share server is not built yet (preview only).

## Brand Commitments

- Must sit naturally inside the Pleos Connect system UI (user requirement): Pleos design tokens, Asta Sans, Pleos day/night palettes.
- Pleos UI Design Guide rules are binding: glanceable in 1–2 s, contrast ≥ 4.5:1, key info (warnings) ≥ 4 mm tall, general text ≥ 3 mm and ≥ 18 px, one message ≤ 3 lines, internationally recognized icons with text labels, no video/animated images/auto-scrolling text while driving, response ≤ 0.25 s, important functions on the first screen, avoid dropdowns and multi-level menus, responsive to window size/orientation changes.

## Evidence on Hand

- Proposal with statistics and sources: docs/proposal.md.
- Emulator verification of pedal guidance (README, section 8).
- No real user testimonials, no field data, no insurer or government partnerships: do not fabricate any.

## Product Principles

1. The car speaks first; the screen confirms.
2. The driver's own promises, never a comparison or a score.
3. Calm by default, unmistakable when it matters: one alarm state, used only for pedal alerts.
4. Native to Pleos: it should read as part of the car, not a third-party dashboard.

## Accessibility & Inclusion

Older eyes: large type, high contrast, short sentences, no reliance on color alone (every state also has a word). One-handed touch with large targets.
