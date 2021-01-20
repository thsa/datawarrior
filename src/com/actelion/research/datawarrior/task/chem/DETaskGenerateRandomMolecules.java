package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.*;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.chem.elib.TaskConstantsELib;
import com.actelion.research.datawarrior.task.chem.elib.UIDelegateELib;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskGenerateRandomMolecules extends ConfigurableTask implements ChangeListener {
	public static final String TASK_NAME = "Generate Random Molecules";

	private static final String PROPERTY_COMPOUND_KIND = "kind";
	private static final String PROPERTY_OXYGEN_BIAS = "oxygenBias";
	private static final String PROPERTY_NITROGEN_BIAS = "nitrogenBias";
	private static final String PROPERTY_SEED_FRAGMENT = "seed";
	private static final String PROPERTY_MIN_SIZE = "minSize";
	private static final String PROPERTY_MAX_SIZE = "maxSize";
	private static final String PROPERTY_SIZE_DISTRIBUTION = "distribution";
	private static final String PROPERTY_MOLECULE_COUNT = "count";

	private static final String[] TEXT_DISTRIBUTION = { "evenly from min to max", "with bias towards middle" };
	private static final String[] CODE_DISTRIBUTION = { "even", "middle" };
	private static final int DISTRIBUTION_EVEN = 0;
	private static final int DISTRIBUTION_MIDDLE = 1;
	private static final int DEFAULT_KIND = TaskConstantsELib.COMPOUND_KIND_DRUGS;

	private static final String BIAS_DESCRIPTOR = DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName;
	private static final String DEFAULT_SEED_FRAGMENT = "eM@Hz@";
	private static final int FINAL_KEEP_SIZE_MUTATION_COUNT = 8;
	private static final int MAX_GROW_MUTATIONS = 128;	// to exit almost endless loops if highly preferred back&forth mutations suppress real changes

	private DataWarrior			mApplication;
	private DEFrame				mTargetFrame;
	private JComboBox			mComboBoxCreateLike,mComboBoxSizeDistribution;
	private JSlider				mSliderOxygenBias,mSliderNitrogenBias;
	private JTextField			mTextFieldMinSize,mTextFieldMaxSize,mTextFieldCount;
	private JEditableStructureView	mSeedCompoundView;
	private JLabel				mLabelNitrogenBias,mLabelOxygenBias;

	public DETaskGenerateRandomMolecules(DEFrame parent) {
		super(parent, true);
		mApplication = parent.getApplication();
		}

		@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, HiDPIHelper.scale(48), gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, 2*gap,
				TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, HiDPIHelper.scale(64), 2*gap,
				TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap } };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Create compounds like", JLabel.RIGHT), "1,1");
		mComboBoxCreateLike = new JComboBox(UIDelegateELib.COMPOUND_KIND_TEXT);
		content.add(mComboBoxCreateLike, "3,1,5,1");

		mLabelNitrogenBias = new JLabel("1.0", JLabel.CENTER);
		mSliderNitrogenBias = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
		mSliderNitrogenBias.setPreferredSize(new Dimension(HiDPIHelper.scale(120), HiDPIHelper.scale(24)));
		mSliderNitrogenBias.setMinorTickSpacing(10);
		mSliderNitrogenBias.setMajorTickSpacing(50);
		mSliderNitrogenBias.setPaintTicks(true);
		mSliderNitrogenBias.addChangeListener(this);
		content.add(new JLabel("Nitrogen bias:", JLabel.RIGHT), "1,3");
		content.add(mLabelNitrogenBias, "3,3");
		content.add(mSliderNitrogenBias, "5,3");

		mLabelOxygenBias = new JLabel("1.0", JLabel.CENTER);
		mSliderOxygenBias = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
		mSliderOxygenBias.setPreferredSize(new Dimension(HiDPIHelper.scale(120), HiDPIHelper.scale(24)));
		mSliderOxygenBias.setMinorTickSpacing(10);
		mSliderOxygenBias.setMajorTickSpacing(50);
		mSliderOxygenBias.setPaintTicks(true);
		mSliderOxygenBias.addChangeListener(this);
		content.add(new JLabel("Oxygen bias:", JLabel.RIGHT), "1,5");
		content.add(mLabelOxygenBias, "3,5");
		content.add(mSliderOxygenBias, "5,5");

		content.add(new JLabel("Seed compound:", JLabel.RIGHT), "1,7");
		content.add(new JLabel("(Select immutable part)", JLabel.RIGHT), "1,9");
		mSeedCompoundView = new JEditableStructureView();
		content.add(mSeedCompoundView, "3,7,5,10");

		content.add(new JLabel("Molecule count:", JLabel.RIGHT), "1,12");
		mTextFieldCount = new JTextField();
		content.add(mTextFieldCount, "3,12");

		content.add(new JLabel("Minimum non-H atom count:", JLabel.RIGHT), "1,14");
		mTextFieldMinSize = new JTextField();
		content.add(mTextFieldMinSize, "3,14");

		content.add(new JLabel("Maximum non-H atom count:", JLabel.RIGHT), "1,16");
		mTextFieldMaxSize = new JTextField();
		content.add(mTextFieldMaxSize, "3,16");

		content.add(new JLabel("Molecule size distribution:", JLabel.RIGHT), "1,18");
		mComboBoxSizeDistribution = new JComboBox(TEXT_DISTRIBUTION);
		content.add(mComboBoxSizeDistribution, "3,18,5,18");

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#RandomMolecules";
		}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == mSliderNitrogenBias) {
			double f = Math.pow(10.0, (double)mSliderNitrogenBias.getValue() / 50.0f);
			mLabelNitrogenBias.setText(DoubleFormat.toString(f, 2));
			}
		if (e.getSource() == mSliderOxygenBias) {
			double f = Math.pow(10.0, (double)mSliderOxygenBias.getValue() / 50.0f);
			mLabelOxygenBias.setText(DoubleFormat.toString(f, 2));
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		try {
			int min = Integer.parseInt(configuration.getProperty(PROPERTY_MIN_SIZE,""));
			int max = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_SIZE,""));
			if (min < 6) {
				showErrorMessage("The minimum molecule size must not be less than six non-H atoms.");
				return false;
				}
			if (max > 256) {
				showErrorMessage("The maximum molecule size must not be more than 256 non-H atoms.");
				return false;
				}
			if (min >= max) {
				showErrorMessage("The maximum molecule size must be larger than the minimim size.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Minumum or maximum size is not numerical.");
			return false;
			}

		try {
			int count = Integer.parseInt(configuration.getProperty(PROPERTY_MOLECULE_COUNT,""));
			if (count < 1) {
				showErrorMessage("Molecule count must not be less than one.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Minumum or maximum size is not numerical.");
			return false;
			}

		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxCreateLike.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_COMPOUND_KIND), UIDelegateELib.COMPOUND_KIND_CODE, 0));
		mSliderNitrogenBias.setValue(Integer.parseInt(configuration.getProperty(PROPERTY_NITROGEN_BIAS, "0")));
		mSliderOxygenBias.setValue(Integer.parseInt(configuration.getProperty(PROPERTY_OXYGEN_BIAS, "0")));

		mTextFieldMinSize.setText(configuration.getProperty(PROPERTY_MIN_SIZE, ""));
		mTextFieldMaxSize.setText(configuration.getProperty(PROPERTY_MAX_SIZE, ""));
		mTextFieldCount.setText(configuration.getProperty(PROPERTY_MOLECULE_COUNT, ""));

		mComboBoxSizeDistribution.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SIZE_DISTRIBUTION), CODE_DISTRIBUTION, DISTRIBUTION_MIDDLE));

		String idcode = configuration.getProperty(PROPERTY_SEED_FRAGMENT, "");
		if (idcode.length() != 0) {
			int index = idcode.indexOf(" ");
			mSeedCompoundView.setIDCode(idcode.substring(0, index), idcode.substring(index+1));
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxCreateLike.setSelectedIndex(DEFAULT_KIND);
		mSliderOxygenBias.setValue(0);
		mSliderNitrogenBias.setValue(0);
		mTextFieldMinSize.setText("18");
		mTextFieldMaxSize.setText("36");
		mTextFieldCount.setText("1000");
		mComboBoxSizeDistribution.setSelectedIndex(DISTRIBUTION_MIDDLE);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		configuration.setProperty(PROPERTY_COMPOUND_KIND, UIDelegateELib.COMPOUND_KIND_CODE[mComboBoxCreateLike.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_NITROGEN_BIAS, Integer.toString(mSliderNitrogenBias.getValue()));
		configuration.setProperty(PROPERTY_OXYGEN_BIAS, Integer.toString(mSliderOxygenBias.getValue()));

		StereoMolecule mol = mSeedCompoundView.getMolecule();
		if (mol.getAllAtoms() != 0) {
			Canonizer canonizer = new Canonizer(mol, Canonizer.ENCODE_ATOM_SELECTION);
			configuration.setProperty(PROPERTY_SEED_FRAGMENT, canonizer.getIDCode()+" "+canonizer.getEncodedCoordinates());
			}

		configuration.setProperty(PROPERTY_MIN_SIZE, mTextFieldMinSize.getText());
		configuration.setProperty(PROPERTY_MAX_SIZE, mTextFieldMaxSize.getText());
		configuration.setProperty(PROPERTY_MOLECULE_COUNT, mTextFieldCount.getText());
		configuration.setProperty(PROPERTY_SIZE_DISTRIBUTION, CODE_DISTRIBUTION[mComboBoxSizeDistribution.getSelectedIndex()]);

		return configuration;
		}

	private AtomTypeList createAtomTypeList(String fileName) {
		try {
			return new AtomTypeList(fileName, AtomTypeCalculator.cPropertiesForMutator);
			}
		catch (Exception e) {
			e.printStackTrace();
			return null;
			}
		}

	public void runTask(Properties configuration) {
		int nitrogenBias = Integer.parseInt(configuration.getProperty(PROPERTY_NITROGEN_BIAS, "0"));
		int oxygenBias = Integer.parseInt(configuration.getProperty(PROPERTY_OXYGEN_BIAS, "0"));
		int minAtoms = Integer.parseInt(configuration.getProperty(PROPERTY_MIN_SIZE, "18"));
		int maxAtoms = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_SIZE, "36"));
		int distribution = findListIndex(configuration.getProperty(PROPERTY_SIZE_DISTRIBUTION), CODE_DISTRIBUTION, DISTRIBUTION_MIDDLE);
		int molCount = Integer.parseInt(configuration.getProperty(PROPERTY_MOLECULE_COUNT, "1000"));
		int kind = findListIndex(configuration.getProperty(PROPERTY_COMPOUND_KIND), UIDelegateELib.COMPOUND_KIND_CODE, DEFAULT_KIND);
		String seed = configuration.getProperty(PROPERTY_SEED_FRAGMENT, DEFAULT_SEED_FRAGMENT);

		final AtomTypeList atomTypeList = createAtomTypeList("/resources/"+TaskConstantsELib.COMPOUND_KIND_FILE[kind]);
		if (atomTypeList == null)
			return;

		final ConcurrentLinkedQueue<String> moleculeQueue = new ConcurrentLinkedQueue<String>();
		final AtomicInteger remaining = new AtomicInteger(molCount);

		startProgress("Generating molecules...", 0, molCount);

		int threadCount = Math.min(molCount, Runtime.getRuntime().availableProcessors());
		Thread[] thread = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			thread[i] = new Thread("Molecule Generator "+(i+1)) {
				public void run() {
					final Random random = new Random();
					Mutator mutator = new Mutator(atomTypeList);

					if (nitrogenBias != 0 || oxygenBias != 0)
						mutator.setBiasProvider(new AtomBiasProvider(Math.pow(10, (double)nitrogenBias/50), Math.pow(10, (double)oxygenBias/50)));

					StereoMolecule mol = new StereoMolecule();

					int m = remaining.decrementAndGet();
					while (m >= 0 && !threadMustDie()) {
						updateProgress(molCount - m - 1);

						new IDCodeParser().parse(mol, seed);

						double randomValue = random.nextDouble();
						if (distribution == DISTRIBUTION_MIDDLE) {
							// calculate value from -1 to 1
							final double BIAS_TOWARDS_CENTER_OF_MASS_OPTIMUM = 1.8;
							randomValue = 0.5 + 0.5 * (Math.pow(BIAS_TOWARDS_CENTER_OF_MASS_OPTIMUM, randomValue) - 1.0) / (BIAS_TOWARDS_CENTER_OF_MASS_OPTIMUM - 1.0);
							if (random.nextBoolean())
								randomValue = 1.0 - randomValue;
							}

						int targetAtomCount = minAtoms + (int)Math.round(randomValue * (maxAtoms - minAtoms));

						for (int i=0; i<MAX_GROW_MUTATIONS && mol.getAllAtoms()<targetAtomCount; i++)
							if (null == mutator.mutate(mol, Mutator.MUTATION_GROW | Mutator.MUTATION_KEEP_SIZE, false))
								break;

						for (int i = 0; i< FINAL_KEEP_SIZE_MUTATION_COUNT; i++)
							if (null == mutator.mutate(mol, Mutator.MUTATION_KEEP_SIZE, false))
								break;

						moleculeQueue.add(new Canonizer(mol).getIDCode());
						m = remaining.decrementAndGet();
						}
					}
				};
			}

		for (Thread t:thread)
			t.start();

		for (Thread t:thread)
			try { t.join(); } catch (InterruptedException ie) {}

		if (!moleculeQueue.isEmpty()) {
			mTargetFrame = mApplication.getEmptyFrame("Random Molecules");

			CompoundTableModel tableModel = mTargetFrame.getTableModel();
			tableModel.initializeTable(moleculeQueue.size(), 2);
			tableModel.prepareStructureColumns(0, "Structure", false, true);

			int count = moleculeQueue.size();
			for (int row=0; row<count; row++)
				tableModel.setTotalValueAt(moleculeQueue.poll(), row, 0);

			tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);
			}
		}
	}

class AtomBiasProvider implements MutationBiasProvider {
	private int mNitrogenCount,mOxygenCount;
	private double mNitrogenFactor,mOxygenFactor;

	public AtomBiasProvider(double nitrogenFactor, double oxygenFactor) {
		mNitrogenFactor = nitrogenFactor;
		mOxygenFactor = oxygenFactor;
		}

	@Override
	public void setBiasReference(StereoMolecule referenceMolecule) {
		mNitrogenCount = getAtomicNoCount(referenceMolecule, 7);
		mOxygenCount = getAtomicNoCount(referenceMolecule, 8);
		}

	@Override
	public double getBiasFactor(StereoMolecule mutatedMolecule) {
		int nitrogenCount = getAtomicNoCount(mutatedMolecule, 7);
		int oxygenCount = getAtomicNoCount(mutatedMolecule, 8);
		return Math.pow(mNitrogenFactor, nitrogenCount - mNitrogenCount) * Math.pow(mOxygenFactor, oxygenCount - mOxygenCount);
		}

	private int getAtomicNoCount(StereoMolecule mol, int atomicNo) {
		int count = 0;
		for (int atom=0; atom<mol.getAllAtoms(); atom++)
			if (mol.getAtomicNo(atom) == atomicNo)
				count++;

		return count;
		}
	}