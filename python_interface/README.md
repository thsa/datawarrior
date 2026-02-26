# DataWarrior Properties Calculator - Python Interface

A Python interface for calculating molecular properties using OpenChemLib, the
cheminformatics engine behind [DataWarrior](https://openmolecules.org/datawarrior).

Takes a CSV file with a SMILES column and outputs all computed chemical properties.

## Setup on a Fresh Linux Machine

### 1. Install system dependencies

```bash
# Ubuntu / Debian
sudo apt update
sudo apt install -y default-jdk python3 python3-pip git

# Verify
java -version    # needs 11+
python3 --version  # needs 3.8+
```

For other distros:
```bash
# Fedora / RHEL
sudo dnf install -y java-21-openjdk-devel python3 python3-pip git

# Arch
sudo pacman -S jdk-openjdk python python-pip git
```

### 2. Clone the repo

```bash
git clone https://github.com/KSUN63/datawarrior.git
cd datawarrior
```

### 3. Build the Java backend (one-time, ~1 second)

```bash
cd python_interface
bash build.sh
```

You should see:
```
Compiling PropertyCalculatorCLI...
Creating property_calculator.jar...
Build complete: .../python_interface/build/property_calculator.jar
```

### 4. Install Python dependencies

```bash
pip install pandas   # needed for DataFrame API
```

### 5. Test it

```bash
python3 calculate_properties.py test_molecules.csv -o results.csv
```

Or from Python:

```python
from calculate_properties import calculate_smiles_list

df = calculate_smiles_list(["CCO", "c1ccccc1", "CC(=O)Oc1ccccc1C(O)=O"])
print(df[["smiles", "cLogP", "cLogS", "Polar_Surface_Area"]])
```

That's it -- no DataWarrior GUI, no Maven/Gradle, no additional downloads.
The required JARs (`openchemlib.jar`, `molviewerlib.jar`) are already in the
repo under `lib/`.

## CLI Usage

```
python calculate_properties.py INPUT.csv [-o OUTPUT.csv] [-s SMILES_COLUMN]
```

| Argument | Description |
|---|---|
| `INPUT.csv` | Input CSV file with a SMILES column |
| `-o OUTPUT` | Output file path (default: `INPUT_properties.csv`) |
| `-s COLUMN` | Name of the SMILES column (auto-detected if not set) |
| `--java-opts` | JVM options, e.g. `--java-opts -Xmx4g` for large files |

The SMILES column is auto-detected by looking for columns named `smiles`, `smi`,
`canonical_smiles`, `mol`, `structure`, or `molecule` (case-insensitive). Falls
back to the first column if none match.

## Python API

### `calculate_properties(input_csv, output_csv=None, smiles_column=None)`

Calculates properties and writes to a CSV file. Returns the output file path.

### `calculate_properties_df(input_csv, smiles_column=None)`

Same as above but returns a `pandas.DataFrame`.

### `calculate_smiles_list(smiles_list)`

Takes a list of SMILES strings, returns a `pandas.DataFrame` with all properties.

## Computed Properties (54)

### Druglikeness Properties
| Property | Description |
|---|---|
| Total_Molweight | Total average molecular weight (g/mol) |
| Molweight | Molecular weight of largest fragment (g/mol) |
| Monoisotopic_Mass | Monoisotopic mass of largest fragment |
| cLogP | Calculated octanol/water partition coefficient |
| cLogS | Calculated aqueous solubility (log mol/l, pH 7.5, 25°C) |
| H_Acceptors | Hydrogen bond acceptor count |
| H_Donors | Hydrogen bond donor count |
| Total_Surface_Area | Total SAS approximation (van der Waals, 1.4Å probe) |
| Relative_PSA | Relative polar surface area |
| Polar_Surface_Area | Topological PSA (Ertl method) |
| Druglikeness | Druglikeness score |

### Toxicity & Shape
| Property | Description |
|---|---|
| Mutagenic | Mutagenicity risk (none/low/high) |
| Tumorigenic | Tumorigenicity risk (none/low/high) |
| Reproductive_Effective | Reproductive toxicity risk (none/low/high) |
| Irritant | Irritant risk (none/low/high) |
| Nasty_Functions | Detected nasty functional groups |
| PAINS | Detected PAINS patterns |
| Shape_Index | Molecular shape (spherical < 0.5 < linear) |
| Molecular_Flexibility | Flexibility (low < 0.5 < high) |
| Molecular_Complexity | Complexity (low < 0.5 < high) |

### Atom & Bond Counts
| Property | Description |
|---|---|
| Fragments | Disconnected fragment count |
| Non_H_Atoms | Heavy atom count |
| Non_C_H_Atoms | Non-carbon heavy atom count |
| Metal_Atoms | Metal atom count |
| Electronegative_Atoms | Electronegative atom count (N, O, P, S, halogens) |
| Stereo_Centers | Stereocenter count |
| Aromatic_Atoms | Aromatic atom count |
| sp3_Carbon_Fraction | sp3-C / total C |
| sp3_Atoms | sp3 atom count (C, N, O, P, S) |
| Symmetric_Atoms | Symmetric atom count |
| Non_H_Bonds | Heavy bond count |
| Rotatable_Bonds | Rotatable bond count |
| Ring_Closures | Ring closure count |

### Ring Counts
| Property | Description |
|---|---|
| Largest_Ring_Size | Size of the largest ring |
| Small_Rings | Small ring count (≤7 members) |
| Carbo_Rings / Hetero_Rings | Carbon-only vs. heteroatom rings |
| Saturated_Rings | Fully saturated ring count |
| Non_Aromatic_Rings / Aromatic_Rings | By aromaticity |
| *(and all cross-combinations)* | Saturated/non-aromatic × carbo/hetero |

### Functional Groups
| Property | Description |
|---|---|
| Amides | Amide nitrogen count |
| Amines | Amine count |
| Alkyl_Amines / Aromatic_Amines | By environment |
| Aromatic_Nitrogens | Aromatic nitrogen count |
| Basic_Nitrogens | Basic nitrogen count (est. pKa > 7) |
| Acidic_Oxygens | Acidic oxygen count (est. pKa < 7) |
| Stereo_Configuration | Stereo description (e.g., "racemate") |

## How It Works

1. The Python interface writes input to a temporary CSV if needed
2. Calls a lightweight Java CLI (`PropertyCalculatorCLI`) that uses OpenChemLib
3. OpenChemLib parses SMILES and computes all properties
4. Results are returned as CSV or pandas DataFrame

No DataWarrior GUI is needed. The calculation engine runs headless.

## License

Same as DataWarrior - GNU General Public License v3.
