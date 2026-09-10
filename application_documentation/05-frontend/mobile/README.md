| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Navigation |

# Rasika mobile implementation

---

Rasika is the public Kotlin Multiplatform app for Android and iOS. It uses a shared Compose UI, a V2 catalogue client, and local bookmark/preferences storage. The [mobile PRD](../../01-requirements/mobile/prd.md) defines the current release boundary.

## Architecture and navigation

| Module | Owns |
|:---|:---|
| [shared/domain](../../../modules/shared/domain) | Public DTOs and domain enums |
| [shared/mobile-data](../../../modules/shared/mobile-data) | Ktor V2 client, repositories, local document storage, fixtures |
| [shared/presentation](../../../modules/shared/presentation) | Home/Explore/Library/Settings, reader, entity screens and presenters |
| [mobile/androidApp](../../../modules/mobile/androidApp) | Android host and instrumentation |
| [mobile/iosApp](../../../modules/mobile/iosApp) | iOS host and UI test targets |

Home keeps search available independently of discovery. Explore searches compositions, ragas, and composers with submitted queries, UUID filters, and recoverable paging. Library stores bookmarks on the device. Settings includes persistent appearance; the reader preserves the chosen stored variant and source labels.

## Build and verify

```bash
make test-mobile
make mobile-android
make mobile-ios
```

Native journeys require an explicit device:

```bash
bash tools/mobile/verify-rasika-journeys.sh android <device-serial>
bash tools/mobile/verify-rasika-journeys.sh ios <simulator-udid>
```

Replace the placeholders with an available device identifier; these commands are examples, not literal shell invocations. The harness rejects generic destinations. Hosted CI compiles native targets but does not establish full device runtime proof.

TRACK-140 acceptance still includes native journeys and manual TalkBack/VoiceOver verification. Read the [evidence report](../../10-implementations/track-140-rasika-discovery.md) for what was actually executed.

## Data and transport

`KtorCatalogueApi` calls `/v2/catalogue`, including discovery. Its DTOs omit editorial fields and include published unclassified compositions. V2's later metadata directories are not mounted yet. [API contract](../../03-api/api-contract.md) covers parameters, visibility, and errors.

Android emulator debug uses `http://10.0.2.2:8080`; iOS simulator debug uses `http://127.0.0.1:8080`. Release transport requires HTTPS. Physical devices need an explicitly reachable server address.

Local persistence protects future/corrupt documents and serializes writes. Bookmarks are not offline lyric packs or account sync. Source changes and unavailable compositions still need current network/error handling when opening a saved work.

## Design references

- [Discovery and reading specification](./rasika-discovery-experience.md): current TRACK-140 behavior and later release requirements.
- [TRACK-138 visual design](./track-138-visual-design.md): foundation and token decisions.
- [Broader UI proposal](./ui-specs.md): earlier intended experiences beyond the current release.
- [Quality guide](../../07-quality/README.md): shared, native, browser, and live-data evidence boundaries.

---

[Section index](./../README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
