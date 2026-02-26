#!/usr/bin/env python3
"""
DataWarrior Chemical Properties Calculator - Python Interface

Calculates molecular properties using OpenChemLib (the engine behind DataWarrior).
Takes a CSV file with a SMILES column and outputs all computed properties.

Properties computed (54 total):
  Druglikeness:      Total Molweight, Molweight, Monoisotopic Mass, cLogP, cLogS,
                     H-Acceptors, H-Donors, Total Surface Area, Relative PSA,
                     Polar Surface Area (TPSA), Druglikeness
  Tox/Shape:         Mutagenic, Tumorigenic, Reproductive Effective, Irritant,
                     Nasty Functions, PAINS, Shape Index, Molecular Flexibility,
                     Molecular Complexity
  Atom Counts:       Fragments, Non-H Atoms, Non-C/H Atoms, Metal Atoms,
                     Electronegative Atoms, Stereo Centers, Aromatic Atoms,
                     sp3-Carbon Fraction, sp3-Atoms, Symmetric Atoms,
                     Non-H Bonds, Rotatable Bonds, Ring Closures
  Ring Counts:       Largest Ring Size, Small Rings, Carbo-Rings, Hetero-Rings,
                     Saturated Rings, Non-Aromatic Rings, Aromatic Rings, and more
  Functional Groups: Amides, Amines, Alkyl-Amines, Aromatic Amines,
                     Aromatic Nitrogens, Basic Nitrogens, Acidic Oxygens,
                     Stereo Configuration
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
        raise RuntimeError(
            "Java not found on PATH. JDK 11+ is required.\n"
            "Install with: sudo apt install default-jdk"
        )
    return java


def _get_package_dir() -> Path:
    return Path(__file__).resolve().parent


def _get_jar_path() -> Path:
    pkg = _get_package_dir()
    jar = pkg / "build" / "property_calculator.jar"
    if not jar.exists():
        print("Java CLI not built yet. Building...", file=sys.stderr)
        build_script = pkg / "build.sh"
        if not build_script.exists():
            raise FileNotFoundError(f"Build script not found: {build_script}")
        result = subprocess.run(
            ["bash", str(build_script)],
            capture_output=True, text=True
        )
        if result.returncode != 0:
            raise RuntimeError(f"Build failed:\n{result.stderr}")
        if not jar.exists():
            raise FileNotFoundError(f"Build succeeded but jar not found: {jar}")
    return jar


def _get_classpath() -> str:
    pkg = _get_package_dir()
    repo_root = pkg.parent
    lib_dir = repo_root / "lib"
    jar = _get_jar_path()

    cp_parts = [
        str(jar),
        str(lib_dir / "openchemlib.jar"),
        str(lib_dir / "molviewerlib.jar"),
    ]

    for part in cp_parts:
        if not Path(part).exists():
            raise FileNotFoundError(f"Required file not found: {part}")

    return ":".join(cp_parts)


def calculate_properties(
    input_csv: str,
    output_csv: str = None,
    smiles_column: str = None,
    java_path: str = None,
    java_opts: list = None,
) -> str:
    """
    Calculate chemical properties for molecules in a CSV file.

    Args:
        input_csv: Path to input CSV file containing a SMILES column.
        output_csv: Path to output CSV file. If None, a path is auto-generated
                    by appending '_properties' to the input filename.
        smiles_column: Name of the SMILES column. Auto-detected if None
                       (looks for 'smiles', 'smi', 'canonical_smiles', etc.).
        java_path: Path to java executable. Auto-detected if None.
        java_opts: Additional JVM options (e.g., ['-Xmx4g']).

    Returns:
        Path to the output CSV file.

    Raises:
        FileNotFoundError: If input file or required JARs are missing.
        RuntimeError: If Java is not installed or calculation fails.
    """
    input_path = Path(input_csv).resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input file not found: {input_csv}")

    if output_csv is None:
        output_csv = str(input_path.parent / f"{input_path.stem}_properties{input_path.suffix}")
    output_path = Path(output_csv).resolve()

    java = java_path or _find_java()
    classpath = _get_classpath()

    cmd = [java]
    if java_opts:
        cmd.extend(java_opts)
    cmd.extend(["-cp", classpath, "PropertyCalculatorCLI", str(input_path), str(output_path)])
    if smiles_column:
        cmd.append(f"--smiles-col={smiles_column}")

    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.stderr:
        for line in result.stderr.strip().split("\n"):
            print(f"[DataWarrior] {line}", file=sys.stderr)

    if result.returncode != 0:
        raise RuntimeError(
            f"Property calculation failed (exit code {result.returncode}):\n{result.stderr}"
        )

    return str(output_path)


def calculate_properties_df(
    input_csv: str,
    smiles_column: str = None,
    java_path: str = None,
    java_opts: list = None,
):
    """
    Calculate chemical properties and return as a pandas DataFrame.

    Same arguments as calculate_properties(), but returns a DataFrame
    instead of writing to a file.

    Returns:
        pandas.DataFrame with original columns plus all computed properties.
    """
    try:
        import pandas as pd
    except ImportError:
        raise ImportError(
            "pandas is required for calculate_properties_df(). "
            "Install with: pip install pandas"
        )

    with tempfile.NamedTemporaryFile(suffix=".csv", delete=False) as tmp:
        tmp_path = tmp.name

    try:
        calculate_properties(input_csv, tmp_path, smiles_column, java_path, java_opts)
        df = pd.read_csv(tmp_path)
        return df
    finally:
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)


def calculate_smiles_list(
    smiles_list: list,
    smiles_column: str = "smiles",
    java_path: str = None,
    java_opts: list = None,
):
    """
    Calculate chemical properties for a list of SMILES strings.

    Args:
        smiles_list: List of SMILES strings.
        smiles_column: Column name for SMILES in the temporary CSV.
        java_path: Path to java executable. Auto-detected if None.
        java_opts: Additional JVM options.

    Returns:
        pandas.DataFrame with SMILES and all computed properties.
    """
    try:
        import pandas as pd
    except ImportError:
        raise ImportError(
            "pandas is required for calculate_smiles_list(). "
            "Install with: pip install pandas"
        )

    with tempfile.NamedTemporaryFile(mode="w", suffix=".csv", delete=False) as tmp_in:
        tmp_in.write(f"{smiles_column}\n")
        for smi in smiles_list:
            tmp_in.write(f"{smi}\n")
        tmp_in_path = tmp_in.name

    try:
        df = calculate_properties_df(
            tmp_in_path,
            smiles_column=smiles_column,
            java_path=java_path,
            java_opts=java_opts,
        )
        return df
    finally:
        if os.path.exists(tmp_in_path):
            os.unlink(tmp_in_path)


def main():
    parser = argparse.ArgumentParser(
        description="Calculate chemical properties from a CSV file with SMILES.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("input_csv", help="Input CSV file with a SMILES column")
    parser.add_argument(
        "-o", "--output",
        help="Output CSV file (default: <input>_properties.csv)",
    )
    parser.add_argument(
        "-s", "--smiles-column",
        help="Name of the SMILES column (auto-detected if not specified)",
    )
    parser.add_argument(
        "--java-opts",
        nargs="*",
        default=[],
        help="Additional JVM options, e.g. --java-opts -Xmx4g",
    )

    args = parser.parse_args()

    try:
        output = calculate_properties(
            args.input_csv,
            args.output,
            args.smiles_column,
            java_opts=args.java_opts,
        )
        print(f"Output written to: {output}")
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
