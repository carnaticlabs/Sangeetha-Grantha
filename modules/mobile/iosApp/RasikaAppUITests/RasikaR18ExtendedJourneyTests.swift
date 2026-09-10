import XCTest

/// TRACK-140 R18 journeys that were listed as residual after the first native pass:
/// multi-page Explore, bookmark toggle, script/source switching, Dynamic Type and
/// reduced-motion. VoiceOver itself remains a manual journey; these tests assert the
/// labelled accessibility tree and launch under content-size / reduce-motion settings.
final class RasikaR18ExtendedJourneyTests: XCTestCase {

    private static let fixturesArgument = "-rasika.uiTest.fixtures"
    private static let resetStateArgument = "-rasika.uiTest.resetState"

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    @discardableResult
    private func launch(
        reset: Bool,
        extraArguments: [String] = [],
        extraEnvironment: [String: String] = [:]
    ) -> XCUIApplication {
        let app = XCUIApplication()
        var arguments = reset
            ? [Self.fixturesArgument, Self.resetStateArgument]
            : [Self.fixturesArgument]
        arguments.append(contentsOf: extraArguments)
        app.launchArguments = arguments
        extraEnvironment.forEach { app.launchEnvironment[$0.key] = $0.value }
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

    private func scrollUntil(
        _ app: XCUIApplication,
        _ label: String,
        timeout: TimeInterval = 20
    ) {
        let start = Date()
        while !element(app, label).exists && Date().timeIntervalSince(start) < timeout {
            app.swipeUp()
        }
        awaitElement(app, label, timeout: 2)
    }

    private func openVatapiReader(_ app: XCUIApplication) {
        awaitElement(app, "Home")
        tap(app, "Explore")
        let placeholder = "Title, incipit or a line of sahitya"
        tap(app, placeholder)
        app.typeText("Vatapi")
        tap(app, "Search")
        awaitElement(app, "Vatapi Ganapatim")
        tap(app, "Vatapi Ganapatim")
        awaitElement(app, "Pallavi")
    }

    func testExploreEmptySearchLoadsTheNextPage() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        tap(app, "Explore")
        tap(app, "Search")
        scrollUntil(app, "Load more")
        tap(app, "Load more")
        scrollUntil(app, "Paging Fixture 31")
    }

    func testBookmarkToggleAppearsInLibraryAndSurvivesRelaunch() {
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
        tap(second, "Remove favourite")
        awaitElement(second, "No favourites yet")
    }

    func testReaderSwitchesToTheDevanagariSource() {
        let app = launch(reset: true)
        openVatapiReader(app)
        awaitElement(app, "Latin")
        tap(app, "Devanagari")
        awaitElement(app, "वातापि गणपतिं भजेहं")
    }

    func testLargeTextKeepsTabsReachable() {
        let app = launch(
            reset: true,
            extraArguments: [
                "-UIPreferredContentSizeCategoryName",
                "UICTContentSizeCategoryAccessibilityXXL",
            ]
        )
        awaitElement(app, "Home")
        for tab in ["Home", "Explore", "Library", "Settings"] {
            awaitElement(app, tab)
        }
        awaitElement(app, "Title, incipit or a line of sahitya")
    }

    func testReducedMotionKeepsTabsReachable() {
        let app = launch(
            reset: true,
            extraArguments: ["-UIAccessibilityIsReduceMotionEnabled", "YES"],
            extraEnvironment: ["UIAccessibilityIsReduceMotionEnabled": "YES"]
        )
        awaitElement(app, "Home")
        for tab in ["Explore", "Library", "Settings", "Home"] {
            tap(app, tab)
            awaitElement(app, tab)
        }
    }

    func testLabelledControlsCoverSearchAndAppearance() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        awaitElement(app, "Title, incipit or a line of sahitya")
        tap(app, "Settings")
        awaitElement(app, "Appearance")
        for choice in ["System", "Light", "Dark"] {
            awaitElement(app, choice)
        }
    }
}
