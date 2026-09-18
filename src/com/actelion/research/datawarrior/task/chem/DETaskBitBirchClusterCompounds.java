package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO fix this AI created rubbish!!!

/**
 * DataWarrior task performing BitBIRCH-style clustering on binary fingerprints
 * already present in the current DataWarrior table.
 *
 * Input:
 *   A text column containing hexadecimal fingerprints.
 *
 * Output:
 *   A new integer column containing the cluster number for each row.
 *
 * Typical configuration:
 *   distance threshold = 0.30
 *   maximum cluster size = 100
 *   worker threads = available processors
 */
public class DETaskBitBirchClusterCompounds extends ConfigurableTask {

    public static final String TASK_NAME = "BitBIRCH Clustering";

    private static final String PROPERTY_FINGERPRINT_COLUMN =
            "fingerprintColumn";

    private static final String PROPERTY_DISTANCE_THRESHOLD =
            "distanceThreshold";

    private static final String PROPERTY_MAX_CLUSTER_SIZE =
            "maxClusterSize";

    private static final String PROPERTY_THREAD_COUNT =
            "threadCount";

    private static final String PROPERTY_RESULT_COLUMN =
            "resultColumn";

    private static final String DEFAULT_RESULT_COLUMN =
            "BitBIRCH Cluster";

    private final DEFrame parentFrame;
    private final CompoundTableModel tableModel;

    private JComboBox<String> fingerprintColumnComboBox;
    private JSpinner distanceThresholdSpinner;
    private JSpinner maxClusterSizeSpinner;
    private JSpinner threadCountSpinner;
    private javax.swing.JTextField resultColumnField;


    public DETaskBitBirchClusterCompounds(DEFrame parent) {
        super(parent, true);

        this.parentFrame = parent;
        this.tableModel = parent.getTableModel();
    }


    @Override
    public JPanel createDialogContent() {
        int columnCount = tableModel.getTotalColumnCount();

        String[] columnNames = new String[columnCount];

        for (int column = 0; column < columnCount; column++) {
            columnNames[column] = tableModel.getColumnTitle(column);
        }

        fingerprintColumnComboBox =
                new JComboBox<>(columnNames);

        distanceThresholdSpinner =
                new JSpinner(
                        new SpinnerNumberModel(
                                0.30,
                                0.0,
                                1.0,
                                0.01
                        )
                );

        maxClusterSizeSpinner =
                new JSpinner(
                        new SpinnerNumberModel(
                                100,
                                1,
                                1_000_000,
                                1
                        )
                );

        int availableProcessors =
                Runtime.getRuntime().availableProcessors();

        threadCountSpinner =
                new JSpinner(
                        new SpinnerNumberModel(
                                Math.max(1, availableProcessors),
                                1,
                                256,
                                1
                        )
                );

        resultColumnField =
                new javax.swing.JTextField(DEFAULT_RESULT_COLUMN, 20);

        double[][] tableSize = {
                {
                        8,
                        TableLayout.PREFERRED,
                        8,
                        TableLayout.FILL,
                        8
                },
                {
                        8,
                        TableLayout.PREFERRED,
                        8,
                        TableLayout.PREFERRED,
                        8,
                        TableLayout.PREFERRED,
                        8,
                        TableLayout.PREFERRED,
                        8,
                        TableLayout.PREFERRED,
                        8
                }
        };

        JPanel panel = new JPanel(new TableLayout(tableSize));

        panel.add(
                new JLabel("Fingerprint column:"),
                "1,1"
        );
        panel.add(
                fingerprintColumnComboBox,
                "3,1"
        );

        panel.add(
                new JLabel("Distance threshold:"),
                "1,3"
        );
        panel.add(
                distanceThresholdSpinner,
                "3,3"
        );

        panel.add(
                new JLabel("Maximum cluster size:"),
                "1,5"
        );
        panel.add(
                maxClusterSizeSpinner,
                "3,5"
        );

        panel.add(
                new JLabel("Worker threads:"),
                "1,7"
        );
        panel.add(
                threadCountSpinner,
                "3,7"
        );

        panel.add(
                new JLabel("Result column:"),
                "1,9"
        );
        panel.add(
                resultColumnField,
                "3,9"
        );

        return panel;
    }


    @Override
    public Properties getDialogConfiguration() {
        Properties configuration = new Properties();

        configuration.setProperty(
                PROPERTY_FINGERPRINT_COLUMN,
                Integer.toString(
                        fingerprintColumnComboBox.getSelectedIndex()
                )
        );

        configuration.setProperty(
                PROPERTY_DISTANCE_THRESHOLD,
                Double.toString(
                        ((Number) distanceThresholdSpinner.getValue())
                                .doubleValue()
                )
        );

        configuration.setProperty(
                PROPERTY_MAX_CLUSTER_SIZE,
                Integer.toString(
                        ((Number) maxClusterSizeSpinner.getValue())
                                .intValue()
                )
        );

        configuration.setProperty(
                PROPERTY_THREAD_COUNT,
                Integer.toString(
                        ((Number) threadCountSpinner.getValue())
                                .intValue()
                )
        );

        configuration.setProperty(
                PROPERTY_RESULT_COLUMN,
                resultColumnField.getText().trim()
        );

        return configuration;
    }


    @Override
    public void setDialogConfiguration(
            Properties configuration) {
        String columnString =
                configuration.getProperty(
                        PROPERTY_FINGERPRINT_COLUMN
                );

        if (columnString != null) {
            fingerprintColumnComboBox.setSelectedIndex(
                    Integer.parseInt(columnString)
            );
        }

        distanceThresholdSpinner.setValue(
                Double.parseDouble(
                        configuration.getProperty(
                                PROPERTY_DISTANCE_THRESHOLD,
                                "0.30"
                        )
                )
        );

        maxClusterSizeSpinner.setValue(
                Integer.parseInt(
                        configuration.getProperty(
                                PROPERTY_MAX_CLUSTER_SIZE,
                                "100"
                        )
                )
        );

        threadCountSpinner.setValue(
                Integer.parseInt(
                        configuration.getProperty(
                                PROPERTY_THREAD_COUNT,
                                Integer.toString(
                                        Runtime.getRuntime()
                                                .availableProcessors()
                                )
                        )
                )
        );

        resultColumnField.setText(
                configuration.getProperty(
                        PROPERTY_RESULT_COLUMN,
                        DEFAULT_RESULT_COLUMN
                )
        );
    }

    @Override
    public void setDialogConfigurationToDefault() {
        // TODO
    }


    @Override
    public Properties getPredefinedConfiguration() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;    // TODO
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
        try {
            int fingerprintColumn = Integer.parseInt(configuration.getProperty(PROPERTY_FINGERPRINT_COLUMN));

            if (fingerprintColumn < 0
                    || fingerprintColumn >= tableModel.getTotalColumnCount()) {
                showErrorMessage("Invalid fingerprint column.");
                return false;
            }

            double threshold =
                    Double.parseDouble(
                            configuration.getProperty(
                                    PROPERTY_DISTANCE_THRESHOLD
                            )
                    );

            if (threshold < 0.0 || threshold > 1.0) {
                showErrorMessage(
                        "Distance threshold must be between 0 and 1."
                );
                return false;
            }

            int maxClusterSize =
                    Integer.parseInt(
                            configuration.getProperty(
                                    PROPERTY_MAX_CLUSTER_SIZE
                            )
                    );

            if (maxClusterSize < 1) {
                showErrorMessage(
                        "Maximum cluster size must be positive."
                );
                return false;
            }

            int threadCount =
                    Integer.parseInt(
                            configuration.getProperty(
                                    PROPERTY_THREAD_COUNT
                            )
                    );

            if (threadCount < 1) {
                showErrorMessage(
                        "Thread count must be positive."
                );
                return false;
            }

            String resultColumn =
                    configuration.getProperty(
                            PROPERTY_RESULT_COLUMN
                    );

            if (resultColumn == null || resultColumn.isBlank()) {
                showErrorMessage(
                        "The result column name cannot be empty."
                );
                return false;
            }

            return true;
        } catch (Exception exception) {
            showErrorMessage(
                    "Invalid BitBIRCH configuration: "
                            + exception.getMessage()
            );

            return false;
        }
    }


    @Override
    public void runTask(Properties configuration) {
        int fingerprintColumn =
                Integer.parseInt(
                        configuration.getProperty(
                                PROPERTY_FINGERPRINT_COLUMN
                        )
                );

        double distanceThreshold =
                Double.parseDouble(
                        configuration.getProperty(
                                PROPERTY_DISTANCE_THRESHOLD
                        )
                );

        int maxClusterSize =
                Integer.parseInt(
                        configuration.getProperty(
                                PROPERTY_MAX_CLUSTER_SIZE
                        )
                );

        int threadCount =
                Integer.parseInt(
                        configuration.getProperty(
                                PROPERTY_THREAD_COUNT
                        )
                );

        String resultColumn =
                configuration.getProperty(
                        PROPERTY_RESULT_COLUMN
                );

        int rowCount = tableModel.getTotalRowCount();

        List<RowFingerprint> fingerprints =
                new ArrayList<>(rowCount);

        for (int row = 0; row < rowCount; row++) {
            long[] fp = (long[])tableModel.getTotalRecord(row).getData(fingerprintColumn);

            if (fp == null)
                continue;

            try {
                fingerprints.add(new RowFingerprint(row, fromFingerprint(fp)));
            } catch (IllegalArgumentException exception) {
                showErrorMessage("Invalid fingerprint in row "
                                + row
                                + ": "
                                + exception.getMessage()
                );
                return;
            }
        }

        if (fingerprints.isEmpty()) {
            showErrorMessage(
                    "No valid fingerprints were found."
            );
            return;
        }

        BitBirchModel model =
                new BitBirchModel(
                        distanceThreshold,
                        maxClusterSize
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        try {
            for (RowFingerprint fingerprint : fingerprints) {
                executor.submit(
                        () -> model.insert(fingerprint)
                );
            }
        } finally {
            executor.shutdown();
        }

        try {
            if (!executor.awaitTermination(
                    1,
                    TimeUnit.HOURS
            )) {
                executor.shutdownNow();

                showErrorMessage(
                        "BitBIRCH clustering timed out."
                );

                return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();

            showErrorMessage(
                    "BitBIRCH clustering was interrupted."
            );

            return;
        }

        /*
         * Create the output column.
         *
         * The exact addNewColumns()/setValueAt() signatures can differ
         * slightly between DataWarrior releases. These are the APIs used
         * by current CompoundTableModel implementations.
         */
        int resultColumnIndex;

        try {
            resultColumnIndex =
                    tableModel.addNewColumns(
                            new String[]{resultColumn}
                    );
        } catch (Exception exception) {
            showErrorMessage(
                    "Unable to create result column: "
                            + exception.getMessage()
            );
            return;
        }

        /*
         * Initialize all rows as empty.
         */
        for (int row = 0; row < rowCount; row++) {
            tableModel.setTotalValueAt(
                    "",
                    row,
                    resultColumnIndex
            );
        }

        /*
         * Write cluster numbers back into the original DataWarrior rows.
         */
        List<BitBirchCluster> clusters =
                model.getClusters();

        for (int clusterIndex = 0;
             clusterIndex < clusters.size();
             clusterIndex++) {

            BitBirchCluster cluster =
                    clusters.get(clusterIndex);

            int clusterNumber = clusterIndex + 1;

            for (RowFingerprint fingerprint :
                    cluster.getMembers()) {

                tableModel.setTotalValueAt(
                        Integer.toString(clusterNumber),
                        fingerprint.row,
                        resultColumnIndex
                );
            }
        }

        tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);
    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }


    private static final class RowFingerprint {
        final int row;
        final BitSet fingerprint;

        RowFingerprint(int row, BitSet fingerprint) {
            this.row = row;
            this.fingerprint =
                    (BitSet) fingerprint.clone();
        }
    }


    private static final class BitBirchCluster {
        private final BitSet unionFingerprint;
        private final List<RowFingerprint> members;

        BitBirchCluster(RowFingerprint first) {
            this.unionFingerprint =
                    (BitSet) first.fingerprint.clone();

            this.members =
                    new ArrayList<>();

            this.members.add(first);
        }

        void add(RowFingerprint fingerprint) {
            unionFingerprint.or(
                    fingerprint.fingerprint
            );

            members.add(fingerprint);
        }

        int size() {
            return members.size();
        }

        List<RowFingerprint> getMembers() {
            return members;
        }
    }


    /**
     * Thread-safe BIRCH-style microcluster model.
     */
    private static final class BitBirchModel {
        private final double distanceThreshold;
        private final int maxClusterSize;

        private final List<BitBirchCluster> clusters =
                new ArrayList<>();

        BitBirchModel(
                double distanceThreshold,
                int maxClusterSize) {
            this.distanceThreshold =
                    distanceThreshold;

            this.maxClusterSize =
                    maxClusterSize;
        }

        /*
         * A synchronized insertion is used because the cluster list and
         * cluster summaries are mutable.
         */
        synchronized void insert(RowFingerprint fingerprint) {
            BitBirchCluster nearest = null;
            double nearestDistance =
                    Double.POSITIVE_INFINITY;

            for (BitBirchCluster cluster : clusters) {
                if (cluster.size() >= maxClusterSize) {
                    continue;
                }

                double distance =
                        tanimotoDistance(
                                fingerprint.fingerprint,
                                cluster.unionFingerprint
                        );

                if (distance < nearestDistance) {
                    nearest = cluster;
                    nearestDistance = distance;
                }
            }

            if (nearest != null
                    && nearestDistance <= distanceThreshold) {
                nearest.add(fingerprint);
            } else {
                clusters.add(
                        new BitBirchCluster(fingerprint)
                );
            }
        }

        synchronized List<BitBirchCluster> getClusters() {
            return new ArrayList<>(clusters);
        }
    }


    /**
     * Binary Tanimoto distance:
     *
     * distance = 1 - intersection / union
     */
    private static double tanimotoDistance(
            BitSet first,
            BitSet second) {
        BitSet intersection =
                (BitSet) first.clone();

        intersection.and(second);

        BitSet union =
                (BitSet) first.clone();

        union.or(second);

        int unionSize =
                union.cardinality();

        if (unionSize == 0) {
            return 0.0;
        }

        return 1.0
                - intersection.cardinality()
                / (double) unionSize;
    }

    private static BitSet fromFingerprint(long[] fp) {
        BitSet result = new BitSet(fp.length * 64);

        int bitIndex = 0;

        for (long l : fp) {
            for (int i=0; i < 64; i++) {
                if ((l & (1L << i)) != 0)
                    result.set(bitIndex);

                bitIndex++;
            }
        }

        return result;
    }
}
