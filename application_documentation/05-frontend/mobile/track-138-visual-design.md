| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Last Updated** | 2026-09-08 |
| **Version** | 2.0.0 |
| **Author** | TRACK-138 Rasika mobile |

# Rasika visual design

Source of truth for the Rasika visual identity across **both** surfaces — the Compose mobile app and the React admin web console. The canonical token values are the ones shipped in
[`RasikaTheme.kt`](../../../modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/theme/RasikaTheme.kt)
(`RasikaTokens`); the web app mirrors them in `modules/frontend/sangita-admin-web/src/index.css`. Do not invent a second palette or type scale in either layer — repoint the tokens.

> **v2.0.0 reconciliation (2026-09-08).** v1 documented a quieter "concert programme" set
> (`saffron #B44A1F`, `templeTeal #1F4E5F`, system fonts, no ornament). The shipped Compose
> theme evolved past that into the richer **Tamil Nadu gopuram** palette below — polychrome
> cornice banding, a distinct saffron/teal/gold triad, and the Fraunces + Work Sans type
> pairing. That shipped code is now the single source of truth; the v1 values are retired.
> The web console adopts this same set so web and mobile read as one product.

The tone is a painted temple interior on handmade paper: warm, polychrome, and readable for long lyric lines — not a generic dashboard or neon “AI” look. The cornice band, more than colour alone, is what carries the temple-interior identity.

---

## Color

Values are sRGB hex, taken from `RasikaTokens` in `RasikaTheme.kt`. Use the Material 3 scheme mapping below (Compose) or the web token names in brackets; do not mix in unlisted accents. Gold and teal-light are **ornament fills only, never text colours** — any label under 14sp/14px uses `saffronDeep`, `inkSoft`, or `teal`.

### Light (default)

| Token (web var) | Hex | Use |
|:---|:---|:---|
| `saffronDeep` (`--color-primary-dark`) | `#A92815` | Small text, section labels, active-tab ink |
| `saffron` (`--color-primary`) | `#C1350F` | Primary action, active chip, selection, focus |
| `coral` | `#E0553F` | Ornament fills, cornice base band |
| `gold` | `#E9A838` | Rules, anga boundaries, kalaśam |
| `teal` (`--color-accent`) | `#1F6B70` | Secondary action, counts, links, raga identity |
| `tealLight` | `#2D8B90` | Ornament vault, cornice teal band |
| `cream` (`--color-background`) | `#FDF3E3` | Screen ground |
| `creamDeep` (`--color-surface-variant`) | `#FBE8CF` | Tab/side strip, notices, selected-nav fill |
| `paper` (`--color-surface-light`) | `#FFFFFF` | Cards |
| `ink` (`--color-ink-900`) | `#2A1A12` | Primary text |
| `inkSoft` (`--color-ink-500`) | `#7A5C4A` | Secondary text (5.5:1 on cream) |
| `hairline` (`--color-border-light`) | `rgba(169,40,21,.20)` | Card borders, dividers |
| `onDark` | `#FFF6E8` | Text over artwork and teal |

### Painted-cornice bands

The banded moulding that closes every screen header — the signature motif. Stacked top→bottom: green, gold, a teal band with a repeating cream scallop, coral base. Web renders it as a slim CSS band; Compose draws it in `RasikaOrnaments.PaintedCornice`.

| Token | Hex |
|:---|:---|
| `corniceGreen` | `#7BA23F` |
| `corniceGold` | `#F2C53D` |
| `tealLight` (band) | `#2D8B90` |
| `corniceScallop` | `#FDF3E3` |
| `cornicePink` (base) | `#E07A8A` |

### Dark

Dark is provisional (an R7 dark pass is still owed) but shipped in `RasikaTokens` and mirrored on web.

| Token | Hex | Use |
|:---|:---|:---|
| `paperDark` | `#161210` | Screen background |
| `cardDark` | `#231C18` | Cards, sheets, fields, side strip |
| `inkDark` | `#F3EBE0` | Primary text |
| `inkSoftDark` | `#B6A598` | Secondary text |
| `saffronDark` | `#E08A4C` | Primary |
| `saffronSoftDark` | `#3A2418` | Selected chip |
| `tealDark` | `#7FB3C3` | Secondary |
| `goldDark` | `#8A7340` | Hairlines |
| `errorDark` | `#E08A84` | Errors |

**Contrast:** body text on `cream` / `paper` must stay above WCAG AA. Do not place saffron text on saffron fills.

**Material 3 mapping** (see `LightScheme` / `DarkScheme` in `RasikaTheme.kt` for the full map)

| Role | Light | Dark |
|:---|:---|:---|
| `primary` | saffron | saffronDark |
| `onPrimary` | cream | ink |
| `secondary` | teal | tealDark |
| `background` / `surface` | cream | paperDark |
| `surfaceContainer*` | paper | cardDark |
| `onSurface` | ink | inkDark |
| `onSurfaceVariant` | inkSoft | inkSoftDark |
| `outline` | hairline / gold | goldDark |
| `error` | saffronDeep | errorDark |

Appearance follows the stored preference on mobile (system, light, or dark) and `prefers-color-scheme` on web.

---

## Type

Two families, shared by mobile and web: **Fraunces** (serif) carries titles and lyrics; **Work Sans** carries labels, body and the tracked uppercase eyebrows. Indic scripts (Devanagari / Tamil / Telugu / Kannada) fall back to the platform font automatically — Fraunces is Latin-only, used for transliterations. Sizes below are the shipped `rasikaTypography` values.

| Role (M3) | Size / line / weight | Family | Use |
|:---|:---|:---|:---|
| displayLarge | 28 / 32 / Medium | Fraunces | Kriti title on the reader |
| titleLarge | 25 / 29 / Medium | Fraunces | Screen titles |
| titleMedium | 16 / 21 / Medium | Fraunces | Item / result / setting titles, `KrithiCard` name |
| bodyLarge | 14 / 20 / Regular | Work Sans | English UI copy, search input, helper text |
| bodyMedium (lyric) | 17.5 / 33 / Regular | Fraunces | Stored lyric text (scales with text-size preference) |
| labelLarge | 10.5 / 15 / SemiBold, 0.16em | Work Sans | Section labels, tab labels — uppercase, saffron |
| labelMedium | 10.5 / 15 / Regular, 0.04em | Work Sans | Meta lines: composer, raga, tala, counts |

Lyric line height stays ≥ 1.9× the font size so Indic stacked mātras don't clip. Text-size preference multiplies lyric (and body): `small` 0.86, `medium` 1.0, `large` 1.14, `extraLarge` 1.32.

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
| Card radius | 14 |
| Chip / field / button radius | 10 |
| Card elevation | 0 (1 dp `hairline` instead of drop shadow) |
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
