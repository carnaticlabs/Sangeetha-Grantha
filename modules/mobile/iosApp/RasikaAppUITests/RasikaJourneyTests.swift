import XCTest

/// TRACK-140 N01 baseline harness. Assertions evolve with U01–U03; GA owns full journeys.
final class RasikaJourneyTests: XCTestCase {
    func testLaunchShowsSearch() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        let home = app.descendants(matching: .any)["Home"]
        XCTAssertTrue(home.waitForExistence(timeout: 20), "Expected the Home tab or title after launch")
    }
}
