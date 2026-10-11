#!/usr/bin/env python3
"""Build reproducible derived analysis from the formal N-Queens experiment logs.

The script reads experiments/raw only. It never edits, deletes, or regenerates raw
experiment records. All generated files are written beside this script.
"""

from __future__ import annotations

import csv
import hashlib
import json
import math
import re
from collections import defaultdict
from dataclasses import dataclass
from decimal import Decimal, getcontext
from pathlib import Path
from typing import Iterable, Sequence

from PIL import Image, ImageDraw, ImageFont


getcontext().prec = 40

ARCHITECTURES = {
    "pipefilter": "管道-过滤器",
    "callreturn-iterative": "调用/返回（迭代）",
    "callreturn-backtracking": "调用/返回（回溯）",
    "blackboard": "黑板",
    "mapreduce": "MapReduce（单机模拟）",
}
ARCHITECTURE_ENGLISH = {
    "pipefilter": "Pipe-Filter",
    "callreturn-iterative": "Call-Return Iterative",
    "callreturn-backtracking": "Call-Return Backtracking",
    "blackboard": "Blackboard",
    "mapreduce": "MapReduce (Single-Machine)",
}
ARCHITECTURE_ORDER = list(ARCHITECTURES)
EXPECTED_N = (8, 10, 12)
EXPECTED_RUNS_PER_GROUP = 5
RUN_DIRECTORY_PATTERN = re.compile(r"^N(?P<n>\d+)-run(?P<run>\d+)$")
COLORS = {
    "pipefilter": "#4C78A8",
    "callreturn-iterative": "#F58518",
    "callreturn-backtracking": "#54A24B",
    "blackboard": "#B279A2",
    "mapreduce": "#E45756",
}


@dataclass(frozen=True)
class RunRecord:
    architecture_id: str
    architecture: str
    n: int
    batch: str
    run_number: int
    run_id: str
    source_file: str
    stderr_file: str
    command_file: str
    exit_code: int
    status: str
    success: bool
    protocol_version: int
    mode: str
    solve_elapsed_ns: int
    first_solution: str
    stderr_empty: bool

    @property
    def solve_elapsed_ms(self) -> Decimal:
        return Decimal(self.solve_elapsed_ns) / Decimal(1_000_000)


@dataclass(frozen=True)
class GroupStatistics:
    architecture_id: str
    architecture: str
    n: int
    run_count: int
    success_count: int
    success_rate: Decimal
    mean_ms: Decimal
    median_ms: Decimal
    min_ms: Decimal
    max_ms: Decimal
    std_ms: Decimal
    first_solution_consistent: bool
    stderr_all_empty: bool


def project_root() -> Path:
    return Path(__file__).resolve().parents[2]


def relative_path(path: Path, root: Path) -> str:
    return path.resolve().relative_to(root.resolve()).as_posix()


def boolean_text(value: bool) -> str:
    return "true" if value else "false"


def decimal_text(value: Decimal) -> str:
    if value == 0:
        return "0"
    return format(value.normalize(), "f")


def rounded_decimal_text(value: Decimal, places: int = 9) -> str:
    quantum = Decimal(1).scaleb(-places)
    return format(value.quantize(quantum), "f")


def display_ms(value: Decimal) -> str:
    return f"{value:.3f}"


def display_ratio(value: Decimal) -> str:
    return f"{value:.2f}"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def validate_solution(columns: Sequence[int], n: int) -> None:
    if len(columns) != n:
        raise ValueError(f"N={n}: solution has {len(columns)} columns")
    for row, column in enumerate(columns):
        if not isinstance(column, int) or not 0 <= column < n:
            raise ValueError(f"N={n}: invalid column {column!r} at row {row}")
        for previous_row in range(row):
            previous_column = columns[previous_row]
            if previous_column == column or abs(previous_column - column) == row - previous_row:
                raise ValueError(f"N={n}: attacking queens at rows {previous_row} and {row}")


def read_records(root: Path) -> tuple[list[RunRecord], list[Path]]:
    raw_root = root / "experiments" / "raw"
    if not raw_root.is_dir():
        raise FileNotFoundError(f"Formal experiment directory not found: {raw_root}")

    records: list[RunRecord] = []
    environment_files: list[Path] = []

    architecture_directories = sorted(
        (path for path in raw_root.iterdir() if path.is_dir()),
        key=lambda path: ARCHITECTURE_ORDER.index(path.name)
        if path.name in ARCHITECTURE_ORDER
        else len(ARCHITECTURE_ORDER),
    )
    actual_architectures = {path.name for path in architecture_directories}
    if actual_architectures != set(ARCHITECTURES):
        raise ValueError(
            "Architecture directories differ from the expected set: "
            f"actual={sorted(actual_architectures)}"
        )

    for architecture_directory in architecture_directories:
        architecture_id = architecture_directory.name
        batch_directories = sorted(path for path in architecture_directory.iterdir() if path.is_dir())
        if len(batch_directories) != 1:
            raise ValueError(
                f"{architecture_id}: expected exactly one formal batch, found {len(batch_directories)}"
            )

        for batch_directory in batch_directories:
            environment_file = batch_directory / "environment.txt"
            compile_exit_file = batch_directory / "compile-exit-code.txt"
            required_batch_files = (
                environment_file,
                batch_directory / "compile-command.txt",
                batch_directory / "compile-stdout.log",
                batch_directory / "compile-stderr.log",
                compile_exit_file,
            )
            missing_batch_files = [path.name for path in required_batch_files if not path.is_file()]
            if missing_batch_files:
                raise ValueError(
                    f"{architecture_id}/{batch_directory.name}: missing batch files "
                    f"{missing_batch_files}"
                )
            if int(compile_exit_file.read_text(encoding="utf-8").strip()) != 0:
                raise ValueError(f"{architecture_id}/{batch_directory.name}: compilation failed")
            environment_files.append(environment_file)

            for run_directory in sorted(path for path in batch_directory.iterdir() if path.is_dir()):
                match = RUN_DIRECTORY_PATTERN.fullmatch(run_directory.name)
                if match is None:
                    raise ValueError(f"Unexpected directory in formal batch: {run_directory}")
                n = int(match.group("n"))
                run_number = int(match.group("run"))

                stdout_file = run_directory / "stdout.log"
                stderr_file = run_directory / "stderr.log"
                exit_code_file = run_directory / "exit-code.txt"
                command_file = run_directory / "command.txt"
                required_run_files = (stdout_file, stderr_file, exit_code_file, command_file)
                missing_run_files = [path.name for path in required_run_files if not path.is_file()]
                if missing_run_files:
                    raise ValueError(
                        f"{relative_path(run_directory, root)}: missing files {missing_run_files}"
                    )

                output_text = stdout_file.read_text(encoding="utf-8").strip()
                try:
                    output = json.loads(output_text)
                except json.JSONDecodeError as error:
                    raise ValueError(f"Invalid JSON in {stdout_file}: {error}") from error
                if output.get("architecture") != architecture_id:
                    raise ValueError(f"Architecture mismatch in {stdout_file}")
                if output.get("n") != n:
                    raise ValueError(f"N mismatch in {stdout_file}")
                if output.get("mode") != "first":
                    raise ValueError(f"Unexpected mode in {stdout_file}: {output.get('mode')!r}")

                elapsed_ns = output.get("solve_elapsed_ns")
                if not isinstance(elapsed_ns, int) or elapsed_ns < 0:
                    raise ValueError(f"Invalid solve_elapsed_ns in {stdout_file}")
                columns = output.get("columns")
                if not isinstance(columns, list):
                    raise ValueError(f"Invalid columns in {stdout_file}")

                exit_code = int(exit_code_file.read_text(encoding="utf-8").strip())
                status = str(output.get("status", ""))
                success = exit_code == 0 and status == "solved"
                if success:
                    validate_solution(columns, n)
                first_solution = json.dumps(columns, ensure_ascii=False, separators=(",", ":"))

                records.append(
                    RunRecord(
                        architecture_id=architecture_id,
                        architecture=ARCHITECTURES[architecture_id],
                        n=n,
                        batch=batch_directory.name,
                        run_number=run_number,
                        run_id=f"run{run_number:02d}",
                        source_file=relative_path(stdout_file, root),
                        stderr_file=relative_path(stderr_file, root),
                        command_file=relative_path(command_file, root),
                        exit_code=exit_code,
                        status=status,
                        success=success,
                        protocol_version=int(output.get("protocol_version")),
                        mode=str(output.get("mode")),
                        solve_elapsed_ns=elapsed_ns,
                        first_solution=first_solution,
                        stderr_empty=stderr_file.stat().st_size == 0,
                    )
                )

    records.sort(
        key=lambda record: (
            ARCHITECTURE_ORDER.index(record.architecture_id),
            record.n,
            record.batch,
            record.run_number,
        )
    )
    return records, environment_files


def validate_dataset(records: Sequence[RunRecord], environment_files: Sequence[Path]) -> None:
    if len(records) != len(ARCHITECTURES) * len(EXPECTED_N) * EXPECTED_RUNS_PER_GROUP:
        raise ValueError(f"Expected 75 formal runs, found {len(records)}")

    grouped: dict[tuple[str, int], list[RunRecord]] = defaultdict(list)
    for record in records:
        grouped[(record.architecture_id, record.n)].append(record)

    expected_groups = {
        (architecture_id, n) for architecture_id in ARCHITECTURE_ORDER for n in EXPECTED_N
    }
    if set(grouped) != expected_groups:
        raise ValueError(f"Expected 15 architecture × N groups, found {len(grouped)}")

    for key, group in grouped.items():
        if len(group) != EXPECTED_RUNS_PER_GROUP:
            raise ValueError(f"{key}: expected 5 runs, found {len(group)}")
        if [record.run_number for record in group] != [1, 2, 3, 4, 5]:
            raise ValueError(f"{key}: run IDs are not exactly 01..05")
        if not all(record.success for record in group):
            raise ValueError(f"{key}: at least one run was not successful")
        if not all(record.stderr_empty for record in group):
            raise ValueError(f"{key}: at least one stderr file is non-empty")
        if len({record.first_solution for record in group}) != 1:
            raise ValueError(f"{key}: first solution differs within the group")

    for n in EXPECTED_N:
        solutions = {record.first_solution for record in records if record.n == n}
        if len(solutions) != 1:
            raise ValueError(f"N={n}: first solution differs across implementations")

    if len(environment_files) != len(ARCHITECTURES):
        raise ValueError(f"Expected 5 environment files, found {len(environment_files)}")
    environment_hashes = {sha256(path) for path in environment_files}
    if len(environment_hashes) != 1:
        raise ValueError("Formal batch environment files are not byte-identical")


def median_decimal(values: Sequence[Decimal]) -> Decimal:
    ordered = sorted(values)
    middle = len(ordered) // 2
    if len(ordered) % 2:
        return ordered[middle]
    return (ordered[middle - 1] + ordered[middle]) / Decimal(2)


def sample_std_decimal(values: Sequence[Decimal]) -> Decimal:
    if len(values) < 2:
        return Decimal(0)
    mean = sum(values, Decimal(0)) / Decimal(len(values))
    variance = sum((value - mean) ** 2 for value in values) / Decimal(len(values) - 1)
    return variance.sqrt()


def calculate_statistics(records: Sequence[RunRecord]) -> list[GroupStatistics]:
    grouped: dict[tuple[str, int], list[RunRecord]] = defaultdict(list)
    for record in records:
        grouped[(record.architecture_id, record.n)].append(record)

    statistics_rows: list[GroupStatistics] = []
    for architecture_id in ARCHITECTURE_ORDER:
        for n in EXPECTED_N:
            group = grouped[(architecture_id, n)]
            values = [record.solve_elapsed_ms for record in group]
            success_count = sum(record.success for record in group)
            statistics_rows.append(
                GroupStatistics(
                    architecture_id=architecture_id,
                    architecture=ARCHITECTURES[architecture_id],
                    n=n,
                    run_count=len(group),
                    success_count=success_count,
                    success_rate=Decimal(success_count) / Decimal(len(group)),
                    mean_ms=sum(values, Decimal(0)) / Decimal(len(values)),
                    median_ms=median_decimal(values),
                    min_ms=min(values),
                    max_ms=max(values),
                    std_ms=sample_std_decimal(values),
                    first_solution_consistent=len({record.first_solution for record in group}) == 1,
                    stderr_all_empty=all(record.stderr_empty for record in group),
                )
            )
    return statistics_rows


def write_csv(path: Path, fieldnames: Sequence[str], rows: Iterable[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8-sig", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def write_run_index(path: Path, records: Sequence[RunRecord]) -> None:
    fields = [
        "architecture",
        "architecture_id",
        "n",
        "batch",
        "run_id",
        "source_file",
        "stderr_file",
        "command_file",
        "exit_code",
        "status",
        "success",
        "protocol_version",
        "mode",
        "solve_elapsed_ns",
        "solve_elapsed_ms",
        "first_solution",
        "stderr_empty",
    ]
    write_csv(
        path,
        fields,
        (
            {
                "architecture": record.architecture,
                "architecture_id": record.architecture_id,
                "n": record.n,
                "batch": record.batch,
                "run_id": record.run_id,
                "source_file": record.source_file,
                "stderr_file": record.stderr_file,
                "command_file": record.command_file,
                "exit_code": record.exit_code,
                "status": record.status,
                "success": boolean_text(record.success),
                "protocol_version": record.protocol_version,
                "mode": record.mode,
                "solve_elapsed_ns": record.solve_elapsed_ns,
                "solve_elapsed_ms": decimal_text(record.solve_elapsed_ms),
                "first_solution": record.first_solution,
                "stderr_empty": boolean_text(record.stderr_empty),
            }
            for record in records
        ),
    )


def write_raw_run_details(path: Path, records: Sequence[RunRecord]) -> None:
    fields = [
        "architecture",
        "architecture_id",
        "n",
        "batch",
        "run_id",
        "solve_elapsed_ns",
        "solve_elapsed_ms",
        "success",
        "first_solution",
    ]
    write_csv(
        path,
        fields,
        (
            {
                "architecture": record.architecture,
                "architecture_id": record.architecture_id,
                "n": record.n,
                "batch": record.batch,
                "run_id": record.run_id,
                "solve_elapsed_ns": record.solve_elapsed_ns,
                "solve_elapsed_ms": decimal_text(record.solve_elapsed_ms),
                "success": boolean_text(record.success),
                "first_solution": record.first_solution,
            }
            for record in records
        ),
    )


def write_detail_results(path: Path, statistics_rows: Sequence[GroupStatistics]) -> None:
    fields = [
        "architecture",
        "architecture_id",
        "n",
        "run_count",
        "success_count",
        "success_rate",
        "mean_ms",
        "median_ms",
        "min_ms",
        "max_ms",
        "std_ms",
        "first_solution_consistent",
        "stderr_all_empty",
    ]
    write_csv(
        path,
        fields,
        (
            {
                "architecture": row.architecture,
                "architecture_id": row.architecture_id,
                "n": row.n,
                "run_count": row.run_count,
                "success_count": row.success_count,
                "success_rate": decimal_text(row.success_rate),
                "mean_ms": decimal_text(row.mean_ms),
                "median_ms": decimal_text(row.median_ms),
                "min_ms": decimal_text(row.min_ms),
                "max_ms": decimal_text(row.max_ms),
                "std_ms": rounded_decimal_text(row.std_ms),
                "first_solution_consistent": boolean_text(row.first_solution_consistent),
                "stderr_all_empty": boolean_text(row.stderr_all_empty),
            }
            for row in statistics_rows
        ),
    )


def statistics_lookup(
    statistics_rows: Sequence[GroupStatistics],
) -> dict[tuple[str, int], GroupStatistics]:
    return {(row.architecture_id, row.n): row for row in statistics_rows}


def write_summary_statistics(path: Path, statistics_rows: Sequence[GroupStatistics]) -> None:
    lookup = statistics_lookup(statistics_rows)
    fields = ["architecture", "architecture_id", "overall_run_count", "overall_success_count"]
    for metric in ("mean", "median", "min", "max"):
        for n in EXPECTED_N:
            fields.append(f"n{n}_{metric}_ms")

    rows: list[dict[str, object]] = []
    for architecture_id in ARCHITECTURE_ORDER:
        groups = [lookup[(architecture_id, n)] for n in EXPECTED_N]
        row: dict[str, object] = {
            "architecture": ARCHITECTURES[architecture_id],
            "architecture_id": architecture_id,
            "overall_run_count": sum(group.run_count for group in groups),
            "overall_success_count": sum(group.success_count for group in groups),
        }
        for metric in ("mean", "median", "min", "max"):
            for n in EXPECTED_N:
                row[f"n{n}_{metric}_ms"] = decimal_text(
                    getattr(lookup[(architecture_id, n)], f"{metric}_ms")
                )
        rows.append(row)
    write_csv(path, fields, rows)


def growth_ratios(
    statistics_rows: Sequence[GroupStatistics],
) -> dict[str, tuple[Decimal, Decimal, Decimal]]:
    lookup = statistics_lookup(statistics_rows)
    ratios: dict[str, tuple[Decimal, Decimal, Decimal]] = {}
    for architecture_id in ARCHITECTURE_ORDER:
        n8 = lookup[(architecture_id, 8)].median_ms
        n10 = lookup[(architecture_id, 10)].median_ms
        n12 = lookup[(architecture_id, 12)].median_ms
        ratios[architecture_id] = (n10 / n8, n12 / n10, n12 / n8)
    return ratios


def write_growth_ratios(path: Path, statistics_rows: Sequence[GroupStatistics]) -> None:
    ratios = growth_ratios(statistics_rows)
    fields = [
        "architecture",
        "architecture_id",
        "n8_to_n10_ratio",
        "n10_to_n12_ratio",
        "n8_to_n12_ratio",
    ]
    write_csv(
        path,
        fields,
        (
            {
                "architecture": ARCHITECTURES[architecture_id],
                "architecture_id": architecture_id,
                "n8_to_n10_ratio": rounded_decimal_text(ratios[architecture_id][0]),
                "n10_to_n12_ratio": rounded_decimal_text(ratios[architecture_id][1]),
                "n8_to_n12_ratio": rounded_decimal_text(ratios[architecture_id][2]),
            }
            for architecture_id in ARCHITECTURE_ORDER
        ),
    )


@dataclass(frozen=True)
class PlotFonts:
    regular_path: Path
    bold_path: Path
    chinese_available: bool

    def regular(self, size: int) -> ImageFont.FreeTypeFont:
        return ImageFont.truetype(str(self.regular_path), size)

    def bold(self, size: int) -> ImageFont.FreeTypeFont:
        return ImageFont.truetype(str(self.bold_path), size)


def configure_plot_font() -> PlotFonts:
    candidates = [
        (Path("C:/Windows/Fonts/msyh.ttc"), Path("C:/Windows/Fonts/msyhbd.ttc")),
        (Path("C:/Windows/Fonts/msyh.ttf"), Path("C:/Windows/Fonts/msyhbd.ttf")),
        (Path("C:/Windows/Fonts/simhei.ttf"), Path("C:/Windows/Fonts/simhei.ttf")),
    ]
    for regular, bold in candidates:
        if regular.is_file():
            return PlotFonts(regular, bold if bold.is_file() else regular, True)

    fallback_candidates = [
        (Path("C:/Windows/Fonts/arial.ttf"), Path("C:/Windows/Fonts/arialbd.ttf")),
        (Path("C:/Windows/Fonts/segoeui.ttf"), Path("C:/Windows/Fonts/seguisb.ttf")),
    ]
    for regular, bold in fallback_candidates:
        if regular.is_file():
            return PlotFonts(regular, bold if bold.is_file() else regular, False)
    raise FileNotFoundError("No usable system font found for chart rendering")


def plot_labels(chinese_font_available: bool) -> dict[str, str]:
    return ARCHITECTURES if chinese_font_available else ARCHITECTURE_ENGLISH


def draw_centered_text(
    draw: ImageDraw.ImageDraw,
    xy: tuple[float, float],
    text: str,
    font: ImageFont.FreeTypeFont,
    fill: str = "#20242A",
) -> None:
    draw.text(xy, text, font=font, fill=fill, anchor="mm", align="center")


def draw_legend(
    draw: ImageDraw.ImageDraw,
    labels: dict[str, str],
    fonts: PlotFonts,
    y: int,
    width: int,
    architecture_ids: Sequence[str] = tuple(ARCHITECTURE_ORDER),
) -> None:
    font = fonts.regular(24)
    items: list[tuple[str, int]] = []
    for architecture_id in architecture_ids:
        label = labels[architecture_id]
        box = draw.textbbox((0, 0), label, font=font)
        items.append((architecture_id, box[2] - box[0] + 54))
    total_width = sum(item_width for _, item_width in items) + 24 * (len(items) - 1)
    x = max(30, (width - total_width) / 2)
    for architecture_id, item_width in items:
        color = COLORS[architecture_id]
        draw.line((x, y, x + 28, y), fill=color, width=5)
        draw.ellipse((x + 10, y - 5, x + 20, y + 5), fill=color)
        draw.text((x + 38, y), labels[architecture_id], font=font, fill="#30343B", anchor="lm")
        x += item_width + 24


def log_y(value: float, minimum: float, maximum: float, top: int, bottom: int) -> float:
    fraction = (math.log10(value) - math.log10(minimum)) / (
        math.log10(maximum) - math.log10(minimum)
    )
    return bottom - fraction * (bottom - top)


def linear_y(value: float, minimum: float, maximum: float, top: int, bottom: int) -> float:
    fraction = (value - minimum) / (maximum - minimum)
    return bottom - fraction * (bottom - top)


def draw_line_chart_axes(
    draw: ImageDraw.ImageDraw,
    fonts: PlotFonts,
    plot_box: tuple[int, int, int, int],
    y_ticks: Sequence[float],
    y_transform,
    x_label: str,
    y_label: str,
) -> dict[int, float]:
    left, top, right, bottom = plot_box
    axis_color = "#555B66"
    grid_color = "#D9DEE7"
    label_font = fonts.regular(23)
    tick_font = fonts.regular(21)
    draw.line((left, top, left, bottom), fill=axis_color, width=2)
    draw.line((left, bottom, right, bottom), fill=axis_color, width=2)
    for tick in y_ticks:
        y = y_transform(tick)
        draw.line((left, y, right, y), fill=grid_color, width=1)
        draw.text((left - 14, y), f"{tick:g}", font=tick_font, fill="#454B55", anchor="rm")
    x_positions = {
        n: left + index * (right - left) / (len(EXPECTED_N) - 1)
        for index, n in enumerate(EXPECTED_N)
    }
    for n, x in x_positions.items():
        draw.line((x, bottom, x, bottom + 8), fill=axis_color, width=2)
        draw.text((x, bottom + 18), str(n), font=tick_font, fill="#454B55", anchor="ma")
    draw_centered_text(draw, ((left + right) / 2, bottom + 72), x_label, label_font)
    draw.text((left, top - 18), y_label, font=label_font, fill="#30343B", anchor="ls")
    return x_positions


def plot_metric_comparison(
    output_path: Path,
    statistics_rows: Sequence[GroupStatistics],
    metric: str,
    fonts: PlotFonts,
) -> None:
    lookup = statistics_lookup(statistics_rows)
    labels = plot_labels(fonts.chinese_available)
    if metric == "mean":
        title = "五个实现的平均求解时间（对数纵轴）" if fonts.chinese_available else "Mean solve time (log scale)"
    else:
        title = "五个实现的中位数求解时间（对数纵轴）" if fonts.chinese_available else "Median solve time (log scale)"
    x_label = "棋盘规模 N" if fonts.chinese_available else "Board size N"
    y_label = "求解时间（ms，对数刻度）" if fonts.chinese_available else "Solve time (ms, log scale)"

    width, height = 1800, 1080
    image = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(image)
    draw_centered_text(draw, (width / 2, 65), title, fonts.bold(42))
    plot_box = (180, 180, 1690, 860)
    y_minimum, y_maximum = 1.5, 1000.0
    y_ticks = (2, 5, 10, 20, 50, 100, 200, 500, 1000)
    transform = lambda value: log_y(value, y_minimum, y_maximum, plot_box[1], plot_box[3])
    x_positions = draw_line_chart_axes(
        draw, fonts, plot_box, y_ticks, transform, x_label, y_label
    )
    for architecture_id in ARCHITECTURE_ORDER:
        values = [float(getattr(lookup[(architecture_id, n)], f"{metric}_ms")) for n in EXPECTED_N]
        points = [(x_positions[n], transform(value)) for n, value in zip(EXPECTED_N, values)]
        draw.line(points, fill=COLORS[architecture_id], width=5, joint="curve")
        for x, y in points:
            draw.ellipse((x - 7, y - 7, x + 7, y + 7), fill=COLORS[architecture_id], outline="white", width=2)
    draw_legend(draw, labels, fonts, 990, width)
    image.save(output_path, format="PNG", optimize=True)


def plot_runtime_growth(
    output_path: Path,
    statistics_rows: Sequence[GroupStatistics],
    fonts: PlotFonts,
) -> None:
    lookup = statistics_lookup(statistics_rows)
    labels = plot_labels(fonts.chinese_available)
    title = "中位数求解时间随 N 增长的变化" if fonts.chinese_available else "Median solve-time growth"
    overall_title = "全部实现" if fonts.chinese_available else "All implementations"
    detail_title = "非 MapReduce 实现细节" if fonts.chinese_available else "Detail excluding MapReduce"
    x_label = "棋盘规模 N" if fonts.chinese_available else "Board size N"
    y_label = "中位数求解时间（ms）" if fonts.chinese_available else "Median solve time (ms)"

    width, height = 2200, 1080
    image = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(image)
    draw_centered_text(draw, (width / 2, 58), title, fonts.bold(42))

    panels = [
        ((160, 190, 1060, 820), 800.0, tuple(ARCHITECTURE_ORDER), overall_title),
        ((1250, 190, 2150, 820), 35.0, tuple(ARCHITECTURE_ORDER[:-1]), detail_title),
    ]
    for plot_box, maximum, architecture_ids, subtitle in panels:
        left, top, right, bottom = plot_box
        draw_centered_text(draw, ((left + right) / 2, 140), subtitle, fonts.bold(30))
        step = 200 if maximum > 100 else 5
        y_ticks = tuple(float(value) for value in range(0, int(maximum) + 1, step))
        transform = lambda value, p=plot_box, m=maximum: linear_y(value, 0.0, m, p[1], p[3])
        x_positions = draw_line_chart_axes(
            draw, fonts, plot_box, y_ticks, transform, x_label, y_label
        )
        for architecture_id in architecture_ids:
            values = [float(lookup[(architecture_id, n)].median_ms) for n in EXPECTED_N]
            points = [(x_positions[n], transform(value)) for n, value in zip(EXPECTED_N, values)]
            draw.line(points, fill=COLORS[architecture_id], width=5, joint="curve")
            for x, y in points:
                draw.ellipse((x - 7, y - 7, x + 7, y + 7), fill=COLORS[architecture_id], outline="white", width=2)
    draw_legend(draw, labels, fonts, 985, width)
    image.save(output_path, format="PNG", optimize=True)


def plot_runtime_distribution(
    output_path: Path,
    records: Sequence[RunRecord],
    fonts: PlotFonts,
) -> None:
    title = "每个实现与 N 组合的 5 次求解时间分布" if fonts.chinese_available else "Five-run solve-time distributions"
    y_label = "求解时间（ms，对数刻度）" if fonts.chinese_available else "Solve time (ms, log scale)"
    short_labels = {
        "pipefilter": "管道" if fonts.chinese_available else "Pipe",
        "callreturn-iterative": "调用/返回\n迭代" if fonts.chinese_available else "C/R\nIter.",
        "callreturn-backtracking": "调用/返回\n回溯" if fonts.chinese_available else "C/R\nBacktrack",
        "blackboard": "黑板" if fonts.chinese_available else "Blackboard",
        "mapreduce": "MapReduce",
    }

    grouped: dict[tuple[str, int], list[float]] = defaultdict(list)
    for record in records:
        grouped[(record.architecture_id, record.n)].append(float(record.solve_elapsed_ms))

    width, height = 2400, 1080
    image = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(image)
    draw_centered_text(draw, (width / 2, 58), title, fonts.bold(42))
    y_minimum, y_maximum = 1.5, 1000.0
    y_ticks = (2, 5, 10, 20, 50, 100, 200, 500, 1000)
    jitter_pixels = (-22, -11, 0, 11, 22)
    panel_width = 720
    for panel_index, n in enumerate(EXPECTED_N):
        left = 115 + panel_index * 785
        top, right, bottom = 170, left + panel_width, 820
        draw_centered_text(draw, ((left + right) / 2, 130), f"N={n}", fonts.bold(30))
        draw.line((left, top, left, bottom), fill="#555B66", width=2)
        draw.line((left, bottom, right, bottom), fill="#555B66", width=2)
        transform = lambda value, t=top, b=bottom: log_y(value, y_minimum, y_maximum, t, b)
        for tick in y_ticks:
            y = transform(tick)
            draw.line((left, y, right, y), fill="#D9DEE7", width=1)
            draw.text((left - 12, y), f"{tick:g}", font=fonts.regular(19), fill="#454B55", anchor="rm")
        draw.text((left, top - 14), y_label, font=fonts.regular(19), fill="#30343B", anchor="ls")

        category_width = (right - left) / len(ARCHITECTURE_ORDER)
        for category_index, architecture_id in enumerate(ARCHITECTURE_ORDER):
            center_x = left + category_width * (category_index + 0.5)
            values = sorted(grouped[(architecture_id, n)])
            q1 = (values[0] + values[1]) / 2
            median = values[2]
            q3 = (values[3] + values[4]) / 2
            minimum, maximum = values[0], values[-1]
            color = COLORS[architecture_id]
            box_left, box_right = center_x - 34, center_x + 34
            draw.line((center_x, transform(minimum), center_x, transform(maximum)), fill="#777777", width=2)
            draw.line((center_x - 18, transform(minimum), center_x + 18, transform(minimum)), fill="#777777", width=2)
            draw.line((center_x - 18, transform(maximum), center_x + 18, transform(maximum)), fill="#777777", width=2)
            draw.rectangle((box_left, transform(q3), box_right, transform(q1)), fill="#F2F5F8", outline=color, width=3)
            draw.line((box_left, transform(median), box_right, transform(median)), fill="#20242A", width=3)
            for offset, value in zip(jitter_pixels, values):
                x, y = center_x + offset, transform(value)
                draw.ellipse((x - 6, y - 6, x + 6, y + 6), fill=color, outline="white", width=1)
            draw.multiline_text(
                (center_x, bottom + 24),
                short_labels[architecture_id],
                font=fonts.regular(18),
                fill="#30343B",
                anchor="ma",
                align="center",
                spacing=2,
            )
    note = "每个箱体叠加全部 5 个原始样本点" if fonts.chinese_available else "All five raw observations are shown"
    draw_centered_text(draw, (width / 2, 1010), note, fonts.regular(24), "#555B66")
    image.save(output_path, format="PNG", optimize=True)


def markdown_statistics_table(rows: Sequence[GroupStatistics]) -> str:
    lines = [
        "| 实现版本 | 平均值（ms） | 中位数（ms） | 最小值（ms） | 最大值（ms） | 样本标准差（ms） |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for row in rows:
        lines.append(
            f"| {row.architecture} | {display_ms(row.mean_ms)} | {display_ms(row.median_ms)} | "
            f"{display_ms(row.min_ms)} | {display_ms(row.max_ms)} | {display_ms(row.std_ms)} |"
        )
    return "\n".join(lines)


def write_analysis_markdown(
    path: Path,
    records: Sequence[RunRecord],
    statistics_rows: Sequence[GroupStatistics],
    environment_hash: str,
) -> None:
    lookup = statistics_lookup(statistics_rows)
    ratios = growth_ratios(statistics_rows)
    by_n = {
        n: [lookup[(architecture_id, n)] for architecture_id in ARCHITECTURE_ORDER]
        for n in EXPECTED_N
    }

    ratio_lines = [
        "| 实现版本 | N=8→10 | N=10→12 | N=8→12 |",
        "|---|---:|---:|---:|",
    ]
    for architecture_id in ARCHITECTURE_ORDER:
        values = ratios[architecture_id]
        ratio_lines.append(
            f"| {ARCHITECTURES[architecture_id]} | {display_ratio(values[0])}× | "
            f"{display_ratio(values[1])}× | {display_ratio(values[2])}× |"
        )

    text = f"""# 五种软件体系架构实验分析

## 1. 实验目的

本实验在统一 N 皇后问题、统一首解目标、统一搜索顺序和统一三个位向量剪枝规则下，比较五个软件体系架构实现版本的求解时间与增长现象。实验关注的是当前代码组织方式在当前实现和冷态采样条件下的表现，不把五个版本误称为五种不同算法，也不据此推导普遍的架构性能排名。

## 2. 实验对象

五个实现版本为：

1. 管道-过滤器；
2. 调用/返回（迭代）；
3. 调用/返回（回溯）；
4. 黑板；
5. MapReduce（单机模拟）。

其中，调用/返回（迭代）和调用/返回（回溯）属于同一调用/返回架构风格下的两个不同搜索实现版本。五个版本都逐行放置皇后、逐列枚举候选、使用列与两条对角线的三个 `long` 位向量剪枝，并返回相同顺序下的首解。

## 3. 实验环境

- 操作系统：Windows 11 家庭版中文版，10.0.26200，64 位；
- 处理器：13th Gen Intel Core i9-13900H；
- 物理内存：16,786,370,560 字节；
- Java/Javac：17.0.12；
- 编译参数：`--release 17 -encoding UTF-8`；
- 五份环境记录 SHA-256：`{environment_hash}`，内容完全一致。

## 4. 实验设计

实验规模为 5 个实现 × 3 个 N（8、10、12）× 每组 5 次，共 75 次。每次运行都启动独立 JVM，并且只调用一次求解器，没有预热。计时字段为 `solve_elapsed_ns`，由公共入口在 `solver.solve(n)` 调用前后使用 `System.nanoTime()` 采集。

该指标不包含 JVM 进程启动、求解器对象构造、结果校验和 JSON 输出时间。不过，每个 JVM 中的求解器都是第一次执行，仍可能受到类首次使用、JIT 编译、首次代码路径和内存分配等冷态因素影响。因此本文将其称为“独立 JVM 的首次、无预热核心求解耗时”，而不是端到端 JVM 冷启动耗时。

为了尽量比较架构组织方式，五个实现统一了问题定义、输入规模、首解目标、逐行/逐列搜索、三个位向量剪枝、测试环境和运行次数。该控制减少了算法差异的干扰，但也限制了各架构针对自身机制做特化优化。结果只反映当前统一实现条件下的权衡。

派生文件包括 [运行追踪索引](run_index.csv)、[原始运行明细](raw_run_details.csv)、[组合统计](detail_results.csv)、[架构汇总](summary_statistics.csv) 和 [中位数增长倍率](runtime_growth_ratio.csv)。`std_ms` 使用 5 次运行的样本标准差，即分母为 `n-1`。

## 5. 数据完整性与正确性验证

- 正式运行记录：{len(records)}；
- 实现版本：{len(ARCHITECTURES)}；
- N：8、10、12；
- 架构 × N 组合：{len(statistics_rows)}；
- 每组运行次数：5；
- 成功记录：{sum(record.success for record in records)}/{len(records)}；
- stderr 为空：{sum(record.stderr_empty for record in records)}/{len(records)}；
- 组内首解一致：15/15；
- 同一 N 跨实现首解一致：3/3；
- 五次正式编译均成功；五份环境记录字节级一致。

首解分别为：N=8 `[0,4,7,5,2,6,1,3]`，N=10 `[0,2,5,7,9,4,8,1,3,6]`，N=12 `[0,2,4,7,9,11,5,10,1,6,8,3]`。

## 6. 描述性统计结果

### 6.1 N=8

{markdown_statistics_table(by_n[8])}

N=8 时，调用/返回两个版本的中位数都约为 2 ms，处于相近水平。黑板中位数为 {display_ms(lookup[("blackboard", 8)].median_ms)} ms。MapReduce（单机模拟）为 {display_ms(lookup[("mapreduce", 8)].median_ms)} ms。管道-过滤器为 {display_ms(lookup[("pipefilter", 8)].median_ms)} ms。管道版本本组的最大值为 {display_ms(lookup[("pipefilter", 8)].max_ms)} ms，冷态调度和线程建立等固定成本在小规模问题上较明显。

### 6.2 N=10

{markdown_statistics_table(by_n[10])}

N=10 时，调用/返回两个版本仍处于约 2～3 ms 范围。黑板中位数为 {display_ms(lookup[("blackboard", 10)].median_ms)} ms。管道-过滤器为 {display_ms(lookup[("pipefilter", 10)].median_ms)} ms。MapReduce（单机模拟）增加到 {display_ms(lookup[("mapreduce", 10)].median_ms)} ms。小于数毫秒的版本间差异与冷态噪声处于相近量级，不宜据此判断绝对优劣。

### 6.3 N=12

{markdown_statistics_table(by_n[12])}

N=12 时，MapReduce（单机模拟）的中位数为 {display_ms(lookup[("mapreduce", 12)].median_ms)} ms，明显高于其他实现。该差异已经远大于数毫秒级计时噪声，值得从当前实现的 frontier 扩展、候选记录创建、分组和排序成本分析。结论必须限定在当前单机模拟、当前任务拆分粒度、当前实现方式和当前冷态实验条件下，不能推广为 MapReduce 架构在所有场景中一定最慢。

## 7. 五种实现的运行时间比较

### 7.1 平均值比较

![平均求解时间对比](mean_runtime_comparison.png)

平均值图使用对数纵轴，以便同时观察约 2 ms 的调用/返回版本和 N=12 时约 767 ms 的 MapReduce 版本。N=8 的管道-过滤器平均值受到较高运行点影响，达到 {display_ms(lookup[("pipefilter", 8)].mean_ms)} ms，而其中位数为 {display_ms(lookup[("pipefilter", 8)].median_ms)} ms。这说明只有 5 次样本时，平均值可能被单次冷态波动拉动。

### 7.2 中位数比较

![中位数求解时间对比](median_runtime_comparison.png)

中位数对单次高值更稳健。调用/返回（回溯）在三个 N 下的中位数分别为 {display_ms(lookup[("callreturn-backtracking", 8)].median_ms)}、{display_ms(lookup[("callreturn-backtracking", 10)].median_ms)} 和 {display_ms(lookup[("callreturn-backtracking", 12)].median_ms)} ms。由于本实验只求按固定顺序遇到的第一个解，首解搜索路径的长度不必随 N 单调增长，所以不能把该现象解释为完整 N 皇后搜索复杂度不增长。

### 7.3 运行波动情况

![运行时间分布](runtime_distribution.png)

分布图保留每个组合的全部 5 个样本点，并叠加箱线信息；纵轴使用对数刻度以覆盖多个数量级。N=8 的黑板和管道-过滤器波动相对更明显。调用/返回版本的绝对差值通常只有不足数毫秒。MapReduce N=12 的 5 次均处于数百毫秒量级，因此其高耗时不是单个异常点造成的。

## 8. 随问题规模增长的变化趋势

![运行时间增长趋势](runtime_growth.png)

中位数增长倍率如下：

{chr(10).join(ratio_lines)}

增长倍率是描述性指标，不是时间复杂度结论。N 只有 3 个取值，每组也只有 5 次。MapReduce（单机模拟）从 N=10 到 N=12 的中位数增长约 {display_ratio(ratios["mapreduce"][1])} 倍，反映当前逐层 frontier 在 N=12 下产生了明显更多的中间状态、候选记录和分组/排序工作。管道、黑板和调用/返回版本的中位数没有严格单调增长，固定开销、首解路径差异和冷态噪声都可能影响小规模结果。

## 9. 架构机制与实验现象分析

**管道-过滤器。** `ArchitectureSolver` 在每次 `solve` 中创建 5 条基于 `LinkedBlockingQueue` 的 Pipe 和 4 个工作线程。输入、候选生成、合法性校验、完整解收集与结果输出通过消息协议连接。搜索状态和消息进行防御性复制，未完成状态还要通过反馈 Pipe 返回生成器。该组织清晰分离处理阶段，但在线程创建、队列 put/take、线程调度、批次/控制消息以及状态传递上存在额外成本。对只求首解且单次工作量较小的 N=8～12，固定通信成本可能占较大比例。

**调用/返回（迭代）。** `ArchitectureSolver` 直接同步调用 `IterativeSearch`。搜索使用循环、`nextColumnByRow` 显式保存每行下一列，并在一个 `BoardState` 中维护皇后位置和三个位向量。调用路径直接，没有线程、队列或中间消息对象。回退由循环显式执行。

**调用/返回（回溯）。** `BacktrackingSearch.searchRow` 使用递归调用栈推进和返回，`BoardState` 负责放置、冲突检查与撤销。与迭代版本相比，它用语言调用栈保存控制位置，而不是显式游标数组。两者的搜索目标和剪枝相同，当前小规模冷态结果都在数毫秒范围，差异不足以支持普遍性能结论。

**黑板。** `BlackboardStorage` 集中保存候选、皇后位置、三个位向量、游标、状态和结果。`Controller` 依次触发候选、列约束、对角线约束和解判定知识源，并负责递归与回退。知识源不直接互调，只通过共享黑板读写。该组织把规则拆分为独立知识源，但每个候选需要多次控制器调度和共享状态字段读写。当前结果高于直接调用/返回版本，但仍处于约 5～8 ms 量级。

**MapReduce（单机模拟）。** 当前代码不是 Hadoop 或分布式实现，也没有并行 Mapper 池。`MapReduceDriver` 在单线程中逐层维护 frontier。Mapper 为每个父状态和每个候选列创建 `CandidateRecord`，包括三类冲突记录；Shuffler 用 `EnumMap` 真实分组；Reducer 复制 LEGAL 组、按 `order` 排序，并形成下一层 frontier 或首解。该实现为了体现 Map→Shuffle→Reduce 过程，会扩展整层状态并创建、分组和排序大量中间对象，而不是沿单条 DFS 路径尽快返回。N=12 时的明显增长与这些当前实现机制一致。

## 10. 实验误差与局限性

1. **每次运行使用独立 JVM。** 75 次记录分别启动 Java 进程，每个进程只执行一次求解。
2. **求解计时不包含 JVM 启动时间。** `solve_elapsed_ns` 只覆盖 `solver.solve(n)`，不能解释为完整程序从启动到退出的端到端耗时。
3. **仍存在冷态影响。** 首次求解可能受到类首次使用、JIT 编译、首次代码路径、内存分配、CPU 调度和系统后台任务影响。
4. **样本量有限。** 每个架构 × N 只有 5 次。平均值可能受单次高值影响，中位数相对稳健，但仍不足以进行强统计推断。
5. **小时间差不能过度解释。** 多个调用/返回结果集中在约 2～4 ms，差值与冷态噪声可能处于相近量级。不能因约 0.5 ms 差异就断言某架构显著优越。
6. **首解实验不代表完整搜索。** 固定搜索顺序下，首次遇到解之前的搜索路径会随 N 和首解位置变化，因此耗时不一定单调。
7. **统一策略既保证公平，也限制特化。** 统一搜索和剪枝减少算法差异，但可能限制某些架构采用更适合自身机制的并行、批处理或启发式优化。
8. **未记录全部运行时状态。** 环境文件一致，但没有持续记录后台负载、CPU 温度、频率和功耗策略。

## 11. 实验结论

数据完整性检查通过：75 次正式运行全部成功，stderr 为空，同一 N 的首解在所有实现和重复运行间一致。调用/返回两个版本在当前小规模冷态首解实验中处于相近的数毫秒范围。黑板增加了知识源调度和共享状态协作，当前耗时约为 5～8 ms。管道-过滤器承担线程、队列、消息和反馈环路的固定成本，当前约为 23～34 ms。MapReduce（单机模拟）在 N=12 下出现数百毫秒耗时，其当前逐层扩展、记录创建、分组与排序成本已经明显超过计时噪声。

上述结论只适用于本项目当前统一搜索策略、当前实现、N=8/10/12、每组 5 次和独立 JVM 首次求解条件。它说明了当前实现的架构组织开销和增长现象，不能推广为相应架构在所有 N 皇后程序或其他应用中的绝对性能优劣。
"""
    path.write_text(text, encoding="utf-8", newline="\n")


def main() -> None:
    root = project_root()
    output_directory = Path(__file__).resolve().parent
    records, environment_files = read_records(root)
    validate_dataset(records, environment_files)
    statistics_rows = calculate_statistics(records)

    write_run_index(output_directory / "run_index.csv", records)
    write_raw_run_details(output_directory / "raw_run_details.csv", records)
    write_detail_results(output_directory / "detail_results.csv", statistics_rows)
    write_summary_statistics(output_directory / "summary_statistics.csv", statistics_rows)
    write_growth_ratios(output_directory / "runtime_growth_ratio.csv", statistics_rows)

    fonts = configure_plot_font()
    plot_metric_comparison(
        output_directory / "mean_runtime_comparison.png",
        statistics_rows,
        "mean",
        fonts,
    )
    plot_metric_comparison(
        output_directory / "median_runtime_comparison.png",
        statistics_rows,
        "median",
        fonts,
    )
    plot_runtime_growth(
        output_directory / "runtime_growth.png",
        statistics_rows,
        fonts,
    )
    plot_runtime_distribution(
        output_directory / "runtime_distribution.png",
        records,
        fonts,
    )

    environment_hash = sha256(environment_files[0])
    write_analysis_markdown(
        output_directory / "analysis.md",
        records,
        statistics_rows,
        environment_hash,
    )

    print("Analysis complete")
    print(f"formal_runs={len(records)}")
    print(f"groups={len(statistics_rows)}")
    print(f"all_success={all(record.success for record in records)}")
    print(f"all_stderr_empty={all(record.stderr_empty for record in records)}")
    print(f"chinese_plot_font={fonts.chinese_available}")


if __name__ == "__main__":
    main()
