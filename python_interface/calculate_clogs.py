#!/usr/bin/env python3
"""
Lightweight cLogS calculator using OpenChemLib's SolubilityPredictor.

Takes a CSV with a SMILES column, appends a cLogS column.
"""

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


def _find_java() -> str:
    java = shutil.which("java")
    if java is None:
        raise RuntimeError("Java not found on PATH. JDK 11+ is required.")
    return java


def _get_package_dir() -> Path:
    return Path(__file__).resolve().parent


def _get_jar_path() -> Path:
    pkg = _get_package_dir()
    jar = pkg / "build" / "clogs_calculator.jar"
    if not jar.exists():
        print("Java CLI not built yet. Building...", file=sys.stderr)
        result = subprocess.run(
            ["bash", str(pkg / "build.sh")], capture_output=True, text=True
        )
        if result.returncode != 0:
            raise RuntimeError(f"Build failed:\n{result.stderr}")
        if not jar.exists():
            raise FileNotFoundError(f"Build succeeded but jar not found: {jar}")
    return jar


def _get_classpath() -> str:
    pkg = _get_package_dir()
    lib_dir = pkg.parent / "lib"
    jar = _get_jar_path()
    return ":".join([str(jar), str(lib_dir / "openchemlib.jar")])


def calculate_clogs(
    input_csv: str,
    output_csv: str = None,
    smiles_column: str = None,
) -> str:
    """
    Calculate cLogS for molecules in a CSV file.

    Args:
        input_csv: Path to input CSV with a SMILES column.
        output_csv: Output path. Defaults to <input>_clogs.csv.
        smiles_column: Name of the SMILES column (auto-detected if None).

    Returns:
        Path to the output CSV.
    """
    input_path = Path(input_csv).resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input file not found: {input_csv}")

    if output_csv is None:
        output_csv = str(input_path.parent / f"{input_path.stem}_clogs{input_path.suffix}")
    output_path = Path(output_csv).resolve()

    cmd = [_find_java(), "-cp", _get_classpath(), "CLogSCalculator",
           str(input_path), str(output_path)]
    if smiles_column:
        cmd.append(f"--smiles-col={smiles_column}")

    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.stderr:
        for line in result.stderr.strip().split("\n"):
            print(f"[cLogS] {line}", file=sys.stderr)
    if result.returncode != 0:
        raise RuntimeError(f"cLogS calculation failed:\n{result.stderr}")

    return str(output_path)


def calculate_clogs_df(input_csv: str, smiles_column: str = None):
    """Calculate cLogS and return a pandas DataFrame."""
    import pandas as pd
    with tempfile.NamedTemporaryFile(suffix=".csv", delete=False) as tmp:
        tmp_path = tmp.name
    try:
        calculate_clogs(input_csv, tmp_path, smiles_column)
        return pd.read_csv(tmp_path)
    finally:
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)


def calculate_clogs_smiles(smiles_list: list):
    """Calculate cLogS for a list of SMILES. Returns a pandas DataFrame."""
    import pandas as pd
    with tempfile.NamedTemporaryFile(mode="w", suffix=".csv", delete=False) as tmp:
        tmp.write("smiles\n")
        for smi in smiles_list:
            tmp.write(f"{smi}\n")
        tmp_path = tmp.name
    try:
        return calculate_clogs_df(tmp_path, smiles_column="smiles")
    finally:
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)


def main():
    parser = argparse.ArgumentParser(description="Calculate cLogS from a CSV with SMILES.")
    parser.add_argument("input_csv", help="Input CSV file")
    parser.add_argument("-o", "--output", help="Output CSV (default: <input>_clogs.csv)")
    parser.add_argument("-s", "--smiles-column", help="SMILES column name (auto-detected)")
    args = parser.parse_args()

    try:
        output = calculate_clogs(args.input_csv, args.output, args.smiles_column)
        print(f"Output written to: {output}")
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
