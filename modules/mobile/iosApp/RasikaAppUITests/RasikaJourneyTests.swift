import XCTest

/// TRACK-138 MVP journeys: launch smoke, favourite/restart, and connection-failure
/// copy that must not drop local bookmarks. Fixture records are synthetic.
final class RasikaJourneyTests: XCTestCase {

    private static let fixturesArgument = "-rasika.uiTest.fixtures"
    private static let resetStateArgument = "-rasika.uiTest.resetState"
    private static let offlineArgument = "-rasika.uiTest.offline"

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    @discardableResult
    private func launch(
        reset: Bool,
        fixtures: Bool = true,
        offline: Bool = false
    ) -> XCUIApplication {
        let app = XCUIApplication()
        var arguments: [String] = []
        if fixtures { arguments.append(Self.fixturesArgument) }
        if reset { arguments.append(Self.resetStateArgument) }
        if offline { arguments.append(Self.offlineArgument) }
        app.launchArguments = arguments
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        return app
    }

    private func element(_ app: XCUIApplication, _ label: String) -> XCUIElement {
        app.descendants(matching: .any)[label]
    }

    private func bottomMost(_ app: XCUIApplication, _ label: String) -> XCUIElement? {
        let query = app.descendants(matching: .any).matching(identifier: label)
        guard query.count > 0 else { return nil }
        return (0..<query.count)
            .map { query.element(boundBy: $0) }
            .filter { $0.exists }
            .max { $0.frame.minY < $1.frame.minY }
    }

    private func awaitElement(
        _ app: XCUIApplication,
        _ label: String,
        timeout: TimeInterval = 20,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        XCTAssertTrue(
            element(app, label).waitForExistence(timeout: timeout),
            "Expected an element labelled '\(label)'",
            file: file, line: line
        )
    }

    private func tap(
        _ app: XCUIApplication,
        _ label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        XCTAssertTrue(
            element(app, label).waitForExistence(timeout: 20),
            "Missing control '\(label)'",
            file: file, line: line
        )
        guard let target = bottomMost(app, label) else {
            XCTFail("No control labelled '\(label)'", file: file, line: line)
            return
        }
        if target.isHittable {
            target.tap()
        } else {
            target.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
    }

    private func openVatapiReader(_ app: XCUIApplication) {
        awaitElement(app, "Home")
        // Home's fixture feature is Vatapi. Opening it avoids flaky simulator
        // keyboard focus on the Explore search field (TRACK-138 close-out).
        if element(app, "Read composition").waitForExistence(timeout: 10) {
            tap(app, "Read composition")
        } else {
            tap(app, "Explore")
            let placeholder = "Title, incipit or a line of sahitya"
            tap(app, placeholder)
            app.typeText("Vatapi")
            tap(app, "Search")
            awaitElement(app, "Vatapi Ganapatim")
            tap(app, "Vatapi Ganapatim")
        }
        awaitElement(app, "Pallavi")
    }

    func testLaunchShowsSearch() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        let home = app.descendants(matching: .any)["Home"]
        XCTAssertTrue(home.waitForExistence(timeout: 20), "Expected the Home tab or title after launch")
    }

    func testFavouriteSurvivesRelaunch() {
        let first = launch(reset: true)
        openVatapiReader(first)
        tap(first, "Save favourite")
        tap(first, "Library")
        awaitElement(first, "Vatapi Ganapatim")
        first.terminate()

        let second = launch(reset: false)
        awaitElement(second, "Home")
        tap(second, "Library")
        awaitElement(second, "Vatapi Ganapatim")
    }

    func testConnectionFailureKeepsFavouritesAndShowsRetry() {
        let first = launch(reset: true)
        openVatapiReader(first)
        tap(first, "Save favourite")
        first.terminate()

        let offline = launch(reset: false, fixtures: false, offline: true)
        awaitElement(offline, "Home")
        tap(offline, "Explore")
        tap(offline, "Search")
        awaitElement(offline, "Could not reach the catalogue")
        awaitElement(offline, "Your favourites are still here. The search needs a connection.")
        tap(offline, "Library")
        awaitElement(offline, "Vatapi Ganapatim")
    }
}
