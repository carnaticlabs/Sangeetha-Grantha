| Metadata | Value |
|:---|:---|
| **Status** | In Progress — mobile (Phase 2) implemented; map link (Phase 3) pending TRACK-154 |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-28 |
| **Author** | Sangita Grantha Team |

# Rasika kshetra exploration

Owner: TRACK-151 (Intent, Spec and Plan live on the track file under `conductor/tracks/`). The backend contract is `GET /v2/catalogue/temples` and `GET /v2/catalogue/temples/{id}` (see [Catalogue API](../../03-api/README.md)); this page records what the mobile app does with it.

## What a rasika sees

- **Home** shows a Kshetras shelf beside Collections. It is supplementary: a failed or empty load shows nothing.
- **Kshetras list** groups the root temples by mandalam, in enum order (Chola, Pandya, Tondai, Nadu, Chera, Kongu, Uttara), with temples that carry no mandalam under "Other places" last. A group with no temple never renders a header. An element chip row filters by bhuta; it offers only the elements present in the loaded list, and `APPU` is glossed as water.
- **Rows** show name, place, published kriti count and, for a complex, "N shrines". A recorded gap is marked with the words "Recorded gap", never by colour alone.
- **Detail** shows the stored overview, the sthala fields that are present (absent fields are omitted, never "unknown"), the parent link for a child shrine, nested child shrines (each opens its own detail) and the kritis sung there, each with its composer. A recorded gap shows the overview as the reason and no kriti list.
- **Reader** shows a "Sung at <temple>" chip. It is a link only once the temple's detail has been served; a temple that returns 404 stays a plain label.
- **Search** cards of kind KSHETRA (TRACK-152) open the same detail screen.

## Honest coverage

Only Dikshitar kritis currently carry a temple. Copy never claims the list covers every kshetra of Carnatic music, and a kriti with no temple is never labelled "no kshetra": an empty `temple_id` means unreviewed.

## Not here

The "Open in Google Maps" link is Phase 3 and waits for TRACK-154 to land confirmed place ids; the detail DTO already carries an optional `place`, and the app ignores it until then. The embedded SDK map is a separate follow-up track.
