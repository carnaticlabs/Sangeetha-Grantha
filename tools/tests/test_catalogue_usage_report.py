import json
import unittest
from pathlib import Path
import importlib.util

ROOT = Path(__file__).resolve().parents[1]

_spec = importlib.util.spec_from_file_location(
    "catalogue_usage_report",
    ROOT / "catalogue-usage-report.py",
)
_mod = importlib.util.module_from_spec(_spec)
assert _spec.loader is not None
_spec.loader.exec_module(_mod)
load_events = _mod.load_events
report = _mod.report


def event(event_id: str, action: str, status: int = 200, session: str | None = "s1", result=None):
    return {
        "eventId": event_id,
        "action": action,
        "status": status,
        "sessionId": session,
        "resultCount": result,
    }


class CatalogueUsageReportTest(unittest.TestCase):
    def test_deduplicates_event_ids_and_separates_actions(self) -> None:
        lines = [
            json.dumps(event("e1", "search", result=3)),
            json.dumps(event("e1", "search", result=3)),
            json.dumps(event("e2", "search", result=0)),
            json.dumps(event("e3", "reader")),
            json.dumps(event("e4", "lyrics")),
            json.dumps(event("e5", "search", status=500)),
            json.dumps(event("e6", "reader", session="s2")),
            "not-json",
        ]
        summary = report(load_events(lines))
        self.assertEqual(6, summary["attempts"])
        self.assertEqual(2, summary["completedSearches"])
        self.assertEqual(1, summary["zeroResultSearches"])
        self.assertEqual(2, summary["readerViews"])
        self.assertEqual(2, summary["approximateSessions"])
        self.assertTrue(summary["sessionsAreNotUniquePeople"])


if __name__ == "__main__":
    unittest.main()
