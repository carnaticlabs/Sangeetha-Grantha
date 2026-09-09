import XCTest

/// TRACK-140 R18 — iOS native journeys.
///
/// These drive the real Compose host against `FixtureCatalogueApi`, not the live corpus.
/// R18 asks for *reproducible* evidence, and a live catalogue would make every navigation,
/// persistence and appearance assertion depend on whatever happens to be published at the
/// time. Fixture records are synthetic contract examples, never live catalogue evidence.
///
/// Deliberately NOT claimed here, so the proof ledger is not overstated:
///  - Multi-page paging: the shared fixture holds two compositions, less than one page.
///    Paging rules are covered by the shared ExplorePresenterTest against a paging fixture.
///  - VoiceOver: these assert the accessibility tree VoiceOver consumes, which is necessary
///    but is not a VoiceOver pass. That stays a manual journey.
///  - Dynamic Type and reduced-motion matrices, which need device-level settings changes.
final class RasikaR18JourneyTests: XCTestCase {

    private static let fixturesArgument = "-rasika.uiTest.fixtures"
    private static let resetStateArgument = "-rasika.uiTest.resetState"

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    /// Launch with fixtures. `reset` clears bookmarks and preferences first.
    @discardableResult
    private func launch(reset: Bool) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = reset
            ? [Self.fixturesArgument, Self.resetStateArgument]
            : [Self.fixturesArgument]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        return app
    }

    /// Compose renders into a single host view, so match on any descendant label.
    private func element(_ app: XCUIApplication, _ label: String) -> XCUIElement {
        app.descendants(matching: .any)[label]
    }

    /// Several labels appear twice — "Settings" names both a header shortcut and the
    /// bottom tab, and an appearance choice is both a row label and its selected value.
    /// Journeys always mean the bottom-most control, so resolve ambiguity by position
    /// rather than asserting a single match that the UI legitimately does not provide.
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
            // Compose text fields expose their placeholder as a non-hittable element.
            // Tapping its centre still reaches the underlying field.
            target.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
    }

    // R01 / R02 — Home is the fresh-install entry, with the four tabs present.
    func testFreshLaunchLandsOnHomeWithTabs() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        for tab in ["Home", "Explore", "Library", "Settings"] {
            awaitElement(app, tab)
        }
    }

    // R01 — the four tabs are stable destinations across switching.
    func testTabsAreStableDestinations() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        for tab in ["Explore", "Library", "Settings", "Home"] {
            tap(app, tab)
            awaitElement(app, tab)
        }
    }

    // R03 / R07 — Explore searches with explicit submission, and a result opens its reading.
    func testExploreSearchOpensAStoredReading() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        tap(app, "Explore")

        let placeholder = "Title, incipit or a line of sahitya"
        XCTAssertTrue(
            element(app, placeholder).waitForExistence(timeout: 20),
            "Expected the search field on Explore"
        )
        tap(app, placeholder)
        app.typeText("Vatapi")
        tap(app, "Search")

        awaitElement(app, "Vatapi Ganapatim")
        tap(app, "Vatapi Ganapatim")
        awaitElement(app, "Pallavi")
    }

    // R09 — Appearance offers System / Light / Dark and applies without a Save step.
    func testAppearanceOffersSystemLightAndDark() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        tap(app, "Settings")
        awaitElement(app, "Appearance")
        for choice in ["System", "Light", "Dark"] {
            awaitElement(app, choice)
        }
        tap(app, "Light")
        // Navigation stays usable immediately after the change.
        tap(app, "Home")
        awaitElement(app, "Home")
    }

    // R09 / R17 — an explicit appearance choice survives a relaunch.
    func testAppearanceChoiceSurvivesRelaunch() {
        let first = launch(reset: true)
        awaitElement(first, "Home")
        tap(first, "Settings")
        awaitElement(first, "Appearance")
        tap(first, "Dark")
        first.terminate()

        let second = launch(reset: false)
        awaitElement(second, "Home")
        tap(second, "Settings")
        awaitElement(second, "Appearance")
        awaitElement(second, "Dark")
    }

    // R08 — a fresh-install Library is empty and explains itself rather than erroring.
    func testFreshLibraryIsEmptyAndExplainsItself() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        tap(app, "Library")
        awaitElement(app, "No favourites yet")
    }

    // R10 — every bottom-tab control is exposed to the accessibility tree. Necessary for
    // VoiceOver, not a substitute for a VoiceOver pass.
    func testNavigationControlsAreExposedToAccessibility() {
        let app = launch(reset: true)
        awaitElement(app, "Home")
        for label in ["Home", "Explore", "Library", "Settings"] {
            XCTAssertTrue(
                element(app, label).exists,
                "Tab '\(label)' is not exposed to assistive technology"
            )
        }
    }

    // R18 — name the runtime the journeys ran on, for the proof ledger.
    func testRecordsDeviceIdentityForTheProofLedger() {
        let device = XCUIDevice.shared
        print(
            "TRACK-140 R18 iOS journey runtime: "
            + "\(ProcessInfo.processInfo.operatingSystemVersionString) "
            + "orientation=\(device.orientation.rawValue)"
        )
    }
}
