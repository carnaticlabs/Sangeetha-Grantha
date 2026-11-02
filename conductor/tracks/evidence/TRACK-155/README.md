# TRACK-155 evidence

iPhone 17, iOS 27.2 simulator, `make mobile-ios` build against the local backend (dev database `sangeetha-grantha-db-1`). Screenshots are numbered in walk order. Scrolling was driven by synthetic swipes, so before-open and after-back frames differ by a row where momentum was still running when the tap landed; compare the row that was opened.

| File | Shows |
|:---|:---|
| `baseline-1b-opened-mahalingaswami-from-scrolled-list.jpg` | Before the fix: list scrolled to the Chola middle, Mahalingaswami opened |
| `baseline-2-list-after-back-reset-to-top.jpg` | Before the fix: back returns the list to the top (defect 1 reproduced) |
| `after-1-list-scrolled-before-open.jpg`, `after-1b-opened-mahalingaswami.jpg`, `after-2-list-after-back-restored.jpg` | After the fix: same scroll, a row opened, back restores the position of the tap |
| `after-3-tiruvarur-scrolled-before-open-kriti.jpg`, `after-3b-kriti-reader-opened.jpg`, `after-4-tiruvarur-after-back-restored.jpg` | After the fix: Tiruvarur (114 kritis, lazy list) scrolled deep, a kriti opened, back restores the position |
| `after-5-list-after-back-from-tiruvarur.jpg` | Back again to the list: same position as before opening Tiruvarur |
| `after-6-fresh-visit-starts-at-top.jpg` | Leaving to Home and opening Kshetras again starts at the top (back-only restore) |
| `after-7a-agni-filter.jpg`, `after-7b-filter-cleared-at-top.jpg` | Filter on then cleared after a small scroll: list is at the top |

Measured on the live API: `GET /v2/catalogue/temples/01930000-0000-7000-8000-000000000001` is 44,169 bytes and answered in 85 ms (single request, warm), so the server and payload are not the cost.

Not captured (see the track Progress Log): a frame-time trace of Tiruvarur before the change, a VoiceOver pass, and the Collections/Browse baseline.
