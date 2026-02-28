use crate::config::Config;
use crate::services::{
    cleanup_ports, ensure_database_running, ensure_process_alive, spawn_backend, spawn_frontend,
    start_extraction_service, stop_extraction_service, wait_for_backend_health,
};
use crate::utils::{print_info, print_step, print_success, print_warning, project_root};
use crate::{AppConfig, ConnectionString, DatabaseConfig, DatabaseManager};
use anyhow::{anyhow, Context, Result};
use clap::{Args, Subcommand, ValueEnum};
use reqwest::{Client, StatusCode};
use serde::Deserialize;
use serde_json::{json, Value};
use sqlx::postgres::PgConnectOptions;
use sqlx::{ConnectOptions, Row};
use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::str::FromStr;
use std::time::{Duration, Instant};
use tokio::time::sleep;

#[derive(Args)]
pub struct TestArgs {
    #[command(subcommand)]
    command: TestCommands,
}

#[derive(Subcommand)]
enum TestCommands {
    /// Run the Sangita Grantha steel thread smoke test
    SteelThread,
    /// Full backend E2E extraction integration test (DB + backend + extractor + queue checks)
    ExtractionE2e(ExtractionE2eArgs),
    /// TRACK-068 markdown ingestion harness (parser + parity + generated artifacts)
    Track068Harness(Track068HarnessArgs),
    /// Quick connectivity smoke test (health + search)
    Upload(UploadArgs),
}

#[derive(Args)]
pub struct UploadArgs {
    /// Override API base URL (default uses config/application.local.toml or env)
    #[arg(long)]
    base_url: Option<String>,
}

#[derive(Args)]
pub struct ExtractionE2eArgs {
    /// Preconfigured scenario profile
    #[arg(long, value_enum, default_value_t = ExtractionScenario::PdfSmoke)]
    scenario: ExtractionScenario,

    /// Source URL to submit for extraction
    #[arg(long)]
    source_url: Option<String>,

    /// Source format for extraction_queue (currently: PDF, HTML, DOCX, IMAGE)
    #[arg(long)]
    source_format: Option<String>,

    /// Page range to limit extraction scope (example: 17-18)
    #[arg(long)]
    page_range: Option<String>,

    /// Max time to wait for PENDING -> INGESTED transition
    #[arg(long, default_value_t = 420)]
    timeout_seconds: u64,

    /// Polling interval while waiting for extraction completion
    #[arg(long, default_value_t = 5)]
    poll_interval_seconds: u64,

    /// Minimum expected extraction result count from Python worker
    #[arg(long, default_value_t = 1)]
    min_result_count: i64,

    /// Skip migration + seed step (use current DB as-is)
    #[arg(long)]
    skip_migrations: bool,

    /// Assume extractor is already running; don't start Docker extraction service
    #[arg(long)]
    skip_extraction_start: bool,

    /// Keep backend/extractor running after test completion
    #[arg(long)]
    keep_services: bool,

    /// CSV path containing Dikshitar entries (used by Dikshitar scenarios)
    #[arg(long)]
    csv_path: Option<String>,

    /// English PDF path (used by dikshitar-a-series scenario)
    #[arg(long)]
    english_pdf_path: Option<String>,

    /// Sanskrit PDF path (used by dikshitar-a-series scenario)
    #[arg(long)]
    sanskrit_pdf_path: Option<String>,

    /// Optional title prefix filter for Dikshitar CSV rows (example: A)
    #[arg(long)]
    title_prefix: Option<String>,

    /// Optional cap on number of Dikshitar CSV rows to process
    #[arg(long)]
    max_rows: Option<usize>,

    /// Fail the scenario when (first10+ragam+talam) collision groups are detected
    #[arg(long)]
    fail_on_collision: bool,
}

#[derive(Args)]
pub struct Track068HarnessArgs {
    /// Expected krithi count in both Sanskrit and English datasets
    #[arg(long, default_value_t = 479)]
    expected_count: usize,

    /// Skip running parser/generator scripts and validate existing artifacts only
    #[arg(long)]
    skip_regenerate: bool,

    /// Enforce strict ingestion readiness gates (missing metadata/pallavi becomes failure)
    #[arg(long)]
    enforce_ingestion_gates: bool,

    /// Skip semantic EN<->SA alignment scoring over krithi_comparison_report.csv
    #[arg(long)]
    skip_semantic_alignment: bool,

    /// Minimum fuzzy score for Title EN<->SA alignment (0-100)
    #[arg(long, default_value_t = 70)]
    semantic_title_min_score: i64,

    /// Minimum fuzzy score for Raga EN<->SA alignment (0-100)
    #[arg(long, default_value_t = 80)]
    semantic_raga_min_score: i64,

    /// Minimum fuzzy score for Tala EN<->SA alignment (0-100)
    #[arg(long, default_value_t = 70)]
    semantic_tala_min_score: i64,

    /// Maximum allowed mismatch ratio for Title EN<->SA alignment
    #[arg(long, default_value_t = 0.15)]
    max_title_mismatch_ratio: f64,

    /// Maximum allowed mismatch ratio for Raga EN<->SA alignment
    #[arg(long, default_value_t = 0.10)]
    max_raga_mismatch_ratio: f64,

    /// Maximum allowed mismatch ratio for Tala EN<->SA alignment
    #[arg(long, default_value_t = 0.10)]
    max_tala_mismatch_ratio: f64,
}

#[derive(Copy, Clone, Debug, Eq, PartialEq, ValueEnum)]
enum ExtractionScenario {
    PdfSmoke,
    BlogspotHtml,
    AkhilaThreeSource,
    DikshitarASeries,
    DikshitarKeyCollision,
}

const DEFAULT_PDF_E2E_URL: &str = "https://guruguha.org/wp-content/uploads/2022/03/mdeng.pdf";
const BLOGSPOT_E2E_CSV_PATH: &str = "database/for_import/Dikshitar-Krithi-Test-20.csv";
const DIKSHITAR_FULL_CSV_PATH: &str = "database/for_import/Dikshitar-Krithi-For-Import.csv";
const AKHILA_HTML_URL: &str =
    "https://guru-guha.blogspot.com/2007/07/dikshitar-kriti-akhilandesvari-raksha.html";
const AKHILA_FIXTURE_DIR: &str = "tools/sangita-cli/fixtures/extraction";
const AKHILA_ENG_PDF: &str = "mdeng-akhila.pdf";
const AKHILA_SKT_PDF: &str = "mdskt-akhila.pdf";
const AKHILA_FIXTURE_PORT: u16 = 8765;
const DIKSHITAR_A_FIXTURE_PORT: u16 = 8766;

struct DbValidationReport {
    status: String,
    result_count: i64,
    evidence_count: i64,
    extraction_method: String,
    extractor_version: String,
    min_section_count: i64,
    min_variant_count: i64,
}

pub async fn run(args: TestArgs) -> Result<()> {
    match args.command {
        TestCommands::SteelThread => run_steel_thread().await,
        TestCommands::ExtractionE2e(extraction_args) => run_extraction_e2e(extraction_args).await,
        TestCommands::Track068Harness(track_068_args) => run_track_068_harness(track_068_args).await,
        TestCommands::Upload(upload_args) => run_smoke_test(upload_args).await,
    }
}

async fn run_track_068_harness(args: Track068HarnessArgs) -> Result<()> {
    let root = project_root()?;
    let for_import_dir = root.join("database/for_import");
    let parser_script = for_import_dir.join("verification_parser.py");
    let generator_script = for_import_dir.join("generate_clean_markdown_from_json.py");
    let skt_json_path = for_import_dir.join("skt_krithis.json");
    let eng_json_path = for_import_dir.join("eng_krithis.json");
    let final_skt_path = for_import_dir.join("final_mdskt.md");
    let final_eng_path = for_import_dir.join("final_mdeng.md");
    let comparison_csv_path = for_import_dir.join("krithi_comparison_report.csv");
    let report_path = for_import_dir.join("track_068_harness_report.json");
    let mut hard_failures: Vec<String> = Vec::new();

    print_step("TRACK-068 HARNESS: Preflight checks");
    for required in [
        for_import_dir.join("mdskt.md"),
        for_import_dir.join("mdeng.md"),
        parser_script.clone(),
        generator_script.clone(),
    ] {
        if !required.exists() {
            return Err(anyhow!("Required file not found: {}", required.display()));
        }
    }

    let python_check = Command::new("python3")
        .arg("--version")
        .current_dir(&root)
        .output()
        .context("Failed to execute python3 --version. Install Python 3 first.")?;
    if !python_check.status.success() {
        return Err(anyhow!(
            "python3 --version failed: {}",
            String::from_utf8_lossy(&python_check.stderr)
        ));
    }
    print_success(&format!(
        "Python runtime detected: {}",
        String::from_utf8_lossy(&python_check.stdout).trim()
    ));

    if args.skip_regenerate {
        print_warning("Skipping parser/generator execution (--skip-regenerate)");
    } else {
        print_step("TRACK-068 HARNESS: Running verification parser");
        run_python_script(&for_import_dir, &parser_script)?;
        print_success("verification_parser.py completed");

        print_step("TRACK-068 HARNESS: Regenerating final markdown + CSV");
        run_python_script(&for_import_dir, &generator_script)?;
        print_success("generate_clean_markdown_from_json.py completed");
    }

    print_step("TRACK-068 HARNESS: Validating JSON parity and section integrity");
    let skt_json = fs::read_to_string(&skt_json_path)
        .with_context(|| format!("Failed to read {}", skt_json_path.display()))?;
    let eng_json = fs::read_to_string(&eng_json_path)
        .with_context(|| format!("Failed to read {}", eng_json_path.display()))?;

    let skt_krithis = parse_track_068_json(&skt_json, "skt_krithis.json")?;
    let eng_krithis = parse_track_068_json(&eng_json, "eng_krithis.json")?;

    let skt_count = skt_krithis.len();
    let eng_count = eng_krithis.len();
    if skt_count != args.expected_count || eng_count != args.expected_count {
        hard_failures.push(format!(
            "Count parity gate failed: expected {} each, got skt={}, eng={}",
            args.expected_count,
            skt_count,
            eng_count
        ));
        print_warning(&hard_failures.last().cloned().unwrap_or_default());
    } else {
        print_success(&format!(
            "Count parity passed: skt={}, eng={} (expected={})",
            skt_count, eng_count, args.expected_count
        ));
    }

    let skt_ids: BTreeSet<usize> = skt_krithis.iter().map(|item| item.id).collect();
    let eng_ids: BTreeSet<usize> = eng_krithis.iter().map(|item| item.id).collect();
    let expected_ids: BTreeSet<usize> = (1..=args.expected_count).collect();
    if skt_ids != expected_ids {
        hard_failures.push(format!(
            "skt_krithis.json id set is not contiguous 1..{}",
            args.expected_count
        ));
    }
    if eng_ids != expected_ids {
        hard_failures.push(format!(
            "eng_krithis.json id set is not contiguous 1..{}",
            args.expected_count
        ));
    }
    if skt_ids != eng_ids {
        hard_failures.push("ID parity failed: Sanskrit and English ID sets differ".to_string());
    }
    if skt_ids == expected_ids && eng_ids == expected_ids && skt_ids == eng_ids {
        print_success("ID parity passed: both datasets contain contiguous IDs with full overlap");
    } else {
        print_warning("ID parity failed; see harness report hardFailures");
    }

    let section_report = compute_track_068_section_report(&skt_krithis, &eng_krithis);
    let metadata_report = compute_track_068_metadata_report(&skt_krithis, &eng_krithis);

    print_info(&format!(
        "Section coverage: sktMissingPallavi={}, engMissingPallavi={}, missingAnySection={} ids",
        section_report.skt_missing_pallavi_count,
        section_report.eng_missing_pallavi_count,
        section_report.missing_any_section_count
    ));
    print_info(&format!(
        "Metadata coverage: sktUnknownTala={}, engUnknownTala={}, rowsWithAnyUnknownMeta={}",
        metadata_report.skt_unknown_tala_count,
        metadata_report.eng_unknown_tala_count,
        metadata_report.any_unknown_metadata_count
    ));

    print_step("TRACK-068 HARNESS: Validating generated artifact structure");
    let final_skt = fs::read_to_string(&final_skt_path)
        .with_context(|| format!("Failed to read {}", final_skt_path.display()))?;
    let final_eng = fs::read_to_string(&final_eng_path)
        .with_context(|| format!("Failed to read {}", final_eng_path.display()))?;
    let final_skt_count = count_markdown_krithis(&final_skt);
    let final_eng_count = count_markdown_krithis(&final_eng);
    if final_skt_count != args.expected_count || final_eng_count != args.expected_count {
        hard_failures.push(format!(
            "Final markdown gate failed: expected {} entries each, got final_mdskt={}, final_mdeng={}",
            args.expected_count,
            final_skt_count,
            final_eng_count
        ));
    }

    let csv_content = fs::read_to_string(&comparison_csv_path)
        .with_context(|| format!("Failed to read {}", comparison_csv_path.display()))?;
    let csv_rows = csv_content.lines().count().saturating_sub(1); // minus header
    if csv_rows != args.expected_count {
        hard_failures.push(format!(
            "Comparison CSV gate failed: expected {} rows, found {}",
            args.expected_count,
            csv_rows
        ));
    }

    print_step("TRACK-068 HARNESS: Semantic EN<->SA alignment scoring");
    let semantic_alignment = run_track_068_semantic_alignment(
        &root,
        &comparison_csv_path,
        args.semantic_title_min_score,
        args.semantic_raga_min_score,
        args.semantic_tala_min_score,
    )?;

    print_info(&format!(
        "Semantic averages: title={:.2}, raga={:.2}, tala={:.2}",
        semantic_alignment.averages.title_avg,
        semantic_alignment.averages.raga_avg,
        semantic_alignment.averages.tala_avg
    ));
    print_info(&format!(
        "Semantic mismatch ratios: title={:.3}, raga={:.3}, tala={:.3}",
        semantic_alignment.mismatches.title_ratio,
        semantic_alignment.mismatches.raga_ratio,
        semantic_alignment.mismatches.tala_ratio
    ));

    if args.skip_semantic_alignment {
        print_warning("Skipping semantic alignment gate (--skip-semantic-alignment)");
    } else {
        let mut semantic_failures: Vec<String> = Vec::new();
        if semantic_alignment.mismatches.title_ratio > args.max_title_mismatch_ratio {
            semantic_failures.push(format!(
                "title mismatch ratio {:.3} > max {:.3}",
                semantic_alignment.mismatches.title_ratio, args.max_title_mismatch_ratio
            ));
        }
        if semantic_alignment.mismatches.raga_ratio > args.max_raga_mismatch_ratio {
            semantic_failures.push(format!(
                "raga mismatch ratio {:.3} > max {:.3}",
                semantic_alignment.mismatches.raga_ratio, args.max_raga_mismatch_ratio
            ));
        }
        if semantic_alignment.mismatches.tala_ratio > args.max_tala_mismatch_ratio {
            semantic_failures.push(format!(
                "tala mismatch ratio {:.3} > max {:.3}",
                semantic_alignment.mismatches.tala_ratio, args.max_tala_mismatch_ratio
            ));
        }
        if !semantic_failures.is_empty() {
            hard_failures.push(format!(
                "Semantic alignment gate failed: {}",
                semantic_failures.join("; ")
            ));
        }
    }

    if args.enforce_ingestion_gates {
        let mut strict_failures: Vec<String> = Vec::new();
        if section_report.skt_missing_pallavi_count > 0 || section_report.eng_missing_pallavi_count > 0 {
            strict_failures.push(format!(
                "Missing pallavi sections (skt={}, eng={})",
                section_report.skt_missing_pallavi_count, section_report.eng_missing_pallavi_count
            ));
        }
        if section_report.missing_any_section_count > 0 {
            strict_failures.push(format!(
                "Krithis without any extracted sections={}",
                section_report.missing_any_section_count
            ));
        }
        if metadata_report.any_unknown_metadata_count > 0 {
            strict_failures.push(format!(
                "Rows with unknown metadata={}",
                metadata_report.any_unknown_metadata_count
            ));
        }
        if !strict_failures.is_empty() {
            hard_failures.push(format!(
                "Strict ingestion gates failed: {}",
                strict_failures.join("; ")
            ));
        }
        print_success("Strict ingestion gates passed");
    } else {
        if section_report.skt_missing_pallavi_count > 0 || section_report.eng_missing_pallavi_count > 0 {
            print_warning("Strict pallavi gate is not enforced (use --enforce-ingestion-gates to fail on this)");
        }
        if metadata_report.any_unknown_metadata_count > 0 {
            print_warning("Unknown metadata exists but strict gate is disabled (use --enforce-ingestion-gates)");
        }
    }

    let report = json!({
        "track": "TRACK-068",
        "expectedCount": args.expected_count,
        "skipRegenerate": args.skip_regenerate,
        "strictIngestionGates": args.enforce_ingestion_gates,
        "skipSemanticAlignment": args.skip_semantic_alignment,
        "hardFailures": hard_failures,
        "counts": {
            "sanskritJson": skt_count,
            "englishJson": eng_count,
            "finalSanskritMarkdown": final_skt_count,
            "finalEnglishMarkdown": final_eng_count,
            "comparisonCsvRows": csv_rows
        },
        "sectionIntegrity": {
            "sktMissingPallaviCount": section_report.skt_missing_pallavi_count,
            "engMissingPallaviCount": section_report.eng_missing_pallavi_count,
            "missingAnySectionCount": section_report.missing_any_section_count,
            "sampleMissingPallaviIds": section_report.sample_missing_pallavi_ids,
            "sampleMissingAnySectionIds": section_report.sample_missing_any_section_ids
        },
        "metadataIntegrity": {
            "sktUnknownTalaCount": metadata_report.skt_unknown_tala_count,
            "engUnknownTalaCount": metadata_report.eng_unknown_tala_count,
            "anyUnknownMetadataCount": metadata_report.any_unknown_metadata_count,
            "sampleUnknownMetadataIds": metadata_report.sample_unknown_metadata_ids
        },
        "semanticAlignment": semantic_alignment
    });
    fs::write(&report_path, serde_json::to_string_pretty(&report)?)
        .with_context(|| format!("Failed writing {}", report_path.display()))?;

    print_success(&format!(
        "TRACK-068 harness completed. Report written to {}",
        report_path.display()
    ));
    let hard_failures = report
        .get("hardFailures")
        .and_then(Value::as_array)
        .cloned()
        .unwrap_or_default();
    if !hard_failures.is_empty() {
        let summary = hard_failures
            .iter()
            .filter_map(Value::as_str)
            .collect::<Vec<_>>()
            .join(" | ");
        return Err(anyhow!("TRACK-068 harness failed: {}", summary));
    }
    Ok(())
}

fn run_python_script(cwd: &Path, script_path: &Path) -> Result<()> {
    let output = Command::new("python3")
        .arg(script_path)
        .current_dir(cwd)
        .output()
        .with_context(|| format!("Failed running python3 {}", script_path.display()))?;

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        let stdout = String::from_utf8_lossy(&output.stdout);
        return Err(anyhow!(
            "Python script failed: {}\nstdout:\n{}\nstderr:\n{}",
            script_path.display(),
            stdout,
            stderr
        ));
    }

    let stdout = String::from_utf8_lossy(&output.stdout);
    if !stdout.trim().is_empty() {
        let trimmed = stdout.trim();
        let preview = if trimmed.chars().count() > 1200 {
            format!("{}...", trimmed.chars().take(1200).collect::<String>())
        } else {
            trimmed.to_string()
        };
        print_info(&format!(
            "{} output: {}",
            script_path
                .file_name()
                .and_then(|s| s.to_str())
                .unwrap_or("script"),
            preview
        ));
    }
    Ok(())
}

#[derive(Debug, Deserialize, serde::Serialize)]
struct Track068SemanticThresholds {
    title_min_score: i64,
    raga_min_score: i64,
    tala_min_score: i64,
}

#[derive(Debug, Deserialize, serde::Serialize)]
struct Track068SemanticAverages {
    title_avg: f64,
    raga_avg: f64,
    tala_avg: f64,
}

#[derive(Debug, Deserialize, serde::Serialize)]
struct Track068SemanticMismatches {
    title_count: usize,
    raga_count: usize,
    tala_count: usize,
    title_ratio: f64,
    raga_ratio: f64,
    tala_ratio: f64,
}

#[derive(Debug, Deserialize, serde::Serialize)]
struct Track068SemanticSample {
    index: usize,
    score: f64,
    english: String,
    sanskrit: String,
}

#[derive(Debug, Deserialize, serde::Serialize)]
struct Track068SemanticSamples {
    title: Vec<Track068SemanticSample>,
    raga: Vec<Track068SemanticSample>,
    tala: Vec<Track068SemanticSample>,
}

#[derive(Debug, Deserialize, serde::Serialize)]
struct Track068SemanticUnknownCounts {
    title_en_unknown: usize,
    title_sa_unknown: usize,
    raga_en_unknown: usize,
    raga_sa_unknown: usize,
    tala_en_unknown: usize,
    tala_sa_unknown: usize,
}

#[derive(Debug, Deserialize, serde::Serialize)]
struct Track068SemanticAlignment {
    rows: usize,
    thresholds: Track068SemanticThresholds,
    averages: Track068SemanticAverages,
    mismatches: Track068SemanticMismatches,
    unknown_counts: Track068SemanticUnknownCounts,
    samples: Track068SemanticSamples,
}

const TRACK_068_SEMANTIC_ALIGNMENT_PY: &str = r#"
import csv
import json
import re
import sys
import unicodedata
from indic_transliteration import sanscript
from indic_transliteration.sanscript import transliterate
from rapidfuzz import fuzz

if len(sys.argv) < 6:
    raise SystemExit('Usage: <csv_path> <title_min> <raga_min> <tala_min> <sample_limit>')

csv_path = sys.argv[1]
title_min = int(sys.argv[2])
raga_min = int(sys.argv[3])
tala_min = int(sys.argv[4])
sample_limit = max(1, int(sys.argv[5]))

def has_devanagari(text: str) -> bool:
    return any('\u0900' <= ch <= '\u097F' for ch in text)

def dev_to_iast(text: str) -> str:
    text = (text or '').strip()
    if not text:
        return ''
    if has_devanagari(text):
        try:
            return transliterate(text, sanscript.DEVANAGARI, sanscript.IAST)
        except Exception:
            return text
    return text

def normalize(text: str) -> str:
    text = dev_to_iast((text or '').strip().lower())
    text = unicodedata.normalize('NFKD', text)
    text = ''.join(ch for ch in text if not unicodedata.combining(ch))
    text = re.sub(r'\([^)]*\)', ' ', text)
    text = re.sub(r'[^a-z0-9\s]', ' ', text)
    text = re.sub(r'\b(sync|synced|unknown|null|none)\b', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text

def is_unknown(value: str) -> bool:
    norm = normalize(value)
    return norm in ('', 'unknown', 'null', 'none')

def score(a: str, b: str) -> float:
    na = normalize(a)
    nb = normalize(b)
    if not na or not nb:
        return 0.0
    return float(fuzz.token_sort_ratio(na, nb))

rows = list(csv.DictReader(open(csv_path, encoding='utf-8')))
n = len(rows)

title_scores = []
raga_scores = []
tala_scores = []
title_mismatch = 0
raga_mismatch = 0
tala_mismatch = 0

title_samples = []
raga_samples = []
tala_samples = []

unknown_counts = {
    'title_en_unknown': 0,
    'title_sa_unknown': 0,
    'raga_en_unknown': 0,
    'raga_sa_unknown': 0,
    'tala_en_unknown': 0,
    'tala_sa_unknown': 0,
}

for row in rows:
    idx = int(row.get('Index') or 0)
    title_en = row.get('Title-EN', '')
    title_sa = row.get('Title-SA', '')
    raga_en = row.get('Raga-EN', '')
    raga_sa = row.get('Raga-SA', '')
    tala_en = row.get('Tala-EN', '')
    tala_sa = row.get('Tala-SA', '')

    if is_unknown(title_en):
        unknown_counts['title_en_unknown'] += 1
    if is_unknown(title_sa):
        unknown_counts['title_sa_unknown'] += 1
    if is_unknown(raga_en):
        unknown_counts['raga_en_unknown'] += 1
    if is_unknown(raga_sa):
        unknown_counts['raga_sa_unknown'] += 1
    if is_unknown(tala_en):
        unknown_counts['tala_en_unknown'] += 1
    if is_unknown(tala_sa):
        unknown_counts['tala_sa_unknown'] += 1

    title_score = score(title_en, title_sa)
    raga_score = score(raga_en, raga_sa)
    tala_score = score(tala_en, tala_sa)

    title_scores.append(title_score)
    raga_scores.append(raga_score)
    tala_scores.append(tala_score)

    if title_score < title_min:
        title_mismatch += 1
        if len(title_samples) < sample_limit:
            title_samples.append({
                'index': idx,
                'score': round(title_score, 2),
                'english': title_en,
                'sanskrit': title_sa,
            })
    if raga_score < raga_min:
        raga_mismatch += 1
        if len(raga_samples) < sample_limit:
            raga_samples.append({
                'index': idx,
                'score': round(raga_score, 2),
                'english': raga_en,
                'sanskrit': raga_sa,
            })
    if tala_score < tala_min:
        tala_mismatch += 1
        if len(tala_samples) < sample_limit:
            tala_samples.append({
                'index': idx,
                'score': round(tala_score, 2),
                'english': tala_en,
                'sanskrit': tala_sa,
            })

def avg(values):
    if not values:
        return 0.0
    return float(sum(values) / len(values))

result = {
    'rows': n,
    'thresholds': {
        'title_min_score': title_min,
        'raga_min_score': raga_min,
        'tala_min_score': tala_min,
    },
    'averages': {
        'title_avg': round(avg(title_scores), 4),
        'raga_avg': round(avg(raga_scores), 4),
        'tala_avg': round(avg(tala_scores), 4),
    },
    'mismatches': {
        'title_count': title_mismatch,
        'raga_count': raga_mismatch,
        'tala_count': tala_mismatch,
        'title_ratio': round((title_mismatch / n) if n else 0.0, 6),
        'raga_ratio': round((raga_mismatch / n) if n else 0.0, 6),
        'tala_ratio': round((tala_mismatch / n) if n else 0.0, 6),
    },
    'unknown_counts': unknown_counts,
    'samples': {
        'title': title_samples,
        'raga': raga_samples,
        'tala': tala_samples,
    },
}

print(json.dumps(result, ensure_ascii=False))
"#;

fn run_track_068_semantic_alignment(
    root: &Path,
    comparison_csv_path: &Path,
    title_min_score: i64,
    raga_min_score: i64,
    tala_min_score: i64,
) -> Result<Track068SemanticAlignment> {
    let worker_project = root.join("tools/krithi-extract-enrich-worker");
    if !worker_project.exists() {
        return Err(anyhow!(
            "Missing worker project required for semantic comparison: {}",
            worker_project.display()
        ));
    }

    let output = Command::new("uv")
        .arg("run")
        .arg("--project")
        .arg(&worker_project)
        .arg("python")
        .arg("-c")
        .arg(TRACK_068_SEMANTIC_ALIGNMENT_PY)
        .arg(comparison_csv_path)
        .arg(title_min_score.to_string())
        .arg(raga_min_score.to_string())
        .arg(tala_min_score.to_string())
        .arg("12")
        .current_dir(root)
        .env("TMPDIR", "/tmp")
        .output()
        .context("Failed executing semantic alignment scoring via uv")?;

    if !output.status.success() {
        return Err(anyhow!(
            "Semantic alignment scoring failed.\nstdout:\n{}\nstderr:\n{}",
            String::from_utf8_lossy(&output.stdout),
            String::from_utf8_lossy(&output.stderr)
        ));
    }

    let stdout = String::from_utf8(output.stdout)
        .context("Semantic alignment output was not valid UTF-8")?;
    let parsed: Track068SemanticAlignment = serde_json::from_str(stdout.trim()).with_context(|| {
        format!(
            "Failed parsing semantic alignment JSON output. Raw output: {}",
            stdout.trim()
        )
    })?;
    Ok(parsed)
}

#[derive(Clone)]
struct Track068Krithi {
    id: usize,
    raga: String,
    tala: String,
    sections: BTreeMap<String, String>,
}

fn parse_track_068_json(contents: &str, label: &str) -> Result<Vec<Track068Krithi>> {
    let raw: Vec<Value> = serde_json::from_str(contents)
        .with_context(|| format!("Failed parsing {}", label))?;

    raw.into_iter()
        .enumerate()
        .map(|(idx, item)| {
            let id = item
                .get("id")
                .and_then(Value::as_u64)
                .ok_or_else(|| anyhow!("{} row {} missing numeric id", label, idx + 1))?
                as usize;
            let raga = item
                .get("raga")
                .and_then(Value::as_str)
                .unwrap_or_default()
                .trim()
                .to_string();
            let tala = item
                .get("tala")
                .and_then(Value::as_str)
                .unwrap_or_default()
                .trim()
                .to_string();
            let sections = item
                .get("sections")
                .and_then(Value::as_object)
                .map(|map| {
                    map.iter()
                        .map(|(k, v)| (k.to_string(), v.as_str().unwrap_or_default().trim().to_string()))
                        .collect::<BTreeMap<_, _>>()
                })
                .unwrap_or_default();

            Ok(Track068Krithi { id, raga, tala, sections })
        })
        .collect()
}

struct Track068SectionReport {
    skt_missing_pallavi_count: usize,
    eng_missing_pallavi_count: usize,
    missing_any_section_count: usize,
    sample_missing_pallavi_ids: Vec<usize>,
    sample_missing_any_section_ids: Vec<usize>,
}

fn compute_track_068_section_report(
    skt_krithis: &[Track068Krithi],
    eng_krithis: &[Track068Krithi],
) -> Track068SectionReport {
    let mut skt_missing_pallavi = 0usize;
    let mut eng_missing_pallavi = 0usize;
    let mut missing_any_section = 0usize;
    let mut sample_missing_pallavi_ids = Vec::new();
    let mut sample_missing_any_section_ids = Vec::new();

    for (skt, eng) in skt_krithis.iter().zip(eng_krithis.iter()) {
        let skt_has_pallavi = has_non_empty_section(&skt.sections, "pallavi");
        let eng_has_pallavi = has_non_empty_section(&eng.sections, "pallavi");
        if !skt_has_pallavi {
            skt_missing_pallavi += 1;
            if sample_missing_pallavi_ids.len() < 12 {
                sample_missing_pallavi_ids.push(skt.id);
            }
        }
        if !eng_has_pallavi {
            eng_missing_pallavi += 1;
            if sample_missing_pallavi_ids.len() < 12 {
                sample_missing_pallavi_ids.push(eng.id);
            }
        }

        let skt_has_any = skt.sections.values().any(|text| !text.trim().is_empty());
        let eng_has_any = eng.sections.values().any(|text| !text.trim().is_empty());
        if !skt_has_any || !eng_has_any {
            missing_any_section += 1;
            if sample_missing_any_section_ids.len() < 12 {
                sample_missing_any_section_ids.push(skt.id);
            }
        }
    }

    sample_missing_pallavi_ids.sort_unstable();
    sample_missing_pallavi_ids.dedup();
    sample_missing_any_section_ids.sort_unstable();
    sample_missing_any_section_ids.dedup();

    Track068SectionReport {
        skt_missing_pallavi_count: skt_missing_pallavi,
        eng_missing_pallavi_count: eng_missing_pallavi,
        missing_any_section_count: missing_any_section,
        sample_missing_pallavi_ids,
        sample_missing_any_section_ids,
    }
}

struct Track068MetadataReport {
    skt_unknown_tala_count: usize,
    eng_unknown_tala_count: usize,
    any_unknown_metadata_count: usize,
    sample_unknown_metadata_ids: Vec<usize>,
}

fn compute_track_068_metadata_report(
    skt_krithis: &[Track068Krithi],
    eng_krithis: &[Track068Krithi],
) -> Track068MetadataReport {
    let mut skt_unknown_tala = 0usize;
    let mut eng_unknown_tala = 0usize;
    let mut any_unknown_meta = 0usize;
    let mut sample_unknown_ids = Vec::new();

    for (skt, eng) in skt_krithis.iter().zip(eng_krithis.iter()) {
        let skt_unknown = is_unknown_field(&skt.raga) || is_unknown_field(&skt.tala);
        let eng_unknown = is_unknown_field(&eng.raga) || is_unknown_field(&eng.tala);

        if is_unknown_field(&skt.tala) {
            skt_unknown_tala += 1;
        }
        if is_unknown_field(&eng.tala) {
            eng_unknown_tala += 1;
        }
        if skt_unknown || eng_unknown {
            any_unknown_meta += 1;
            if sample_unknown_ids.len() < 12 {
                sample_unknown_ids.push(skt.id);
            }
        }
    }

    Track068MetadataReport {
        skt_unknown_tala_count: skt_unknown_tala,
        eng_unknown_tala_count: eng_unknown_tala,
        any_unknown_metadata_count: any_unknown_meta,
        sample_unknown_metadata_ids: sample_unknown_ids,
    }
}

fn has_non_empty_section(sections: &BTreeMap<String, String>, key: &str) -> bool {
    sections
        .get(key)
        .map(|text| !text.trim().is_empty())
        .unwrap_or(false)
}

fn is_unknown_field(value: &str) -> bool {
    let normalized = value.trim().to_lowercase();
    normalized.is_empty() || normalized == "unknown" || normalized == "null" || normalized == "none"
}

fn count_markdown_krithis(markdown: &str) -> usize {
    markdown
        .lines()
        .filter(|line| line.starts_with("# "))
        .count()
}

async fn run_steel_thread() -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;
    let client = build_http_client(45)?;
    let api_host = resolve_connectable_host(&config.api_host);

    print_step("PHASE 1: Database & Migrations");
    let app_config = AppConfig::from_file(&root.join("config/application.local.toml"))?;
    ensure_database_running(&app_config).await?;
    apply_migrations(&app_config, &root).await?;

    print_step("PHASE 2: Backend Verification");
    let mut backend = spawn_backend(&config, &root, Some("test"))?;

    let health_url = format!("http://{}:{}/health", api_host, config.api_port);
    wait_for_backend_health(&client, &health_url, &mut backend, 180).await?;

    print_step("Running API Smoke Checks...");
    let search_url = format!("http://{}:{}/v1/krithis/search", api_host, config.api_port);
    let search_resp = client.get(&search_url).send().await?;
    if !search_resp.status().is_success() {
        let _ = backend.kill();
        return Err(anyhow!("Search endpoint failed: {}", search_resp.status()));
    }
    print_success("Krithi search endpoint reachable");

    let audit_url = format!("http://{}:{}/v1/audit/logs", api_host, config.api_port);
    let audit_resp = client
        .get(&audit_url)
        .bearer_auth(&config.admin_token)
        .send()
        .await?;
    if audit_resp.status().is_success() {
        print_success("Admin audit logs reachable with admin token");
    } else {
        eprintln!(
            "Warning: audit logs returned {} (check ADMIN_TOKEN)",
            audit_resp.status()
        );
    }

    ensure_process_alive(&mut backend, "Backend")?;

    print_step("PHASE 3: Frontend Launch");
    let mut frontend = spawn_frontend(&config, &root, true)?;
    sleep(Duration::from_secs(3)).await;
    print_success("Frontend started");

    print_step("PHASE 4: Manual Verification");
    println!("Admin Web: http://localhost:{}", config.frontend_port);
    println!("API: http://{}:{}", api_host, config.api_port);
    println!("Admin Token: {}", config.admin_token);
    println!("\nPress Ctrl+C to stop servers and exit.");

    tokio::signal::ctrl_c().await?;

    print_step("Shutting down...");
    let _ = backend.kill();
    let _ = frontend.kill();

    Ok(())
}

async fn apply_migrations(app_config: &AppConfig, root: &PathBuf) -> Result<()> {
    let migrations_path = root.join("database/migrations");
    let seed_data_path = root.join("database/seed_data");

    print_step("Applying migrations...");

    let connection_string = ConnectionString::new(app_config);
    let db_config = DatabaseConfig::new(connection_string.to_string())
        .with_admin_db(app_config.database.admin_db.clone())
        .with_admin_user(app_config.database.admin_user.clone())
        .with_admin_password(app_config.database.admin_password.clone());

    let seed_path = if seed_data_path.exists() {
        Some(seed_data_path)
    } else {
        None
    };

    let manager = DatabaseManager::new(db_config, migrations_path, seed_path);

    manager.ensure_database_exists().await?;
    manager.setup_connection_pool().await?;
    manager.run_migrations().await?;
    manager.run_seed_data().await?;

    print_success("Migrations complete");
    Ok(())
}

async fn run_extraction_e2e(args: ExtractionE2eArgs) -> Result<()> {
    if args.scenario == ExtractionScenario::AkhilaThreeSource {
        return run_akhila_three_source_e2e(args).await;
    }
    if args.scenario == ExtractionScenario::DikshitarASeries {
        return run_dikshitar_a_series_convergence(args).await;
    }
    if args.scenario == ExtractionScenario::DikshitarKeyCollision {
        return run_dikshitar_key_collision_scan(args).await;
    }

    let config = Config::load()?;
    let root = project_root()?;
    let app_config = AppConfig::from_file(&root.join("config/application.local.toml"))?;
    let api_host = resolve_connectable_host(&config.api_host);
    let api_base = format!("http://{}:{}", api_host, config.api_port);
    let client = build_http_client(45)?;
    let (source_url, source_format, page_range) = resolve_extraction_inputs(&args, &root)?;
    print_info(&format!(
        "Scenario {:?}: source_format={}, source_url={}, page_range={}",
        args.scenario,
        source_format,
        source_url,
        if page_range.is_empty() {
            "<none>"
        } else {
            page_range.as_str()
        }
    ));

    let mut backend: Option<Child> = None;
    let mut started_extraction_for_test = false;
    let extraction_was_running_before = is_extraction_service_running(&root);

    let test_result: Result<()> = async {
        print_step("PHASE 1: Database readiness");
        ensure_database_running(&app_config).await?;
        if args.skip_migrations {
            print_warning("Skipping migrations and seed data (--skip-migrations)");
        } else {
            apply_migrations(&app_config, &root).await?;
        }

        print_step("PHASE 2: Backend startup");
        cleanup_ports(&config)?;
        let mut backend_process = spawn_backend(&config, &root, Some("test"))?;
        let health_url = format!("{api_base}/health");
        wait_for_backend_health(&client, &health_url, &mut backend_process, 240).await?;
        ensure_process_alive(&mut backend_process, "Backend")?;
        backend = Some(backend_process);

        print_step("PHASE 3: Python extraction service");
        if args.skip_extraction_start {
            print_warning("Skipping extraction service start (--skip-extraction-start)");
        } else if extraction_was_running_before {
            print_info("Extraction service already running; reusing existing container");
        } else {
            start_extraction_service(&root)?;
            started_extraction_for_test = true;
            print_success("Extraction service started");
            sleep(Duration::from_secs(3)).await;
        }

        print_step("PHASE 4: Authentication");
        let jwt_auth = issue_admin_jwt(&client, &api_base, &config.admin_token).await?;
        print_success("Admin JWT issued");

        print_step("PHASE 5: Submit extraction task");
        let task_id = submit_extraction_task(
            &client,
            &api_base,
            &token,
            &source_url,
            &source_format,
            &page_range,
        )
        .await?;
        print_success(&format!("Extraction submitted (task={task_id})"));

        print_step("PHASE 6: Wait for INGESTED");
        wait_for_ingested(
            &client,
            &api_base,
            &token,
            &task_id,
            args.timeout_seconds,
            args.poll_interval_seconds,
        )
        .await?;
        print_success("Extraction reached INGESTED");

        print_step("PHASE 7: Database/API validation");
        let report =
            validate_extraction_outcome(&app_config, &task_id, &source_url, args.min_result_count)
                .await?;
        print_success(&format!(
            "Validation passed: status={}, results={}, evidence={}, method={}, extractor={}, minSections={}, minVariants={}",
            report.status,
            report.result_count,
            report.evidence_count,
            report.extraction_method,
            report.extractor_version,
            report.min_section_count,
            report.min_variant_count,
        ));

        Ok(())
    }
    .await;

    if args.keep_services {
        print_info("Leaving backend/extractor running (--keep-services)");
    } else {
        print_step("Cleanup");
        if started_extraction_for_test {
            if let Err(e) = stop_extraction_service(&root) {
                print_warning(&format!("Failed to stop extraction service: {e}"));
            } else {
                print_success("Extraction service stopped");
            }
        }
        stop_backend_process(&mut backend);
        if let Err(e) = cleanup_ports(&config) {
            print_warning(&format!("Port cleanup warning: {e}"));
        }
    }

    test_result
}

struct TaskValidationResult {
    task_id: String,
    report: DbValidationReport,
}

struct AkhilaAlignmentReport {
    krithi_id: String,
    scripts: Vec<String>,
}

struct SourceTaskBinding {
    source_url: String,
    task_id: String,
}

struct DikshitarCsvRow {
    index: usize,
    title: String,
    csv_raga: String,
    source_url: String,
}

struct DikshitarHtmlTask {
    row: DikshitarCsvRow,
    source_url: String,
    task_id: String,
}

#[derive(Clone)]
struct DikshitarIdentityKeyRow {
    row_index: usize,
    title: String,
    task_id: String,
    prefix10: String,
    raga: String,
    tala: String,
}

struct DikshitarCollisionGroup {
    key: String,
    rows: Vec<DikshitarIdentityKeyRow>,
}

struct DikshitarCollisionScanReport {
    keyed_rows: usize,
    metadata_missing_rows: usize,
    metadata_missing_examples: Vec<String>,
    groups: Vec<DikshitarCollisionGroup>,
}

async fn run_dikshitar_a_series_convergence(args: ExtractionE2eArgs) -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;
    let app_config = AppConfig::from_file(&root.join("config/application.local.toml"))?;
    let api_host = resolve_connectable_host(&config.api_host);
    let api_base = format!("http://{}:{}", api_host, config.api_port);
    let client = build_http_client(45)?;

    let csv_path = args
        .csv_path
        .as_ref()
        .map(PathBuf::from)
        .unwrap_or_else(|| root.join(BLOGSPOT_E2E_CSV_PATH));
    let english_pdf_path = args
        .english_pdf_path
        .as_ref()
        .map(PathBuf::from)
        .ok_or_else(|| anyhow!("--english-pdf-path is required for dikshitar-a-series scenario"))?;
    let sanskrit_pdf_path = args
        .sanskrit_pdf_path
        .as_ref()
        .map(PathBuf::from)
        .ok_or_else(|| {
            anyhow!("--sanskrit-pdf-path is required for dikshitar-a-series scenario")
        })?;

    if !csv_path.exists() {
        return Err(anyhow!("CSV path does not exist: {}", csv_path.display()));
    }
    if !english_pdf_path.exists() {
        return Err(anyhow!(
            "English PDF path does not exist: {}",
            english_pdf_path.display()
        ));
    }
    if !sanskrit_pdf_path.exists() {
        return Err(anyhow!(
            "Sanskrit PDF path does not exist: {}",
            sanskrit_pdf_path.display()
        ));
    }

    let english_file = english_pdf_path
        .file_name()
        .and_then(|s| s.to_str())
        .ok_or_else(|| anyhow!("Invalid english PDF file name"))?
        .to_string();
    let sanskrit_file = sanskrit_pdf_path
        .file_name()
        .and_then(|s| s.to_str())
        .ok_or_else(|| anyhow!("Invalid sanskrit PDF file name"))?
        .to_string();

    let english_dir = english_pdf_path
        .parent()
        .ok_or_else(|| anyhow!("English PDF has no parent directory"))?;
    let sanskrit_dir = sanskrit_pdf_path
        .parent()
        .ok_or_else(|| anyhow!("Sanskrit PDF has no parent directory"))?;
    if english_dir != sanskrit_dir {
        return Err(anyhow!(
            "English and Sanskrit PDFs must be in the same directory for local fixture serving"
        ));
    }

    let title_prefix = args.title_prefix.as_deref().or(Some("A"));
    let rows = load_dikshitar_rows(&csv_path, title_prefix, args.max_rows)?;
    if rows.is_empty() {
        return Err(anyhow!(
            "No Dikshitar CSV entries found for prefix {:?} in {}",
            title_prefix.unwrap_or("<none>"),
            csv_path.display()
        ));
    }

    print_info(&format!(
        "Scenario DikshitarASeries: {} CSV rows (prefix={:?}, maxRows={:?}), csv={}, englishPdf={}, sanskritPdf={}",
        rows.len(),
        title_prefix,
        args.max_rows,
        csv_path.display(),
        english_pdf_path.display(),
        sanskrit_pdf_path.display()
    ));

    let mut backend: Option<Child> = None;
    let mut fixture_server: Option<Child> = None;
    let mut started_extraction_for_test = false;
    let extraction_was_running_before = is_extraction_service_running(&root);

    let test_result: Result<()> = async {
        print_step("PHASE 1: Database readiness");
        ensure_database_running(&app_config).await?;
        if args.skip_migrations {
            print_warning("Skipping migrations and seed data (--skip-migrations)");
        } else {
            apply_migrations(&app_config, &root).await?;
        }

        print_step("PHASE 2: Backend startup");
        cleanup_ports(&config)?;
        let mut backend_process = spawn_backend(&config, &root, Some("test"))?;
        let health_url = format!("{api_base}/health");
        wait_for_backend_health(&client, &health_url, &mut backend_process, 240).await?;
        ensure_process_alive(&mut backend_process, "Backend")?;
        backend = Some(backend_process);

        print_step("PHASE 3: Python extraction service");
        if args.skip_extraction_start {
            print_warning("Skipping extraction service start (--skip-extraction-start)");
        } else if extraction_was_running_before {
            print_info("Extraction service already running; reusing existing container");
        } else {
            start_extraction_service(&root)?;
            started_extraction_for_test = true;
            print_success("Extraction service started");
            sleep(Duration::from_secs(3)).await;
        }

        print_step("PHASE 4: Local fixture server");
        let mut server = start_fixture_http_server_from_dir(english_dir, DIKSHITAR_A_FIXTURE_PORT)?;
        wait_for_fixture_server(&client, DIKSHITAR_A_FIXTURE_PORT, &english_file, &mut server, 20)
            .await?;
        wait_for_fixture_server(
            &client,
            DIKSHITAR_A_FIXTURE_PORT,
            &sanskrit_file,
            &mut server,
            20,
        )
        .await?;
        fixture_server = Some(server);
        print_success("Dikshitar A-series fixture server started");

        let run_tag = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);
        let english_pdf_source_url = format!(
            "http://host.docker.internal:{}/{}?run={}",
            DIKSHITAR_A_FIXTURE_PORT, english_file, run_tag
        );
        let sanskrit_pdf_source_url = format!(
            "http://host.docker.internal:{}/{}?run={}",
            DIKSHITAR_A_FIXTURE_PORT, sanskrit_file, run_tag
        );

        print_step("PHASE 5: Authentication");
        let jwt_auth = issue_admin_jwt(&client, &api_base, &config.admin_token).await?;
        print_success("Admin JWT issued");

        print_step("PHASE 6: Submit PDF baselines");
        let english_pdf_task = run_and_validate_extraction_task(
            &client,
            &api_base,
            &token,
            &app_config,
            "Dikshitar A English PDF",
            &english_pdf_source_url,
            "PDF",
            "",
            args.timeout_seconds,
            args.poll_interval_seconds,
            1,
        )
        .await?;
        let sanskrit_pdf_task = run_and_validate_extraction_task(
            &client,
            &api_base,
            &token,
            &app_config,
            "Dikshitar A Sanskrit PDF",
            &sanskrit_pdf_source_url,
            "PDF",
            "",
            args.timeout_seconds,
            args.poll_interval_seconds,
            1,
        )
        .await?;

        print_step("PHASE 7: Submit HTML rows from CSV");
        let mut html_tasks: Vec<DikshitarHtmlTask> = Vec::new();
        let mut html_failures: Vec<String> = Vec::new();
        for row in rows {
            let row_source_url = append_query_param(
                &append_query_param(&row.source_url, "run", &run_tag.to_string()),
                "row",
                &row.index.to_string(),
            );
            let label = format!("A-row {:02}: {}", row.index, truncate_for_label(&row.title, 48));
            let task_result = run_and_validate_extraction_task(
                &client,
                &api_base,
                &token,
                &app_config,
                &label,
                &row_source_url,
                "HTML",
                "",
                args.timeout_seconds,
                args.poll_interval_seconds,
                1,
            )
            .await;
            match task_result {
                Ok(task) => {
                    html_tasks.push(DikshitarHtmlTask {
                        row,
                        source_url: row_source_url,
                        task_id: task.task_id,
                    });
                }
                Err(e) => {
                    let failure = format!("row {} '{}': {}", row.index, row.title, e);
                    print_warning(&format!("Skipping failed row: {}", failure));
                    html_failures.push(failure);
                }
            }
        }

        print_step("PHASE 8: Convergence analysis");
        let connection_string = ConnectionString::new(&app_config).to_string();
        let options =
            PgConnectOptions::from_str(&connection_string)?.log_statements(log::LevelFilter::Off);
        let pool = sqlx::PgPool::connect_with(options)
            .await
            .context("Failed to connect to PostgreSQL for Dikshitar A convergence checks")?;

        let english_ids = fetch_task_krithi_ids(
            &pool,
            &english_pdf_source_url,
            &english_pdf_task.task_id,
        )
        .await?;
        let sanskrit_ids = fetch_task_krithi_ids(
            &pool,
            &sanskrit_pdf_source_url,
            &sanskrit_pdf_task.task_id,
        )
        .await?;
        let collision_scan = scan_dikshitar_identity_key_collisions(&pool, &html_tasks).await?;
        if !collision_scan.groups.is_empty() {
            print_warning(&format!(
                "Detected {} first10+raga+tala collision group(s); flagging as outliers and continuing",
                collision_scan.groups.len()
            ));
            for group in collision_scan.groups.iter().take(6) {
                let members = group
                    .rows
                    .iter()
                    .map(|row| {
                        format!(
                            "#{} {} [raga={}, tala={}]",
                            row.row_index, row.title, row.raga, row.tala
                        )
                    })
                    .collect::<Vec<_>>()
                    .join(" | ");
                print_warning(&format!("Collision key {} => {}", group.key, members));
            }
        }
        if args.fail_on_collision && !collision_scan.groups.is_empty() {
            pool.close().await;
            return Err(anyhow!(
                "Collision groups detected ({}) and --fail-on-collision was set",
                collision_scan.groups.len()
            ));
        }

        let mut converged = 0usize;
        let mut partial = 0usize;
        let mut not_converged = 0usize;
        let mut outlier_rows = 0usize;
        let mut misses: Vec<String> = Vec::new();
        let outlier_task_ids: BTreeSet<String> = collision_scan
            .groups
            .iter()
            .flat_map(|group| group.rows.iter().map(|row| row.task_id.clone()))
            .collect();

        for html in &html_tasks {
            if outlier_task_ids.contains(&html.task_id) {
                outlier_rows += 1;
                misses.push(format!("{} (key-collision outlier)", html.row.title));
                continue;
            }
            let html_ids = fetch_task_krithi_ids(&pool, &html.source_url, &html.task_id).await?;
            let in_eng = html_ids.iter().any(|id| english_ids.contains(id));
            let in_skt = html_ids.iter().any(|id| sanskrit_ids.contains(id));
            if in_eng && in_skt {
                converged += 1;
            } else if in_eng || in_skt {
                partial += 1;
                misses.push(format!(
                    "{} (partial: eng={}, skt={})",
                    html.row.title, in_eng, in_skt
                ));
            } else {
                not_converged += 1;
                misses.push(format!("{} (no match)", html.row.title));
            }
        }
        not_converged += html_failures.len();
        for failure in &html_failures {
            misses.push(format!("FAILED: {}", failure));
        }

        pool.close().await;

        print_success(&format!(
            "Dikshitar A convergence: totalRows={}, successfulRows={}, failedRows={}, keyOutliers={}, converged={}, partial={}, notConverged={}, englishPdfKrithis={}, sanskritPdfKrithis={}, keyedRows={}, metadataMissingRows={}",
            html_tasks.len() + html_failures.len(),
            html_tasks.len(),
            html_failures.len(),
            outlier_rows,
            converged,
            partial,
            not_converged,
            english_ids.len(),
            sanskrit_ids.len(),
            collision_scan.keyed_rows,
            collision_scan.metadata_missing_rows
        ));
        if !misses.is_empty() {
            let preview = misses.into_iter().take(12).collect::<Vec<_>>().join(" | ");
            print_warning(&format!("Non-converged/partial examples: {}", preview));
        }

        Ok(())
    }
    .await;

    if let Some(mut server) = fixture_server.take() {
        let _ = server.kill();
        let _ = server.wait();
    }

    if args.keep_services {
        print_info("Leaving backend/extractor running (--keep-services)");
    } else {
        print_step("Cleanup");
        if started_extraction_for_test {
            if let Err(e) = stop_extraction_service(&root) {
                print_warning(&format!("Failed to stop extraction service: {e}"));
            } else {
                print_success("Extraction service stopped");
            }
        }
        stop_backend_process(&mut backend);
        if let Err(e) = cleanup_ports(&config) {
            print_warning(&format!("Port cleanup warning: {e}"));
        }
    }

    test_result
}

async fn run_dikshitar_key_collision_scan(args: ExtractionE2eArgs) -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;
    let app_config = AppConfig::from_file(&root.join("config/application.local.toml"))?;
    let api_host = resolve_connectable_host(&config.api_host);
    let api_base = format!("http://{}:{}", api_host, config.api_port);
    let client = build_http_client(45)?;

    let csv_path = args
        .csv_path
        .as_ref()
        .map(PathBuf::from)
        .unwrap_or_else(|| root.join(DIKSHITAR_FULL_CSV_PATH));
    let title_prefix = args.title_prefix.as_deref();
    let rows = load_dikshitar_rows(&csv_path, title_prefix, args.max_rows)?;
    if rows.is_empty() {
        return Err(anyhow!(
            "No Dikshitar CSV entries found for prefix {:?} in {}",
            title_prefix.unwrap_or("<none>"),
            csv_path.display()
        ));
    }

    print_info(&format!(
        "Scenario DikshitarKeyCollision: {} CSV rows (prefix={:?}, maxRows={:?}), csv={}",
        rows.len(),
        title_prefix,
        args.max_rows,
        csv_path.display()
    ));

    let mut backend: Option<Child> = None;
    let mut started_extraction_for_test = false;
    let extraction_was_running_before = is_extraction_service_running(&root);

    let test_result: Result<()> = async {
        print_step("PHASE 1: Database readiness");
        ensure_database_running(&app_config).await?;
        if args.skip_migrations {
            print_warning("Skipping migrations and seed data (--skip-migrations)");
        } else {
            apply_migrations(&app_config, &root).await?;
        }

        print_step("PHASE 2: Backend startup");
        cleanup_ports(&config)?;
        let mut backend_process = spawn_backend(&config, &root, Some("test"))?;
        let health_url = format!("{api_base}/health");
        wait_for_backend_health(&client, &health_url, &mut backend_process, 240).await?;
        ensure_process_alive(&mut backend_process, "Backend")?;
        backend = Some(backend_process);

        print_step("PHASE 3: Python extraction service");
        if args.skip_extraction_start {
            print_warning("Skipping extraction service start (--skip-extraction-start)");
        } else if extraction_was_running_before {
            print_info("Extraction service already running; reusing existing container");
        } else {
            start_extraction_service(&root)?;
            started_extraction_for_test = true;
            print_success("Extraction service started");
            sleep(Duration::from_secs(3)).await;
        }

        print_step("PHASE 4: Authentication");
        let jwt_auth = issue_admin_jwt(&client, &api_base, &config.admin_token).await?;
        print_success("Admin JWT issued");

        print_step("PHASE 5: Submit HTML rows from CSV");
        let run_tag = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);
        let mut html_tasks: Vec<DikshitarHtmlTask> = Vec::new();
        let mut html_failures: Vec<String> = Vec::new();
        for row in rows {
            let row_source_url = append_query_param(
                &append_query_param(&row.source_url, "run", &run_tag.to_string()),
                "row",
                &row.index.to_string(),
            );
            let label = format!(
                "Row {:03}: {}",
                row.index,
                truncate_for_label(&row.title, 48)
            );
            let task_result = run_and_validate_extraction_task(
                &client,
                &api_base,
                &token,
                &app_config,
                &label,
                &row_source_url,
                "HTML",
                "",
                args.timeout_seconds,
                args.poll_interval_seconds,
                args.min_result_count.max(1),
            )
            .await;
            match task_result {
                Ok(task) => {
                    html_tasks.push(DikshitarHtmlTask {
                        row,
                        source_url: row_source_url,
                        task_id: task.task_id,
                    });
                }
                Err(e) => {
                    let failure = format!(
                        "row {} '{}' (csvRaga='{}'): {}",
                        row.index, row.title, row.csv_raga, e
                    );
                    print_warning(&format!("Skipping failed row: {}", failure));
                    html_failures.push(failure);
                }
            }
        }

        print_step("PHASE 6: Identity-key collision analysis");
        let connection_string = ConnectionString::new(&app_config).to_string();
        let options =
            PgConnectOptions::from_str(&connection_string)?.log_statements(log::LevelFilter::Off);
        let pool = sqlx::PgPool::connect_with(options)
            .await
            .context("Failed to connect to PostgreSQL for Dikshitar collision scan")?;

        let collision_scan = scan_dikshitar_identity_key_collisions(&pool, &html_tasks).await?;
        let collision_rows = collision_scan
            .groups
            .iter()
            .map(|group| group.rows.len())
            .sum::<usize>();

        if !collision_scan.groups.is_empty() {
            print_warning(&format!(
                "Found {} collision group(s) for (first10+raga+tala); flagging and continuing",
                collision_scan.groups.len()
            ));
            for group in collision_scan.groups.iter().take(10) {
                let members = group
                    .rows
                    .iter()
                    .map(|row| {
                        format!(
                            "#{} {} [raga={}, tala={}]",
                            row.row_index, row.title, row.raga, row.tala
                        )
                    })
                    .collect::<Vec<_>>()
                    .join(" | ");
                print_warning(&format!("Collision key {} => {}", group.key, members));
            }
        }

        pool.close().await;

        print_success(&format!(
            "Dikshitar key-collision scan: totalRows={}, successfulRows={}, failedRows={}, keyedRows={}, metadataMissingRows={}, collisionGroups={}, collisionRows={}",
            html_tasks.len() + html_failures.len(),
            html_tasks.len(),
            html_failures.len(),
            collision_scan.keyed_rows,
            collision_scan.metadata_missing_rows,
            collision_scan.groups.len(),
            collision_rows
        ));
        if !html_failures.is_empty() {
            let preview = html_failures
                .iter()
                .take(8)
                .cloned()
                .collect::<Vec<_>>()
                .join(" | ");
            print_warning(&format!("Row failures (sample): {}", preview));
        }

        assert_collision_scan_metadata_coverage(&collision_scan)?;

        if args.fail_on_collision && !collision_scan.groups.is_empty() {
            return Err(anyhow!(
                "Collision groups detected ({}) and --fail-on-collision was set",
                collision_scan.groups.len()
            ));
        }

        Ok(())
    }
    .await;

    if args.keep_services {
        print_info("Leaving backend/extractor running (--keep-services)");
    } else {
        print_step("Cleanup");
        if started_extraction_for_test {
            if let Err(e) = stop_extraction_service(&root) {
                print_warning(&format!("Failed to stop extraction service: {e}"));
            } else {
                print_success("Extraction service stopped");
            }
        }
        stop_backend_process(&mut backend);
        if let Err(e) = cleanup_ports(&config) {
            print_warning(&format!("Port cleanup warning: {e}"));
        }
    }

    test_result
}

async fn run_akhila_three_source_e2e(args: ExtractionE2eArgs) -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;
    let app_config = AppConfig::from_file(&root.join("config/application.local.toml"))?;
    let api_host = resolve_connectable_host(&config.api_host);
    let api_base = format!("http://{}:{}", api_host, config.api_port);
    let client = build_http_client(45)?;

    print_info("Scenario AkhilaThreeSource: HTML + Roman PDF + Sanskrit PDF convergence check");
    if args.source_url.is_some() || args.source_format.is_some() || args.page_range.is_some() {
        print_warning(
            "Ignoring --source-url/--source-format/--page-range for AkhilaThreeSource scenario",
        );
    }

    let mut backend: Option<Child> = None;
    let mut fixture_server: Option<Child> = None;
    let mut started_extraction_for_test = false;
    let extraction_was_running_before = is_extraction_service_running(&root);

    let test_result: Result<()> = async {
        print_step("PHASE 1: Database readiness");
        ensure_database_running(&app_config).await?;
        if args.skip_migrations {
            print_warning("Skipping migrations and seed data (--skip-migrations)");
        } else {
            apply_migrations(&app_config, &root).await?;
        }

        print_step("PHASE 2: Backend startup");
        cleanup_ports(&config)?;
        let mut backend_process = spawn_backend(&config, &root, Some("test"))?;
        let health_url = format!("{api_base}/health");
        wait_for_backend_health(&client, &health_url, &mut backend_process, 240).await?;
        ensure_process_alive(&mut backend_process, "Backend")?;
        backend = Some(backend_process);

        print_step("PHASE 3: Python extraction service");
        if args.skip_extraction_start {
            print_warning("Skipping extraction service start (--skip-extraction-start)");
        } else if extraction_was_running_before {
            print_info("Extraction service already running; reusing existing container");
        } else {
            start_extraction_service(&root)?;
            started_extraction_for_test = true;
            print_success("Extraction service started");
            sleep(Duration::from_secs(3)).await;
        }

        print_step("PHASE 4: Local fixture server");
        let mut server = start_fixture_http_server(&root, AKHILA_FIXTURE_PORT)?;
        wait_for_fixture_server(&client, AKHILA_FIXTURE_PORT, AKHILA_ENG_PDF, &mut server, 20)
            .await?;
        wait_for_fixture_server(&client, AKHILA_FIXTURE_PORT, AKHILA_SKT_PDF, &mut server, 20)
            .await?;
        fixture_server = Some(server);
        print_success("Akhila fixture server started");

        let run_tag = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);
        let html_source_url = format!("{}?run={}", AKHILA_HTML_URL, run_tag);
        let english_pdf_url = format!(
            "http://host.docker.internal:{}/{}",
            AKHILA_FIXTURE_PORT, AKHILA_ENG_PDF
        );
        let sanskrit_pdf_url = format!(
            "http://host.docker.internal:{}/{}",
            AKHILA_FIXTURE_PORT, AKHILA_SKT_PDF
        );
        let english_pdf_source_url = format!("{}?run={}", english_pdf_url, run_tag);
        let sanskrit_pdf_source_url = format!("{}?run={}", sanskrit_pdf_url, run_tag);

        print_step("PHASE 5: Authentication");
        let jwt_auth = issue_admin_jwt(&client, &api_base, &config.admin_token).await?;
        print_success("Admin JWT issued");

        print_step("PHASE 6: Submit and ingest all 3 sources");
        let html = run_and_validate_extraction_task(
            &client,
            &api_base,
            &token,
            &app_config,
            "Akhila HTML",
            &html_source_url,
            "HTML",
            "",
            args.timeout_seconds,
            args.poll_interval_seconds,
            1,
        )
        .await?;
        let english = run_and_validate_extraction_task(
            &client,
            &api_base,
            &token,
            &app_config,
            "Akhila English PDF",
            &english_pdf_source_url,
            "PDF",
            "1-1",
            args.timeout_seconds,
            args.poll_interval_seconds,
            1,
        )
        .await?;
        let sanskrit = run_and_validate_extraction_task(
            &client,
            &api_base,
            &token,
            &app_config,
            "Akhila Sanskrit PDF",
            &sanskrit_pdf_source_url,
            "PDF",
            "1-1",
            args.timeout_seconds,
            args.poll_interval_seconds,
            1,
        )
        .await?;

        print_step("PHASE 7: Cross-source convergence checks");
        let alignment = validate_akhila_three_source_alignment(
            &app_config,
            vec![
                SourceTaskBinding {
                    source_url: html_source_url,
                    task_id: html.task_id.clone(),
                },
                SourceTaskBinding {
                    source_url: english_pdf_source_url,
                    task_id: english.task_id.clone(),
                },
                SourceTaskBinding {
                    source_url: sanskrit_pdf_source_url,
                    task_id: sanskrit.task_id.clone(),
                },
            ],
        )
        .await?;
        print_success(&format!(
            "3-source regression passed: htmlTask={}, englishTask={}, sanskritTask={}, krithi={}, scripts={}",
            html.task_id,
            english.task_id,
            sanskrit.task_id,
            alignment.krithi_id,
            alignment.scripts.join(","),
        ));
        let _ = (html.report, english.report, sanskrit.report);

        Ok(())
    }
    .await;

    if let Some(mut server) = fixture_server.take() {
        let _ = server.kill();
        let _ = server.wait();
    }

    if args.keep_services {
        print_info("Leaving backend/extractor running (--keep-services)");
    } else {
        print_step("Cleanup");
        if started_extraction_for_test {
            if let Err(e) = stop_extraction_service(&root) {
                print_warning(&format!("Failed to stop extraction service: {e}"));
            } else {
                print_success("Extraction service stopped");
            }
        }
        stop_backend_process(&mut backend);
        if let Err(e) = cleanup_ports(&config) {
            print_warning(&format!("Port cleanup warning: {e}"));
        }
    }

    test_result
}

fn start_fixture_http_server(root: &Path, port: u16) -> Result<Child> {
    let fixture_dir = root.join(AKHILA_FIXTURE_DIR);
    let english_path = fixture_dir.join(AKHILA_ENG_PDF);
    let sanskrit_path = fixture_dir.join(AKHILA_SKT_PDF);
    if !english_path.exists() {
        return Err(anyhow!("Missing fixture PDF: {}", english_path.display()));
    }
    if !sanskrit_path.exists() {
        return Err(anyhow!("Missing fixture PDF: {}", sanskrit_path.display()));
    }

    start_fixture_http_server_from_dir(&fixture_dir, port)
}

fn start_fixture_http_server_from_dir(directory: &Path, port: u16) -> Result<Child> {
    let child = Command::new("python3")
        .arg("-m")
        .arg("http.server")
        .arg(port.to_string())
        .arg("--bind")
        .arg("127.0.0.1")
        .arg("--directory")
        .arg(directory.as_os_str())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .context("Failed to start local fixture HTTP server")?;
    Ok(child)
}

fn load_dikshitar_rows(
    csv_path: &Path,
    title_prefix: Option<&str>,
    max_rows: Option<usize>,
) -> Result<Vec<DikshitarCsvRow>> {
    let contents = std::fs::read_to_string(csv_path)
        .with_context(|| format!("Failed to read CSV at {}", csv_path.display()))?;
    let normalized_prefix = title_prefix.map(|prefix| prefix.to_lowercase());
    let mut rows = Vec::new();
    for (line_no, line) in contents.lines().enumerate().skip(1) {
        let parts: Vec<&str> = line.split(',').map(str::trim).collect();
        if parts.len() < 3 {
            continue;
        }
        let title = parts[0].trim();
        let csv_raga = parts[1].trim();
        let source_url = parts.last().copied().unwrap_or_default().trim();
        if title.is_empty() || source_url.is_empty() {
            continue;
        }
        if let Some(prefix) = normalized_prefix.as_ref() {
            if !title.to_lowercase().starts_with(prefix) {
                continue;
            }
        }
        if !(source_url.starts_with("http://") || source_url.starts_with("https://")) {
            continue;
        }
        rows.push(DikshitarCsvRow {
            index: line_no,
            title: title.to_string(),
            csv_raga: csv_raga.to_string(),
            source_url: source_url.to_string(),
        });
        if let Some(limit) = max_rows {
            if rows.len() >= limit {
                break;
            }
        }
    }
    Ok(rows)
}

fn append_query_param(url: &str, key: &str, value: &str) -> String {
    let separator = if url.contains('?') { '&' } else { '?' };
    format!("{url}{separator}{key}={value}")
}

fn truncate_for_label(input: &str, max_chars: usize) -> String {
    if input.chars().count() <= max_chars {
        return input.to_string();
    }
    input.chars().take(max_chars).collect::<String>() + "..."
}

fn normalize_identity_component(input: &str) -> String {
    input
        .chars()
        .flat_map(|c| c.to_lowercase())
        .filter(|c| c.is_ascii_alphanumeric())
        .collect::<String>()
}

fn normalize_identity_prefix10(title: &str) -> String {
    normalize_identity_component(title)
        .chars()
        .take(10)
        .collect()
}

async fn fetch_task_raga_tala(
    pool: &sqlx::PgPool,
    source_url: &str,
    task_id: &str,
) -> Result<Option<(String, String)>> {
    let row = sqlx::query(
        r#"
        SELECT
            COALESCE(
                NULLIF(
                    TRIM(
                        COALESCE(
                            e.raw_extraction::jsonb->'ragas'->0->>'name',
                            e.raw_extraction::jsonb->>'raga'
                        )
                    ),
                    ''
                ),
                NULLIF(TRIM(rg.name), '')
            ) AS raga,
            COALESCE(
                NULLIF(
                    TRIM(
                        COALESCE(
                            e.raw_extraction::jsonb->>'tala',
                            e.raw_extraction::jsonb->'talas'->0->>'name'
                        )
                    ),
                    ''
                ),
                NULLIF(TRIM(tl.name), '')
            ) AS tala
        FROM extraction_queue q
        JOIN krithi_source_evidence e
          ON e.source_url = $1
         AND e.extracted_at >= q.created_at
        LEFT JOIN krithis k ON k.id = e.krithi_id
        LEFT JOIN ragas rg ON rg.id = k.primary_raga_id
        LEFT JOIN talas tl ON tl.id = k.tala_id
        WHERE q.id = $2::uuid
        ORDER BY
            CASE
                WHEN COALESCE(
                    NULLIF(
                        TRIM(
                            COALESCE(
                                e.raw_extraction::jsonb->'ragas'->0->>'name',
                                e.raw_extraction::jsonb->>'raga'
                            )
                        ),
                        ''
                    ),
                    NULLIF(TRIM(rg.name), '')
                ) IS NULL THEN 1 ELSE 0
            END,
            CASE
                WHEN COALESCE(
                    NULLIF(
                        TRIM(
                            COALESCE(
                                e.raw_extraction::jsonb->>'tala',
                                e.raw_extraction::jsonb->'talas'->0->>'name'
                            )
                        ),
                        ''
                    ),
                    NULLIF(TRIM(tl.name), '')
                ) IS NULL THEN 1 ELSE 0
            END,
            e.extracted_at DESC
        LIMIT 1
        "#,
    )
    .bind(source_url)
    .bind(task_id)
    .fetch_optional(pool)
    .await
    .with_context(|| {
        format!(
            "Failed fetching raga/tala metadata for source {} task {}",
            source_url, task_id
        )
    })?;

    let Some(row) = row else {
        return Ok(None);
    };
    let raga = row.get::<Option<String>, _>("raga").unwrap_or_default();
    let tala = row.get::<Option<String>, _>("tala").unwrap_or_default();
    let raga = raga.trim().to_string();
    let tala = tala.trim().to_string();
    if raga.is_empty() || tala.is_empty() {
        return Ok(None);
    }
    Ok(Some((raga, tala)))
}

async fn scan_dikshitar_identity_key_collisions(
    pool: &sqlx::PgPool,
    html_tasks: &[DikshitarHtmlTask],
) -> Result<DikshitarCollisionScanReport> {
    let mut keyed_rows: Vec<DikshitarIdentityKeyRow> = Vec::new();
    let mut metadata_missing_rows = 0usize;
    let mut metadata_missing_examples = Vec::new();

    for html in html_tasks {
        let Some((raga, tala)) =
            fetch_task_raga_tala(pool, &html.source_url, &html.task_id).await?
        else {
            metadata_missing_rows += 1;
            metadata_missing_examples.push(format!(
                "#{} {} (csvRaga={})",
                html.row.index, html.row.title, html.row.csv_raga
            ));
            continue;
        };
        let key_row = DikshitarIdentityKeyRow {
            row_index: html.row.index,
            title: html.row.title.clone(),
            task_id: html.task_id.clone(),
            prefix10: normalize_identity_prefix10(&html.row.title),
            raga,
            tala,
        };
        keyed_rows.push(key_row);
    }

    let mut groups_by_key: BTreeMap<String, Vec<DikshitarIdentityKeyRow>> = BTreeMap::new();
    for row in keyed_rows.iter().cloned() {
        let key = format!(
            "{}|{}|{}",
            row.prefix10,
            normalize_identity_component(&row.raga),
            normalize_identity_component(&row.tala)
        );
        groups_by_key.entry(key).or_default().push(row);
    }

    let groups = groups_by_key
        .into_iter()
        .filter_map(|(key, rows)| {
            if rows.len() > 1 {
                Some(DikshitarCollisionGroup { key, rows })
            } else {
                None
            }
        })
        .collect::<Vec<_>>();

    Ok(DikshitarCollisionScanReport {
        keyed_rows: keyed_rows.len(),
        metadata_missing_rows,
        metadata_missing_examples,
        groups,
    })
}

fn assert_collision_scan_metadata_coverage(report: &DikshitarCollisionScanReport) -> Result<()> {
    if report.metadata_missing_rows == 0 {
        return Ok(());
    }
    let sample = report
        .metadata_missing_examples
        .iter()
        .take(8)
        .cloned()
        .collect::<Vec<_>>()
        .join(" | ");
    Err(anyhow!(
        "Collision metadata regression: metadataMissingRows={} (expected 0). Sample rows: {}",
        report.metadata_missing_rows,
        if sample.is_empty() {
            "<none>".to_string()
        } else {
            sample
        }
    ))
}

async fn fetch_task_krithi_ids(
    pool: &sqlx::PgPool,
    source_url: &str,
    task_id: &str,
) -> Result<BTreeSet<String>> {
    let rows = sqlx::query(
        r#"
        SELECT DISTINCT e.krithi_id::text AS krithi_id
        FROM extraction_queue q
        JOIN krithi_source_evidence e
          ON e.source_url = $1
         AND e.extracted_at >= q.created_at
        WHERE q.id = $2::uuid
        "#,
    )
    .bind(source_url)
    .bind(task_id)
    .fetch_all(pool)
    .await
    .with_context(|| {
        format!(
            "Failed fetching krithi_ids for source {} task {}",
            source_url, task_id
        )
    })?;

    Ok(rows
        .into_iter()
        .map(|row| row.get::<String, _>("krithi_id"))
        .collect())
}

async fn wait_for_fixture_server(
    client: &Client,
    port: u16,
    filename: &str,
    child: &mut Child,
    timeout_seconds: u64,
) -> Result<()> {
    let url = format!("http://127.0.0.1:{port}/{filename}");
    let deadline = Instant::now() + Duration::from_secs(timeout_seconds);
    while Instant::now() < deadline {
        if let Some(status) = child
            .try_wait()
            .context("Failed checking fixture server state")?
        {
            return Err(anyhow!("Fixture server exited early with status {status}"));
        }
        if let Ok(response) = client.get(&url).send().await {
            if response.status().is_success() {
                return Ok(());
            }
        }
        sleep(Duration::from_millis(250)).await;
    }
    Err(anyhow!(
        "Timed out waiting for fixture server readiness at {}",
        url
    ))
}

#[allow(clippy::too_many_arguments)]
async fn run_and_validate_extraction_task(
    client: &Client,
    api_base: &str,
    token: &str,
    app_config: &AppConfig,
    label: &str,
    source_url: &str,
    source_format: &str,
    page_range: &str,
    timeout_seconds: u64,
    poll_interval_seconds: u64,
    min_result_count: i64,
) -> Result<TaskValidationResult> {
    print_step(&format!("{label}: submit task"));
    let task_id = submit_extraction_task(
        client,
        api_base,
        token,
        source_url,
        source_format,
        page_range,
    )
    .await?;
    print_success(&format!("{label}: task={task_id}"));

    wait_for_ingested(
        client,
        api_base,
        token,
        &task_id,
        timeout_seconds,
        poll_interval_seconds,
    )
    .await?;

    let report =
        validate_extraction_outcome(app_config, &task_id, source_url, min_result_count).await?;
    print_success(&format!(
        "{label}: status={}, results={}, evidence={}, method={}, minSections={}, minVariants={}",
        report.status,
        report.result_count,
        report.evidence_count,
        report.extraction_method,
        report.min_section_count,
        report.min_variant_count
    ));

    Ok(TaskValidationResult { task_id, report })
}

async fn validate_akhila_three_source_alignment(
    app_config: &AppConfig,
    source_tasks: Vec<SourceTaskBinding>,
) -> Result<AkhilaAlignmentReport> {
    if source_tasks.len() != 3 {
        return Err(anyhow!(
            "Expected exactly 3 source/task bindings, got {}",
            source_tasks.len()
        ));
    }

    let source_urls: Vec<String> = source_tasks
        .iter()
        .map(|it| it.source_url.clone())
        .collect();
    let task_ids: Vec<String> = source_tasks.iter().map(|it| it.task_id.clone()).collect();

    let connection_string = ConnectionString::new(app_config).to_string();
    let options =
        PgConnectOptions::from_str(&connection_string)?.log_statements(log::LevelFilter::Off);
    let pool = sqlx::PgPool::connect_with(options)
        .await
        .context("Failed to connect to PostgreSQL for 3-source alignment checks")?;

    let alignment_row = sqlx::query(
        r#"
        WITH source_tasks AS (
            SELECT * FROM unnest($1::text[], $2::text[]) AS t(source_url, task_id)
        ),
        run_evidence AS (
            SELECT e.source_url, e.krithi_id
            FROM source_tasks st
            JOIN extraction_queue q
              ON q.id::text = st.task_id
            JOIN krithi_source_evidence e
              ON e.source_url = st.source_url
             AND e.extracted_at >= q.created_at
            WHERE
              COALESCE(lower(e.raw_extraction::jsonb->>'title'), '') LIKE '%akhil%'
              OR COALESCE(lower(e.raw_extraction::jsonb->>'alternateTitle'), '') LIKE '%akhil%'
        )
        SELECT
            e.krithi_id::text AS krithi_id,
            COUNT(DISTINCT e.source_url)::bigint AS source_count
        FROM run_evidence e
        GROUP BY e.krithi_id
        HAVING COUNT(DISTINCT e.source_url) = 3
        ORDER BY COUNT(*) DESC
        LIMIT 1
        "#,
    )
    .bind(&source_urls)
    .bind(&task_ids)
    .fetch_optional(&pool)
    .await
    .context("Failed querying shared krithi across Akhila 3-source evidence")?;

    let alignment_row = if let Some(row) = alignment_row {
        row
    } else {
        let rows = sqlx::query(
            r#"
            WITH source_tasks AS (
                SELECT * FROM unnest($1::text[], $2::text[]) AS t(source_url, task_id)
            )
            SELECT e.source_url, e.krithi_id::text AS krithi_id
            FROM source_tasks st
            JOIN extraction_queue q
              ON q.id::text = st.task_id
            JOIN krithi_source_evidence e
              ON e.source_url = st.source_url
             AND e.extracted_at >= q.created_at
            WHERE
              COALESCE(lower(e.raw_extraction::jsonb->>'title'), '') LIKE '%akhil%'
              OR COALESCE(lower(e.raw_extraction::jsonb->>'alternateTitle'), '') LIKE '%akhil%'
            ORDER BY e.source_url
            "#,
        )
        .bind(&source_urls)
        .bind(&task_ids)
        .fetch_all(&pool)
        .await
        .context("Failed querying per-source evidence for debugging")?;

        let mut by_source: BTreeMap<String, Vec<String>> = BTreeMap::new();
        for row in rows {
            let source_url: String = row.get("source_url");
            let krithi_id: String = row.get("krithi_id");
            by_source.entry(source_url).or_default().push(krithi_id);
        }
        pool.close().await;
        return Err(anyhow!(
            "3-source convergence failed: no single krithi linked to all sources. Per-source krithi IDs: {:?}",
            by_source
        ));
    };

    let krithi_id: String = alignment_row.get("krithi_id");
    let source_count: i64 = alignment_row.get("source_count");
    if source_count != 3 {
        pool.close().await;
        return Err(anyhow!(
            "Expected shared krithi evidence count=3, got {} for krithi {}",
            source_count,
            krithi_id
        ));
    }

    let scripts_rows = sqlx::query(
        r#"
        SELECT DISTINCT script::text AS script
        FROM krithi_lyric_variants
        WHERE krithi_id = $1::uuid
        "#,
    )
    .bind(&krithi_id)
    .fetch_all(&pool)
    .await
    .context("Failed querying lyric variant scripts for shared krithi")?;

    let scripts: Vec<String> = scripts_rows
        .iter()
        .map(|row| row.get::<String, _>("script").to_lowercase())
        .collect();
    if !scripts.contains(&"latin".to_string()) || !scripts.contains(&"devanagari".to_string()) {
        pool.close().await;
        return Err(anyhow!(
            "Expected shared krithi {} to have both latin and devanagari lyric variants, found scripts={:?}",
            krithi_id,
            scripts
        ));
    }

    pool.close().await;
    Ok(AkhilaAlignmentReport { krithi_id, scripts })
}

async fn run_smoke_test(args: UploadArgs) -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;
    let base_url = derive_api_base(&config, &root, args.base_url)?;
    let trimmed_base = base_url.trim_end_matches('/').to_string();

    print_step(&format!("API base: {trimmed_base}"));

    let client = build_http_client(30)?;

    let health_url = format!("{trimmed_base}/health");
    print_step("Checking backend health...");
    let health_resp = client.get(&health_url).send().await?;
    if !health_resp.status().is_success() {
        return Err(anyhow!(
            "Health check failed with status {}",
            health_resp.status()
        ));
    }
    let body = health_resp.text().await.unwrap_or_default();
    print_success(&format!("Backend healthy ({})", body));

    let search_url = format!("{trimmed_base}/v1/krithis/search");
    print_step("Checking krithi search...");
    let search_resp = client.get(&search_url).send().await?;
    if !search_resp.status().is_success() {
        return Err(anyhow!(
            "Search check failed with status {}",
            search_resp.status()
        ));
    }
    print_success("Krithi search reachable");

    Ok(())
}

fn derive_api_base(
    config: &Config,
    root: &PathBuf,
    override_base: Option<String>,
) -> Result<String> {
    if let Some(base) = override_base {
        return Ok(base);
    }

    let config_path = root.join("config/application.local.toml");
    if config_path.exists() {
        let contents = std::fs::read_to_string(&config_path)?;
        if let Ok(val) = toml::from_str::<toml::Value>(&contents) {
            if let Some(url) = val
                .get("frontend")
                .and_then(|f| f.get("api_url"))
                .and_then(|v| v.as_str())
            {
                return Ok(url.to_string());
            }
        }
    }

    Ok(format!(
        "http://{}:{}",
        resolve_connectable_host(&config.api_host),
        config.api_port
    ))
}

fn resolve_extraction_inputs(
    args: &ExtractionE2eArgs,
    root: &PathBuf,
) -> Result<(String, String, String)> {
    let source_format = if let Some(value) = args.source_format.as_ref() {
        value.trim().to_uppercase()
    } else {
        match args.scenario {
            ExtractionScenario::PdfSmoke => "PDF".to_string(),
            ExtractionScenario::BlogspotHtml => "HTML".to_string(),
            ExtractionScenario::AkhilaThreeSource => "HTML".to_string(),
            ExtractionScenario::DikshitarASeries => "HTML".to_string(),
            ExtractionScenario::DikshitarKeyCollision => "HTML".to_string(),
        }
    };

    let source_url = if let Some(value) = args.source_url.as_ref() {
        value.trim().to_string()
    } else {
        match args.scenario {
            ExtractionScenario::PdfSmoke => DEFAULT_PDF_E2E_URL.to_string(),
            ExtractionScenario::BlogspotHtml => pick_blogspot_url_from_csv(root)?,
            ExtractionScenario::AkhilaThreeSource => AKHILA_HTML_URL.to_string(),
            ExtractionScenario::DikshitarASeries => pick_blogspot_url_from_csv(root)?,
            ExtractionScenario::DikshitarKeyCollision => pick_blogspot_url_from_csv(root)?,
        }
    };

    if source_url.is_empty() {
        return Err(anyhow!("Source URL cannot be empty"));
    }

    let page_range = if let Some(value) = args.page_range.as_ref() {
        value.trim().to_string()
    } else if source_format == "PDF" {
        "17-18".to_string()
    } else {
        String::new()
    };

    Ok((source_url, source_format, page_range))
}

fn pick_blogspot_url_from_csv(root: &PathBuf) -> Result<String> {
    let csv_path = root.join(BLOGSPOT_E2E_CSV_PATH);
    let contents = std::fs::read_to_string(&csv_path).with_context(|| {
        format!(
            "Failed to read Blogspot scenario CSV at {}",
            csv_path.display()
        )
    })?;

    for line in contents.lines().skip(1) {
        let candidate = line
            .split(',')
            .next_back()
            .map(str::trim)
            .unwrap_or_default();

        if candidate.starts_with("http://") || candidate.starts_with("https://") {
            if candidate.contains("blogspot.") || candidate.contains("guru-guha.blogspot.com") {
                return Ok(candidate.to_string());
            }
        }
    }

    Err(anyhow!("No Blogspot URL found in {}", csv_path.display()))
}

fn build_http_client(timeout_seconds: u64) -> Result<Client> {
    Client::builder()
        .timeout(Duration::from_secs(timeout_seconds))
        // Avoid proxy autodiscovery in constrained shells where system APIs may panic.
        .no_proxy()
        .build()
        .context("Failed to construct HTTP client")
}

fn resolve_connectable_host(configured_host: &str) -> &str {
    match configured_host {
        "0.0.0.0" | "::" => "127.0.0.1",
        other => other,
    }
}

fn stop_backend_process(backend: &mut Option<Child>) {
    if let Some(mut process) = backend.take() {
        let _ = process.kill();
        let _ = process.wait();
    }
}

fn is_extraction_service_running(root: &Path) -> bool {
    let output = Command::new("docker")
        .args([
            "compose",
            "--profile",
            "extraction",
            "ps",
            "--format",
            "json",
            "krithi-extract-enrich-worker",
        ])
        .current_dir(root)
        .output();

    match output {
        Ok(out) if out.status.success() => {
            let stdout = String::from_utf8_lossy(&out.stdout);
            stdout.contains("\"State\":\"running\"")
                || stdout.contains("\"Health\":\"healthy\"")
                || stdout.contains("running")
        }
        _ => false,
    }
}

async fn issue_admin_jwt(client: &Client, api_base: &str, admin_token: &str) -> Result<String> {
    let token_url = format!("{api_base}/v1/auth/token");
    let response = client
        .post(&token_url)
        .json(&json!({
            "adminToken": admin_token,
            "email": "admin@sangitagrantha.org",
            "roles": ["admin"],
        }))
        .send()
        .await
        .context("Failed to call /v1/auth/token")?;

    if !response.status().is_success() {
        let status = response.status();
        let body = response.text().await.unwrap_or_default();
        return Err(anyhow!("Auth token request failed: {status}. Body: {body}"));
    }

    let payload: Value = response
        .json()
        .await
        .context("Invalid /v1/auth/token JSON response")?;
    payload
        .get("token")
        .and_then(Value::as_str)
        .map(|token| token.to_string())
        .ok_or_else(|| anyhow!("Missing token in /v1/auth/token response"))
}

async fn submit_extraction_task(
    client: &Client,
    api_base: &str,
    token: &str,
    source_url: &str,
    source_format: &str,
    page_range: &str,
) -> Result<String> {
    let extraction_url = format!("{api_base}/v1/admin/sourcing/extractions");
    let response = client
        .post(&extraction_url)
        .bearer_auth(token)
        .json(&json!({
            "sourceUrl": source_url,
            "sourceFormat": source_format,
            "pageRange": if page_range.trim().is_empty() { Value::Null } else { Value::String(page_range.to_string()) },
            "composerHint": "Muthuswami Dikshitar",
            "maxAttempts": 3,
            "extractionIntent": "PRIMARY",
        }))
        .send()
        .await
        .context("Failed to submit extraction request")?;

    if response.status() != StatusCode::CREATED {
        let status = response.status();
        let body = response.text().await.unwrap_or_default();
        return Err(anyhow!(
            "Extraction submission failed: expected 201, got {status}. Body: {body}"
        ));
    }

    let payload: Value = response
        .json()
        .await
        .context("Invalid extraction submission JSON response")?;

    payload
        .get("id")
        .and_then(Value::as_str)
        .map(|id| id.to_string())
        .ok_or_else(|| anyhow!("Missing extraction task id in submission response"))
}

async fn wait_for_ingested(
    client: &Client,
    api_base: &str,
    token: &str,
    task_id: &str,
    timeout_seconds: u64,
    poll_interval_seconds: u64,
) -> Result<()> {
    let poll_url = format!("{api_base}/v1/admin/sourcing/extractions/{task_id}");
    let deadline = Instant::now() + Duration::from_secs(timeout_seconds);
    let poll_every = Duration::from_secs(poll_interval_seconds.max(1));
    let mut seen_statuses: Vec<String> = Vec::new();

    while Instant::now() < deadline {
        let response = client
            .get(&poll_url)
            .bearer_auth(token)
            .send()
            .await
            .context("Failed polling extraction detail endpoint")?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(anyhow!("Extraction polling failed: {status}. Body: {body}"));
        }

        let detail: Value = response
            .json()
            .await
            .context("Invalid extraction detail JSON response")?;
        let status = detail
            .get("status")
            .and_then(Value::as_str)
            .unwrap_or("UNKNOWN")
            .to_string();

        if !seen_statuses.contains(&status) {
            seen_statuses.push(status.clone());
            print_info(&format!(
                "Task {task_id} status transition: {}",
                seen_statuses.last().cloned().unwrap_or_default()
            ));
        }

        match status.as_str() {
            "INGESTED" => return Ok(()),
            "FAILED" => {
                let error = detail.get("errorDetail").cloned().unwrap_or(Value::Null);
                return Err(anyhow!("Extraction failed. errorDetail={error}"));
            }
            "CANCELLED" => return Err(anyhow!("Extraction was cancelled")),
            _ => {}
        }

        sleep(poll_every).await;
    }

    Err(anyhow!(
        "Timed out waiting for extraction {} to reach INGESTED after {} seconds. Seen statuses: {}",
        task_id,
        timeout_seconds,
        seen_statuses.join(" -> ")
    ))
}

async fn validate_extraction_outcome(
    app_config: &AppConfig,
    task_id: &str,
    source_url: &str,
    min_result_count: i64,
) -> Result<DbValidationReport> {
    let connection_string = ConnectionString::new(app_config).to_string();
    let options =
        PgConnectOptions::from_str(&connection_string)?.log_statements(log::LevelFilter::Off);
    let pool = sqlx::PgPool::connect_with(options)
        .await
        .context("Failed to connect to PostgreSQL for validation checks")?;

    let row = sqlx::query(
        r#"
        SELECT
            status::text AS status,
            COALESCE(result_count, 0)::bigint AS result_count,
            extraction_method,
            extractor_version,
            result_payload
        FROM extraction_queue
        WHERE id = $1::uuid
        "#,
    )
    .bind(task_id)
    .fetch_one(&pool)
    .await
    .with_context(|| format!("Failed to load extraction_queue row for task {task_id}"))?;

    let status: String = row.get("status");
    let result_count: i64 = row.get("result_count");
    let extraction_method: Option<String> = row.get("extraction_method");
    let extractor_version: Option<String> = row.get("extractor_version");
    let payload: Option<Value> = row.get("result_payload");

    if status != "INGESTED" {
        pool.close().await;
        return Err(anyhow!(
            "Expected extraction_queue.status=INGESTED, got {status} for task {task_id}"
        ));
    }

    if result_count < min_result_count {
        pool.close().await;
        return Err(anyhow!(
            "Expected result_count >= {}, got {} for task {}",
            min_result_count,
            result_count,
            task_id
        ));
    }

    let payload_count = payload
        .as_ref()
        .and_then(Value::as_array)
        .map(|arr| arr.len() as i64)
        .unwrap_or(0);

    if result_count > 0 && payload_count == 0 {
        pool.close().await;
        return Err(anyhow!(
            "result_count is {}, but result_payload is empty for task {}",
            result_count,
            task_id
        ));
    }

    if payload_count > 0 && payload_count != result_count {
        pool.close().await;
        return Err(anyhow!(
            "result_count ({}) does not match result_payload length ({}) for task {}",
            result_count,
            payload_count,
            task_id
        ));
    }

    let mut min_section_count = 0i64;
    let mut min_variant_count = 0i64;
    if let Some(items) = payload.as_ref().and_then(Value::as_array) {
        min_section_count = i64::MAX;
        min_variant_count = i64::MAX;
        let mut leakage_examples: Vec<String> = Vec::new();

        for (idx, item) in items.iter().enumerate() {
            let sections = item
                .get("sections")
                .and_then(Value::as_array)
                .map(|arr| arr.len() as i64)
                .unwrap_or(0);
            let variants = item
                .get("lyricVariants")
                .and_then(Value::as_array)
                .map(|arr| arr.len() as i64)
                .unwrap_or(0);
            min_section_count = min_section_count.min(sections);
            min_variant_count = min_variant_count.min(variants);

            if sections == 0 {
                pool.close().await;
                return Err(anyhow!(
                    "Payload regression: extraction[{}] has no sections for task {}",
                    idx,
                    task_id
                ));
            }
            if variants == 0 {
                pool.close().await;
                return Err(anyhow!(
                    "Payload regression: extraction[{}] has no lyricVariants for task {}",
                    idx,
                    task_id
                ));
            }

            if let Some(identity) = item.get("identityCandidates") {
                let composers = identity
                    .get("composers")
                    .and_then(Value::as_array)
                    .ok_or_else(|| {
                        anyhow!(
                            "Payload regression: extraction[{}].identityCandidates.composers is missing/invalid for task {}",
                            idx,
                            task_id
                        )
                    })?;
                let ragas = identity
                    .get("ragas")
                    .and_then(Value::as_array)
                    .ok_or_else(|| {
                        anyhow!(
                            "Payload regression: extraction[{}].identityCandidates.ragas is missing/invalid for task {}",
                            idx,
                            task_id
                        )
                    })?;
                for candidate in composers.iter().chain(ragas.iter()) {
                    let score = candidate
                        .get("score")
                        .and_then(Value::as_i64)
                        .ok_or_else(|| {
                            anyhow!(
                                "Payload regression: extraction[{}].identityCandidates contains candidate without numeric score for task {}",
                                idx,
                                task_id
                            )
                        })?;
                    if !(0..=100).contains(&score) {
                        pool.close().await;
                        return Err(anyhow!(
                            "Payload regression: extraction[{}].identityCandidates score={} out of range [0,100] for task {}",
                            idx,
                            score,
                            task_id
                        ));
                    }
                    let confidence = candidate
                        .get("confidence")
                        .and_then(Value::as_str)
                        .unwrap_or_default();
                    if !matches!(confidence, "HIGH" | "MEDIUM" | "LOW") {
                        pool.close().await;
                        return Err(anyhow!(
                            "Payload regression: extraction[{}].identityCandidates has invalid confidence '{}' for task {}",
                            idx,
                            confidence,
                            task_id
                        ));
                    }
                }
            }

            if let Some(enrichment) = item.get("metadataEnrichment") {
                let provider = enrichment
                    .get("provider")
                    .and_then(Value::as_str)
                    .unwrap_or_default()
                    .trim()
                    .to_string();
                if provider.is_empty() {
                    pool.close().await;
                    return Err(anyhow!(
                        "Payload regression: extraction[{}].metadataEnrichment.provider is missing for task {}",
                        idx,
                        task_id
                    ));
                }
                let applied = enrichment
                    .get("applied")
                    .and_then(Value::as_bool)
                    .ok_or_else(|| {
                        anyhow!(
                            "Payload regression: extraction[{}].metadataEnrichment.applied is missing/invalid for task {}",
                            idx,
                            task_id
                        )
                    })?;
                let fields_updated = enrichment
                    .get("fieldsUpdated")
                    .and_then(Value::as_array)
                    .cloned()
                    .unwrap_or_default();
                if applied && fields_updated.is_empty() {
                    pool.close().await;
                    return Err(anyhow!(
                        "Payload regression: extraction[{}].metadataEnrichment.applied=true but fieldsUpdated empty for task {}",
                        idx,
                        task_id
                    ));
                }
            }

            let variant_items = item
                .get("lyricVariants")
                .and_then(Value::as_array)
                .cloned()
                .unwrap_or_default();
            for (variant_idx, variant) in variant_items.iter().enumerate() {
                let lyric_sections = variant
                    .get("sections")
                    .and_then(Value::as_array)
                    .cloned()
                    .unwrap_or_default();
                if lyric_sections.is_empty() {
                    pool.close().await;
                    return Err(anyhow!(
                        "Payload regression: extraction[{}].lyricVariants[{}] has no sections for task {}",
                        idx,
                        variant_idx,
                        task_id
                    ));
                }
                for lyric_section in lyric_sections {
                    let text = lyric_section
                        .get("text")
                        .and_then(Value::as_str)
                        .unwrap_or_default();
                    if looks_like_metadata_heading(text) {
                        leakage_examples.push(format!(
                            "extraction[{}].lyricVariants[{}]: '{}'",
                            idx,
                            variant_idx,
                            text.lines().next().unwrap_or_default().trim()
                        ));
                    }
                }
            }
        }

        if !leakage_examples.is_empty() {
            let sample = leakage_examples
                .into_iter()
                .take(8)
                .collect::<Vec<_>>()
                .join(" | ");
            pool.close().await;
            return Err(anyhow!(
                "Payload regression: lyric sections contain metadata-heading text for task {}. Samples: {}",
                task_id,
                sample
            ));
        }
    }

    let extraction_method = extraction_method
        .filter(|v| !v.trim().is_empty())
        .ok_or_else(|| anyhow!("Missing extraction_method for task {task_id}"))?;
    let extractor_version = extractor_version
        .filter(|v| !v.trim().is_empty())
        .ok_or_else(|| anyhow!("Missing extractor_version for task {task_id}"))?;

    let evidence_row = sqlx::query(
        r#"
        SELECT COUNT(*)::bigint AS evidence_count
        FROM krithi_source_evidence e
        WHERE source_url = $1
          AND e.extracted_at >= (
              SELECT q.created_at
              FROM extraction_queue q
              WHERE q.id = $2::uuid
          )
        "#,
    )
    .bind(source_url)
    .bind(task_id)
    .fetch_one(&pool)
    .await
    .context("Failed to query krithi_source_evidence counts")?;

    let evidence_count_after_task: i64 = evidence_row.get("evidence_count");

    let evidence_total_row = sqlx::query(
        r#"
        SELECT COUNT(*)::bigint AS evidence_count
        FROM krithi_source_evidence
        WHERE source_url = $1
        "#,
    )
    .bind(source_url)
    .fetch_one(&pool)
    .await
    .context("Failed to query total krithi_source_evidence count for source_url")?;

    let evidence_total_count: i64 = evidence_total_row.get("evidence_count");
    let evidence_count = if evidence_count_after_task > 0 {
        evidence_count_after_task
    } else {
        evidence_total_count
    };

    if result_count > 0 && evidence_count == 0 {
        pool.close().await;
        return Err(anyhow!(
            "Expected at least one krithi_source_evidence row for source_url {} (task {}), found 0",
            source_url,
            task_id
        ));
    }

    if evidence_count_after_task == 0 && evidence_total_count > 0 {
        print_warning(&format!(
            "No new evidence rows were created after task creation time; using existing evidence row(s) for source_url {} (count={}).",
            source_url, evidence_total_count
        ));
    }

    if evidence_count < result_count {
        print_warning(&format!(
            "Evidence count ({}) is lower than result count ({}). This can occur if some extractions are filtered during ingestion.",
            evidence_count, result_count
        ));
    }

    pool.close().await;
    Ok(DbValidationReport {
        status,
        result_count,
        evidence_count,
        extraction_method,
        extractor_version,
        min_section_count,
        min_variant_count,
    })
}

fn looks_like_metadata_heading(text: &str) -> bool {
    text.lines().take(3).any(|line| {
        let lowered = line.trim().to_lowercase();
        lowered.starts_with("meaning")
            || lowered.starts_with("notes")
            || lowered.starts_with("gist")
            || lowered.starts_with("word division")
            || lowered.starts_with("variations")
    })
}
