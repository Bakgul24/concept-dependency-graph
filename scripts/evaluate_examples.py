#!/usr/bin/env python3
"""Evaluate extracted graphs against curated example expectations.

Modes:
  mock:    use expected_graph.json as the actual graph for a deterministic smoke run.
  saved:   read actual_graph.json from each example directory.
  backend: call a running backend, then fetch the produced graph response.
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EXAMPLES_DIR = ROOT / "examples"


# =========================
# 🔧 NORMALIZATION FIX
# =========================
def normalize_id(id_str: str) -> str:
    if not id_str:
        return ""
    return (
        id_str.lower()
        .replace("_graph", "")
        .replace("-", "_")
        .strip()
    )


def concept_ids(graph: dict) -> set[str]:
    return {
        normalize_id(item["id"])
        for item in graph.get("concepts", [])
    }


def dependency_pairs(graph: dict) -> set[tuple[str, str]]:
    return {
        (
            normalize_id(item["from"]),
            normalize_id(item["to"])
        )
        for item in graph.get("dependencies", [])
    }


def safe_div(numerator: int, denominator: int) -> float:
    return round(numerator / denominator, 4) if denominator else 1.0


def normalize_graph_response(graph_response: dict) -> dict:
    concepts = []
    for node in graph_response.get("nodes", []):
        data = node.get("data", {})
        concepts.append({
            "id": node.get("id"),
            "name": data.get("label", node.get("id")),
            "type": data.get("conceptType", "DEFINITION"),
        })

    dependencies = []
    for edge in graph_response.get("edges", []):
        data = edge.get("data") or {}
        dependencies.append({
            "from": edge.get("source"),
            "to": edge.get("target"),
            "reason": data.get("reason") or edge.get("reason", ""),
        })

    return {"concepts": concepts, "dependencies": dependencies}


def backend_extract(example_dir: Path, backend_url: str) -> dict:
    text = (example_dir / "input.md").read_text(encoding="utf-8")
    payload = json.dumps({
        "title": example_dir.name,
        "text": text,
    }).encode("utf-8")

    request = urllib.request.Request(
        f"{backend_url.rstrip('/')}/graphs/analyze",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    with urllib.request.urlopen(request, timeout=180) as response:
        analysis = json.loads(response.read().decode("utf-8"))

    graph_id = analysis["graphId"]

    with urllib.request.urlopen(
        f"{backend_url.rstrip('/')}/graphs/{graph_id}",
        timeout=60
    ) as response:
        return normalize_graph_response(json.loads(response.read().decode("utf-8")))


def load_actual(example_dir: Path, mode: str, backend_url: str | None) -> tuple[dict, str]:
    if mode == "mock":
        return json.loads((example_dir / "expected_graph.json").read_text(encoding="utf-8")), "mock"

    if mode == "saved":
        actual_path = example_dir / "actual_graph.json"
        if not actual_path.exists():
            raise FileNotFoundError(f"Missing saved extraction: {actual_path}")
        return json.loads(actual_path.read_text(encoding="utf-8")), "saved"

    if mode == "backend":
        if not backend_url:
            raise ValueError("--backend-url is required in backend mode")
        return backend_extract(example_dir, backend_url), "backend"

    raise ValueError(f"Unknown mode: {mode}")


def evaluate_example(example_dir: Path, mode: str, backend_url: str | None) -> dict:
    expected = json.loads((example_dir / "expected_graph.json").read_text(encoding="utf-8"))
    actual, source = load_actual(example_dir, mode, backend_url)

    expected_concepts = concept_ids(expected)
    actual_concepts = concept_ids(actual)

    expected_dependencies = dependency_pairs(expected)
    actual_dependencies = dependency_pairs(actual)

    concept_true_positive = len(expected_concepts & actual_concepts)
    dependency_true_positive = len(expected_dependencies & actual_dependencies)

    return {
        "example": example_dir.name,
        "source": source,

        "conceptPrecision": safe_div(concept_true_positive, len(actual_concepts)),
        "conceptRecall": safe_div(concept_true_positive, len(expected_concepts)),

        "dependencyPrecision": safe_div(dependency_true_positive, len(actual_dependencies)),
        "dependencyRecall": safe_div(dependency_true_positive, len(expected_dependencies)),

        "expectedConcepts": sorted(expected_concepts),
        "actualConcepts": sorted(actual_concepts),

        "missingConcepts": sorted(expected_concepts - actual_concepts),
        "extraConcepts": sorted(actual_concepts - expected_concepts),

        "missingDependencies": sorted(
            [{"from": a, "to": b} for a, b in expected_dependencies - actual_dependencies],
            key=lambda x: (x["from"], x["to"])
        ),

        "extraDependencies": sorted(
            [{"from": a, "to": b} for a, b in actual_dependencies - expected_dependencies],
            key=lambda x: (x["from"], x["to"])
        ),
    }


def write_report(results: list[dict], mode: str) -> None:
    output = {
        "mode": mode,
        "examples": results,
        "macroAverage": {
            "conceptPrecision": round(sum(r["conceptPrecision"] for r in results) / len(results), 4),
            "conceptRecall": round(sum(r["conceptRecall"] for r in results) / len(results), 4),
            "dependencyPrecision": round(sum(r["dependencyPrecision"] for r in results) / len(results), 4),
            "dependencyRecall": round(sum(r["dependencyRecall"] for r in results) / len(results), 4),
        },
    }

    (ROOT / "evaluation-results.json").write_text(json.dumps(output, indent=2), encoding="utf-8")

    lines = [
        "# Evaluation Report",
        "",
        f"Mode: `{mode}`",
        "",
        "| Example | Concept P | Concept R | Dependency P | Dependency R |",
        "| --- | ---: | ---: | ---: | ---: |",
    ]

    for result in results:
        lines.append(
            f"| {result['example']} | {result['conceptPrecision']:.2f} | {result['conceptRecall']:.2f} | "
            f"{result['dependencyPrecision']:.2f} | {result['dependencyRecall']:.2f} |"
        )

    avg = output["macroAverage"]

    lines.extend([
        "",
        "## Macro Average",
        "",
        f"- Concept precision: {avg['conceptPrecision']:.2f}",
        f"- Concept recall: {avg['conceptRecall']:.2f}",
        f"- Dependency precision: {avg['dependencyPrecision']:.2f}",
        f"- Dependency recall: {avg['dependencyRecall']:.2f}",
        "",
        "In `mock` mode the script uses expected graphs as deterministic actual outputs. Use `backend` mode for a real extraction run against a local backend.",
    ])

    (ROOT / "evaluation-report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["mock", "saved", "backend"], default="mock")
    parser.add_argument("--backend-url", default="http://localhost:8080/api")
    args = parser.parse_args()

    example_dirs = sorted(
        path for path in EXAMPLES_DIR.iterdir()
        if (path / "expected_graph.json").exists()
    )

    if not example_dirs:
        print("No examples found.", file=sys.stderr)
        return 1

    try:
        results = [evaluate_example(path, args.mode, args.backend_url) for path in example_dirs]
    except (FileNotFoundError, ValueError, urllib.error.URLError) as exc:
        print(f"Evaluation failed: {exc}", file=sys.stderr)
        return 2

    write_report(results, args.mode)
    print("Wrote evaluation-results.json and evaluation-report.md")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())