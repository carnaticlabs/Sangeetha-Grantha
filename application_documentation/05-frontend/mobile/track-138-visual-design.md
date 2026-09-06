| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Last Updated** | 2026-09-05 |
| **Version** | 1.0.0 |
| **Author** | TRACK-138 Rasika mobile |

# TRACK-138 Rasika visual design

Source of truth for Rasika Compose UI. Implement these tokens in `RasikaTheme`; do not invent a second palette or type scale. System fonts only for this MVP (on-device script shaping). No decorative generated assets.

The tone is a concert programme on handmade paper: warm, quiet, and readable for long lyric lines — not a generic dashboard or neon “AI” look.

---

## Color

Values are sRGB hex. Use the Material 3 scheme mapping below; do not mix in unlisted accents.

### Light (default)

| Token | Hex | Use |
|:---|:---|:---|
| `paper` | `#F6F0E6` | Screen background |
| `paperRaised` | `#FFFBF4` | Cards, sheets, fields |
| `ink` | `#1C1410` | Primary text |
| `inkMuted` | `#6B5B4F` | Secondary text, meta |
| `saffron` | `#B44A1F` | Primary actions, selected tab, focus |
| `saffronSoft` | `#F3D9C8` | Selected chip / subtle fill |
| `templeTeal` | `#1F4E5F` | Secondary (raga identity, links) |
| `goldLine` | `#C4A35A` | Hairline dividers, tab indicator |
| `error` | `#A33B32` | Errors only |
| `success` | `#2F6B4F` | Favourited / success only |

### Dark

| Token | Hex | Use |
|:---|:---|:---|
| `paper` | `#161210` | Screen background |
| `paperRaised` | `#231C18` | Cards, sheets, fields |
| `ink` | `#F3EBE0` | Primary text |
| `inkMuted` | `#B6A598` | Secondary text |
| `saffron` | `#E08A4C` | Primary |
| `saffronSoft` | `#3A2418` | Selected chip |
| `templeTeal` | `#7FB3C3` | Secondary |
| `goldLine` | `#8A7340` | Hairlines |
| `error` | `#E08A84` | Errors |
| `success` | `#7FBF9A` | Favourited |

**Contrast:** body text on `paper` / `paperRaised` must stay above WCAG AA. Do not place saffron text on saffron fills.

**Material 3 mapping**

| Role | Light | Dark |
|:---|:---|:---|
| `primary` | saffron | saffron |
| `onPrimary` | `#FFFBF4` | `#1C1410` |
| `secondary` | temple teal | temple teal |
| `background` / `surface` | paper | paper |
| `surfaceContainer` | paperRaised | paperRaised |
| `onSurface` | ink | ink |
| `onSurfaceVariant` | inkMuted | inkMuted |
| `outline` | goldLine | goldLine |
| `error` | error | error |

Appearance follows the stored preference: system, light, or dark.

---

## Type

Use the platform default sans (`FontFamily.SansSerif`). Do not bundle display fonts in this slice.

| Role | Size / line / weight | Use |
|:---|:---|:---|
| Display | 32 / 40 / 600 | Kriti title on the reader |
| Title | 22 / 28 / 600 | Screen titles, tab context |
| Card title | 17 / 24 / 600 | `KrithiCard` name |
| Body | 16 / 26 / 400 | English UI copy |
| Lyric | 18 / 32 / 400 | Stored lyric text (scale with text-size preference) |
| Section | 13 / 18 / 600 | Pallavi / Anupallavi / Charanam labels (letter-spacing 0.08em, saffron) |
| Meta | 13 / 18 / 400 | Composer, raga, tala, counts |
| Caption | 12 / 16 / 400 | Helper, timestamps |

Lyric line height stays at least 1.7× the font size so Indian scripts can shape. Text-size preference multiplies lyric and body only: `small` 0.9, `medium` 1.0, `large` 1.2, `extraLarge` 1.4.

---

## Space and shape

4 dp grid.

| Token | dp |
|:---|---:|
| `xxs` | 4 |
| `xs` | 8 |
| `sm` | 12 |
| `md` | 16 |
| `lg` | 24 |
| `xl` | 32 |
| `screen` | 20 horizontal inset |

| Token | dp |
|:---|---:|
| Card radius | 16 |
| Chip / field radius | 12 |
| Button radius | 12 |
| Card elevation | 0 (1 dp hairline `goldLine` instead of drop shadow) |
| Tap target | ≥ 48 |

---

## Components

**Tab bar.** Three destinations: Search, Browse, Favourites. Selected icon and label use saffron; unselected use `inkMuted`. Top hairline `goldLine`. No fourth tab in the MVP.

**Search field.** Full-width on paperRaised, 12 dp radius, saffron caret. Placeholder: “Search kritis by name”. Submit is explicit (keyboard search / button); do not debounce into the API.

**Krithi card.** Title, composer, ordered raga names (middot-separated), optional tala. Ragamalika shows a small teal “Ragamalika” label. Favourite is a bookmark affordance, not a heart.

**Load / empty / error.** Centered on the screen: short title, one-sentence explanation, primary Retry when the failure is retryable. Empty search: “No published kritis match.” Do not show skeleton carousels.

**Reader.** Title (display), composer + form + tala (meta), ordered raga list, then script/reading chips, then section label + lyric. Varnam / Swarajathi show a caption that lyrics are stored text, not notation.

**Chips.** Unselected: paperRaised + ink. Selected: saffronSoft fill + saffron outline. Multiple same-script readings stay separate chips with their stored labels.

---

## Motion

| Event | Duration | Easing |
|:---|:---|:---|
| Tab content crossfade | 180 ms | emphasized decelerate |
| Card press | 80 ms | standard |
| Error / empty appear | 160 ms | standard |

No looping animation, no parallax, no staggered list entrance. Honour reduced-motion: skip crossfade, swap instantly.

---

## Accessibility

- Tab, search, favourite, retry, and script/reading controls have accessible names from `strings.xml`.
- Contrast as above; selected tab is not colour-only (label weight 600).
- Large text must not clip section labels or the search field.
- Platform back pops the current tab stack; the last root tab does not exit the app from Compose.

---

## Out of scope for this document

Hero carousels, discovery feeds, notation rendering, and custom webfonts. Those belong to the older [mobile UI specification](./ui-specs.md), not the Rasika MVP.
