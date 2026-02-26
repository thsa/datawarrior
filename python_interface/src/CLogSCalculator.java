import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.prediction.SolubilityPredictor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CLogSCalculator {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: CLogSCalculator <input.csv> [output.csv] [--smiles-col=SMILES]");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = null;
        String smilesColName = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--smiles-col="))
                smilesColName = args[i].substring("--smiles-col=".length());
            else if (!args[i].startsWith("--"))
                outputFile = args[i];
        }

        SmilesParser parser = new SmilesParser();
        SolubilityPredictor predictor = new SolubilityPredictor();

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        PrintWriter writer = outputFile != null
                ? new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))
                : new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));

        String headerLine = reader.readLine();
        if (headerLine == null) { System.err.println("Empty input file"); System.exit(1); }

        String[] headers = parseCSVLine(headerLine);
        int smilesCol = findSmilesColumn(headers, smilesColName);

        StringBuilder outHeader = new StringBuilder();
        for (String h : headers) outHeader.append(escapeCSV(h.trim())).append(",");
        outHeader.append("cLogS");
        writer.println(outHeader);

        String line;
        int lineNum = 1, errorCount = 0;
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
                row.append("");
            } else {
                try {
                    StereoMolecule mol = parser.parseMolecule(smiles);
                    if (mol == null || mol.getAllAtoms() == 0) throw new Exception("Empty molecule");
                    mol.stripSmallFragments(true);
                    double logS = predictor.assessSolubility(mol);
                    row.append(Double.isNaN(logS) ? "" : String.format("%.4f", logS));
                } catch (Exception e) {
                    System.err.println("Line " + lineNum + " (" + smiles + "): " + e.getMessage());
                    errorCount++;
                    row.append("ERROR");
                }
            }
            writer.println(row);
        }

        reader.close();
        writer.flush();
        writer.close();

        if (errorCount > 0)
            System.err.println("Completed with " + errorCount + " errors out of " + (lineNum - 1) + " rows");
        else
            System.err.println("Successfully processed " + (lineNum - 1) + " rows");
    }

    private static int findSmilesColumn(String[] headers, String smilesColName) {
        if (smilesColName != null)
            for (int i = 0; i < headers.length; i++)
                if (headers[i].trim().equalsIgnoreCase(smilesColName)) return i;

        String[] candidates = {"smiles", "smi", "canonical_smiles", "mol", "structure", "molecule"};
        for (String c : candidates)
            for (int i = 0; i < headers.length; i++)
                if (headers[i].trim().equalsIgnoreCase(c)) return i;

        System.err.println("Warning: No SMILES column identified, using first column");
        return 0;
    }

    private static String escapeCSV(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }

    private static String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; }
                    else inQuotes = false;
                } else current.append(c);
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { fields.add(current.toString()); current = new StringBuilder(); }
                else current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
