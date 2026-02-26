import com.actelion.research.chem.*;
import com.actelion.research.chem.conf.MolecularFlexibilityCalculator;
import com.actelion.research.chem.descriptor.DescriptorHandlerLongFFP512;
import com.actelion.research.chem.prediction.*;
import com.actelion.research.chem.ugly.NastyFunctionDetector;
import com.actelion.research.chem.ugly.PainsDetector;

import java.io.*;
import java.util.*;

public class PropertyCalculatorCLI {

    private static final String[] PROPERTY_NAMES = {
        "Total_Molweight", "Molweight", "Monoisotopic_Mass",
        "cLogP", "cLogS",
        "H_Acceptors", "H_Donors",
        "Total_Surface_Area", "Relative_PSA", "Polar_Surface_Area",
        "Druglikeness",
        "Mutagenic", "Tumorigenic", "Reproductive_Effective", "Irritant",
        "Nasty_Functions", "PAINS",
        "Shape_Index", "Molecular_Flexibility", "Molecular_Complexity",
        "Fragments", "Non_H_Atoms", "Non_C_H_Atoms", "Metal_Atoms",
        "Electronegative_Atoms", "Stereo_Centers", "Aromatic_Atoms",
        "sp3_Carbon_Fraction", "sp3_Atoms", "Symmetric_Atoms",
        "Non_H_Bonds", "Rotatable_Bonds", "Ring_Closures",
        "Largest_Ring_Size", "Small_Rings", "Carbo_Rings", "Hetero_Rings",
        "Saturated_Rings", "Non_Aromatic_Rings", "Aromatic_Rings",
        "Saturated_Carbo_Rings", "Non_Aromatic_Carbo_Rings", "Carbo_Aromatic_Rings",
        "Saturated_Hetero_Rings", "Non_Aromatic_Hetero_Rings", "Hetero_Aromatic_Rings",
        "Amides", "Amines", "Alkyl_Amines", "Aromatic_Amines",
        "Aromatic_Nitrogens", "Basic_Nitrogens", "Acidic_Oxygens",
        "Stereo_Configuration"
    };

    private final CLogPPredictor logPPredictor;
    private final SolubilityPredictor logSPredictor;
    private final TotalSurfaceAreaPredictor surfacePredictor;
    private final DruglikenessPredictor druglikenessPredictor;
    private final ToxicityPredictor toxicityPredictor;
    private final NastyFunctionDetector nastyDetector;
    private final PainsDetector painsDetector;
    private final MolecularFlexibilityCalculator flexCalculator;
    private final DescriptorHandlerLongFFP512 ffpHandler;
    private final SmilesParser smilesParser;

    public PropertyCalculatorCLI() {
        logPPredictor = new CLogPPredictor();
        logSPredictor = new SolubilityPredictor();
        surfacePredictor = new TotalSurfaceAreaPredictor();
        druglikenessPredictor = new DruglikenessPredictor();
        toxicityPredictor = new ToxicityPredictor();
        nastyDetector = new NastyFunctionDetector();
        painsDetector = new PainsDetector();
        flexCalculator = new MolecularFlexibilityCalculator();
        ffpHandler = new DescriptorHandlerLongFFP512();
        smilesParser = new SmilesParser();
    }

    public String[] calculateProperties(StereoMolecule mol) {
        String[] results = new String[PROPERTY_NAMES.length];

        try {
            mol.ensureHelperArrays(Molecule.cHelperSymmetrySimple);

            long[] ffp = ffpHandler.createDescriptor(mol);

            // Molweight
            MolecularFormula mfFull = new MolecularFormula(mol);
            results[0] = fmt(mfFull.getRelativeWeight(), 6); // Total_Molweight

            StereoMolecule fragMol = new StereoMolecule(mol);
            fragMol.stripSmallFragments(true);
            fragMol.ensureHelperArrays(Molecule.cHelperSymmetrySimple);

            MolecularFormula mfFrag = new MolecularFormula(fragMol);
            results[1] = fmt(mfFrag.getRelativeWeight(), 6); // Molweight
            results[2] = fmt(mfFrag.getAbsoluteWeight(), 9); // Monoisotopic_Mass

            // cLogP, cLogS
            results[3] = fmt(logPPredictor.assessCLogP(fragMol));  // cLogP
            results[4] = fmt(logSPredictor.assessSolubility(fragMol)); // cLogS

            // H-Acceptors
            int acceptors = 0;
            for (int atom = 0; atom < fragMol.getAllAtoms(); atom++)
                if ((fragMol.getAtomicNo(atom) == 7 || fragMol.getAtomicNo(atom) == 8) && fragMol.getAtomCharge(atom) <= 0)
                    acceptors++;
            results[5] = String.valueOf(acceptors);

            // H-Donors
            int donors = 0;
            for (int atom = 0; atom < fragMol.getAllAtoms(); atom++)
                if ((fragMol.getAtomicNo(atom) == 7 || fragMol.getAtomicNo(atom) == 8) && fragMol.getAllHydrogens(atom) > 0)
                    donors++;
            results[6] = String.valueOf(donors);

            // Surface Area
            results[7] = fmt(surfacePredictor.assessTotalSurfaceArea(fragMol)); // Total_Surface_Area
            results[8] = fmt(surfacePredictor.assessRelativePolarSurfaceArea(fragMol)); // Relative_PSA
            results[9] = fmt(surfacePredictor.assessPSA(fragMol)); // TPSA

            // Druglikeness (no-index version, no fingerprint needed)
            double dl = druglikenessPredictor.assessDruglikeness(fragMol, null);
            results[10] = fmt(dl);

            // Toxicity
            results[11] = ToxicityPredictor.RISK_NAME[toxicityPredictor.assessRisk(fragMol, ToxicityPredictor.cRiskTypeMutagenic, null)];
            results[12] = ToxicityPredictor.RISK_NAME[toxicityPredictor.assessRisk(fragMol, ToxicityPredictor.cRiskTypeTumorigenic, null)];
            results[13] = ToxicityPredictor.RISK_NAME[toxicityPredictor.assessRisk(fragMol, ToxicityPredictor.cRiskTypeReproductiveEffective, null)];
            results[14] = ToxicityPredictor.RISK_NAME[toxicityPredictor.assessRisk(fragMol, ToxicityPredictor.cRiskTypeIrritant, null)];

            // Nasty Functions & PAINS
            long[] fragFFP = ffpHandler.createDescriptor(fragMol);
            results[15] = nastyDetector.getNastyFunctionString(fragMol, fragFFP);
            results[16] = painsDetector.getPainsString(fragMol, fragFFP);

            // Shape, Flexibility, Complexity
            results[17] = fmt(MolecularShapeCalculator.assessShape(fragMol));
            results[18] = fmt(flexCalculator.calculateMolecularFlexibility(fragMol));
            results[19] = fmt(FastMolecularComplexityCalculator.assessComplexity(fragMol));

            // Atom counts
            int[] fragNo = new int[mol.getAllAtoms()];
            int fragments = mol.getFragmentNumbers(fragNo, false, true);
            results[20] = String.valueOf(fragments);
            results[21] = String.valueOf(fragMol.getAtoms()); // Non-H atoms
            int nonCH = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++)
                if (fragMol.getAtomicNo(atom) != 6) nonCH++;
            results[22] = String.valueOf(nonCH);

            int metalAtoms = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++)
                if (fragMol.isMetalAtom(atom)) metalAtoms++;
            results[23] = String.valueOf(metalAtoms);

            int negAtoms = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++)
                if (fragMol.isElectronegative(atom)) negAtoms++;
            results[24] = String.valueOf(negAtoms);

            results[25] = String.valueOf(fragMol.getStereoCenterCount());

            int aromaticAtoms = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++)
                if (fragMol.isAromaticAtom(atom)) aromaticAtoms++;
            results[26] = String.valueOf(aromaticAtoms);

            // sp3 carbon fraction
            int sp3C = 0, totalC = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++) {
                if (fragMol.getAtomicNo(atom) == 6) {
                    totalC++;
                    if (fragMol.getAtomPi(atom) == 0 && fragMol.getAtomCharge(atom) <= 0)
                        sp3C++;
                }
            }
            results[27] = totalC > 0 ? fmt((double) sp3C / totalC) : "0";

            // sp3 atoms
            int sp3Atoms = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++)
                if ((fragMol.getAtomicNo(atom) == 6 && fragMol.getAtomPi(atom) == 0)
                        || (fragMol.getAtomicNo(atom) == 7 && !fragMol.isFlatNitrogen(atom))
                        || (fragMol.getAtomicNo(atom) == 8 && fragMol.getAtomPi(atom) == 0 && !fragMol.isAromaticAtom(atom))
                        || (fragMol.getAtomicNo(atom) == 15)
                        || (fragMol.getAtomicNo(atom) == 16 && !fragMol.isAromaticAtom(atom)))
                    sp3Atoms++;
            results[28] = String.valueOf(sp3Atoms);

            // Symmetric atoms
            int maxRank = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++)
                if (maxRank < fragMol.getSymmetryRank(atom))
                    maxRank = fragMol.getSymmetryRank(atom);
            results[29] = String.valueOf(fragMol.getAtoms() - maxRank);

            results[30] = String.valueOf(fragMol.getBonds()); // Non-H bonds
            results[31] = String.valueOf(fragMol.getRotatableBondCount());

            // Ring closures
            int[] fNo2 = new int[fragMol.getAllAtoms()];
            int frags2 = fragMol.getFragmentNumbers(fNo2, false, false);
            results[32] = String.valueOf(frags2 + fragMol.getAllBonds() - fragMol.getAllAtoms());

            // Ring counts
            RingCollection rc = fragMol.getRingSet();
            int largestRing = 0;
            for (int bond = 0; bond < fragMol.getBonds(); bond++)
                largestRing = Math.max(largestRing, fragMol.getBondRingSize(bond));
            results[33] = String.valueOf(largestRing);
            results[34] = String.valueOf(rc.getSize()); // small rings

            int carboRings = 0, heteroRings = 0;
            int satRings = 0, nonAromRings = 0, aromRings = 0;
            int carboSatRings = 0, carboNonAromRings = 0, carboAromRings = 0;
            int heteroSatRings = 0, heteroNonAromRings = 0, heteroAromRings = 0;

            for (int i = 0; i < rc.getSize(); i++) {
                boolean hasHetero = false;
                int[] ra = rc.getRingAtoms(i);
                for (int a : ra) {
                    if (fragMol.getAtomicNo(a) != 6) { hasHetero = true; break; }
                }

                boolean isArom = rc.isAromatic(i);
                boolean isSaturated = false;
                if (!isArom) {
                    isSaturated = true;
                    int[] rb = rc.getRingBonds(i);
                    for (int b : rb) {
                        if (fragMol.getBondOrder(b) > 1) { isSaturated = false; break; }
                    }
                }

                if (!hasHetero) carboRings++; else heteroRings++;

                if (isArom) {
                    aromRings++;
                    if (!hasHetero) carboAromRings++; else heteroAromRings++;
                } else {
                    nonAromRings++;
                    if (!hasHetero) carboNonAromRings++; else heteroNonAromRings++;
                    if (isSaturated) {
                        satRings++;
                        if (!hasHetero) carboSatRings++; else heteroSatRings++;
                    }
                }
            }
            results[35] = String.valueOf(carboRings);
            results[36] = String.valueOf(heteroRings);
            results[37] = String.valueOf(satRings);
            results[38] = String.valueOf(nonAromRings);
            results[39] = String.valueOf(aromRings);
            results[40] = String.valueOf(carboSatRings);
            results[41] = String.valueOf(carboNonAromRings);
            results[42] = String.valueOf(carboAromRings);
            results[43] = String.valueOf(heteroSatRings);
            results[44] = String.valueOf(heteroNonAromRings);
            results[45] = String.valueOf(heteroAromRings);

            // Functional groups
            int amides = 0, amines = 0, alkylAmines = 0, arylAmines = 0;
            int aromN = 0, basicN = 0, acidicO = 0;
            for (int atom = 0; atom < fragMol.getAtoms(); atom++) {
                if (AtomFunctionAnalyzer.isAmide(fragMol, atom)) amides++;
                if (AtomFunctionAnalyzer.isAmine(fragMol, atom)) amines++;
                if (AtomFunctionAnalyzer.isAlkylAmine(fragMol, atom)) alkylAmines++;
                if (AtomFunctionAnalyzer.isArylAmine(fragMol, atom)) arylAmines++;
                if (fragMol.getAtomicNo(atom) == 7 && fragMol.isAromaticAtom(atom)) aromN++;
                if (AtomFunctionAnalyzer.isBasicNitrogen(fragMol, atom)) basicN++;
                if (AtomFunctionAnalyzer.isAcidicOxygen(fragMol, atom)) acidicO++;
            }
            results[46] = String.valueOf(amides);
            results[47] = String.valueOf(amines);
            results[48] = String.valueOf(alkylAmines);
            results[49] = String.valueOf(arylAmines);
            results[50] = String.valueOf(aromN);
            results[51] = String.valueOf(basicN);
            results[52] = String.valueOf(acidicO);

            // Stereo configuration
            String chiralText = fragMol.getChiralText();
            results[53] = chiralText != null ? chiralText : "";

        } catch (Exception e) {
            for (int i = 0; i < results.length; i++)
                if (results[i] == null) results[i] = "ERROR";
        }

        return results;
    }

    private static String fmt(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "";
        long rounded = Math.round(value * 10000.0);
        if (rounded == (long) value * 10000)
            return String.valueOf((long) value);
        return String.format("%.4f", value);
    }

    private static String fmt(double value, int digits) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "";
        return String.format("%." + Math.max(0, digits - (int) Math.max(0, Math.log10(Math.abs(value)) + 1)) + "f", value);
    }

    private static String escapeCSV(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: PropertyCalculatorCLI <input.csv> [output.csv] [--smiles-col=SMILES]");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = null;
        String smilesColName = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--smiles-col=")) {
                smilesColName = args[i].substring("--smiles-col=".length());
            } else if (!args[i].startsWith("--")) {
                outputFile = args[i];
            }
        }

        PropertyCalculatorCLI calculator = new PropertyCalculatorCLI();
        SmilesParser parser = new SmilesParser();

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        PrintWriter writer;
        if (outputFile != null) {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
        } else {
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
        }

        String headerLine = reader.readLine();
        if (headerLine == null) {
            System.err.println("Empty input file");
            System.exit(1);
        }

        String[] headers = parseCSVLine(headerLine);
        int smilesCol = -1;

        if (smilesColName != null) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase(smilesColName)) {
                    smilesCol = i;
                    break;
                }
            }
        }

        if (smilesCol == -1) {
            String[] candidates = {"smiles", "smi", "canonical_smiles", "mol", "structure", "molecule"};
            for (String candidate : candidates) {
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].trim().equalsIgnoreCase(candidate)) {
                        smilesCol = i;
                        break;
                    }
                }
                if (smilesCol != -1) break;
            }
        }

        if (smilesCol == -1) {
            smilesCol = 0;
            System.err.println("Warning: No SMILES column identified, using first column");
        }

        StringBuilder outHeader = new StringBuilder();
        for (String h : headers) outHeader.append(escapeCSV(h.trim())).append(",");
        for (int i = 0; i < PROPERTY_NAMES.length; i++) {
            outHeader.append(PROPERTY_NAMES[i]);
            if (i < PROPERTY_NAMES.length - 1) outHeader.append(",");
        }
        writer.println(outHeader);

        String line;
        int lineNum = 1;
        int errorCount = 0;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            String[] fields = parseCSVLine(line);
            if (fields.length <= smilesCol) {
                System.err.println("Line " + lineNum + ": not enough columns, skipping");
                errorCount++;
                continue;
            }

            String smiles = fields[smilesCol].trim();
            StringBuilder row = new StringBuilder();
            for (String f : fields) row.append(escapeCSV(f)).append(",");

            if (smiles.isEmpty()) {
                for (int i = 0; i < PROPERTY_NAMES.length; i++) {
                    row.append("");
                    if (i < PROPERTY_NAMES.length - 1) row.append(",");
                }
            } else {
                try {
                    StereoMolecule mol = parser.parseMolecule(smiles);
                    if (mol == null || mol.getAllAtoms() == 0) throw new Exception("Empty molecule");
                    String[] props = calculator.calculateProperties(mol);
                    for (int i = 0; i < props.length; i++) {
                        row.append(escapeCSV(props[i] != null ? props[i] : ""));
                        if (i < props.length - 1) row.append(",");
                    }
                } catch (Exception e) {
                    System.err.println("Line " + lineNum + " (" + smiles + "): " + e.getMessage());
                    errorCount++;
                    for (int i = 0; i < PROPERTY_NAMES.length; i++) {
                        row.append("ERROR");
                        if (i < PROPERTY_NAMES.length - 1) row.append(",");
                    }
                }
            }
            writer.println(row);
        }

        reader.close();
        writer.flush();
        writer.close();

        if (errorCount > 0) {
            System.err.println("Completed with " + errorCount + " errors out of " + (lineNum - 1) + " rows");
        } else {
            System.err.println("Successfully processed " + (lineNum - 1) + " rows");
        }
    }

    private static String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
