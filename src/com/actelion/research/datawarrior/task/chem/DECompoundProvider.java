package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.chem.*;
import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.task.chem.elib.TaskConstantsELib;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.clipboard.ClipboardHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class serves two purposes:
 * 1) If a CompoundCollectionPane is used to define a compound list relevant for a specific task,<br>
 *    and if it shall be possible to defer determining compound list members till the task is executed,<br>
 *    then this class may provide additional popup menu items for the CompoundCollectionPane, extended<br>
 *    task configuration handling, and deferred building of compound list according to the configuration.<br>
 * 2) Independent of a CompoundCollectionPane this class may build compound collections from file, clipboard,<br>
 *    random creation, table model columns, which can be requested at task design time or task execution time.
 */
public class DECompoundProvider {
	private static final String PROPERTY_DEFERRED_SOURCE = "deferred";
	private static final String DEFERRED_CLIPBOARD = "clipboard";
	private static final String DEFERRED_FILE = "file:";

	// random molecule generation constants
	private static final String RANDOM_MOLECULE_SEED = "eM@Hz@";
	private static final int FINAL_KEEP_SIZE_MUTATION_COUNT = 8;
	private static final int MAX_GROW_MUTATIONS = 128;	// to exit almost endless loops if highly preferred back&forth mutations suppress real changes

	public static final int SOURCE_CLIPBOARD = 1;
	public static final int SOURCE_FILE = 2;

	private CompoundCollectionPane mCompoundPane;
	private File  mFile;
	private boolean mIsClipboard;

	/**
	 * If a CompoundCollectionPane is used to define a compound list relevant for a specific task,
	 * and if a compound list may be determined at task runtime instead of task configuration time,
	 * then this class provides CompoundCollectionPane UI extentions and configuration management for this purpose.
	 * @param compoundPane
	 */
	public DECompoundProvider(CompoundCollectionPane compoundPane) {
		mCompoundPane = compoundPane;
		}

	public void addPopupItems(Component parent) {
		JMenu exeTimeMenu = new JMenu("At Execution Time");
		JCheckBoxMenuItem exeItemPaste = new JCheckBoxMenuItem("Read Structures From Clipboard");
		JCheckBoxMenuItem exeItemFile = new JCheckBoxMenuItem(mFile == null ? "Read Structures From File..." : "File: '" + mFile.getName() + "'");
		exeItemFile.addActionListener(e -> {
			if (exeItemFile.isSelected()) {
				File file = new FileHelper(parent).selectFileToOpen("Please select a compound file", CompoundFileHelper.cFileTypeCompoundFiles);
				if (file == null || !FileHelper.fileExists(file)) { // cancelled
					exeItemFile.setSelected(false);
					mFile = null;
				}
				else {
					mFile = file;
					exeItemPaste.setSelected(false);
					mIsClipboard = false;
					mCompoundPane.getModel().clear();
					mCompoundPane.setMessage("<At execution time will read compounds from file '" + file.getName() + "'>");
				}
			}
			else  {
				mCompoundPane.setMessage(null);
				mFile = null;
			}
		} );
		exeTimeMenu.add(exeItemFile);
		exeItemPaste.addActionListener(e -> {
			mIsClipboard = exeItemPaste.isSelected();
			if (mIsClipboard) {
				exeItemFile.setSelected(false);
				mFile = null;
				mCompoundPane.getModel().clear();
				mCompoundPane.setMessage("<At execution time will read compounds from clipboard>");
			}
			else {
				mCompoundPane.setMessage(null);
				mIsClipboard = false;
			}
		} );
		exeTimeMenu.add(exeItemPaste);
		mCompoundPane.addCustomPopupItem(exeTimeMenu);
	}

	/**
	 * @param configuration
	 * @return true if configuration was found that causes deferred compound reading
	 */
	public boolean getDialogConfiguration(Properties configuration) {
		if (mIsClipboard) {
			configuration.setProperty(PROPERTY_DEFERRED_SOURCE, DEFERRED_CLIPBOARD);
			return true;
		}
		if (mFile != null) {
			configuration.setProperty(PROPERTY_DEFERRED_SOURCE, DEFERRED_FILE+mFile.getPath());
			return true;
		}
		return false;
	}

	public boolean setDialogConfiguration(Properties configuration) {
		String deferred = configuration.getProperty(PROPERTY_DEFERRED_SOURCE);
		if (deferred != null) {
			if (deferred.equals(DEFERRED_CLIPBOARD)) {
				mIsClipboard = true;
			}
			else if (deferred.startsWith(DEFERRED_FILE)) {
				File file = new File(deferred.substring(DEFERRED_FILE.length()));
				if (file.exists() & file.canRead())
					mFile = file;
			}
			return true;
		}
		return false;
	}

	/**
	 * @param configuration
	 * @return null (if not deferred), [0][] (if defined as deferred, but no compounds found), or [n][2] idcodes and names
	 */
	public static ArrayList<StereoMolecule> getCompounds(Component parent, Properties configuration) {
		String deferred = configuration.getProperty(PROPERTY_DEFERRED_SOURCE);
		if (deferred != null) {
			if (deferred.equals(DEFERRED_CLIPBOARD))
				return getCompounds(parent, SOURCE_CLIPBOARD, null);
			if (deferred.startsWith(DEFERRED_FILE)) {
				return getCompounds(parent, SOURCE_FILE, deferred.substring(DEFERRED_FILE.length()));
			}
			return new ArrayList<>();
		}
		return null;
	}

	public static ArrayList<StereoMolecule> getCompounds(Component parent, int source, String info) {
		if (source == SOURCE_CLIPBOARD) {
			return new ClipboardHandler().pasteMolecules();
		}
		if (source == SOURCE_FILE) {
			File file = new File(info);
			if (file.exists() & file.canRead())
				return new FileHelper(parent).readStructuresFromFile(true);
		}
		return null;
	}

	public static void createRandomCompounds(ProgressController pc, int molCount, int kind, ConcurrentLinkedQueue<StereoMolecule> compounds) {
		final int MIN_ATOMS = 12;
		final int MAX_ATOMS = 18;
		final AtomicInteger remaining = new AtomicInteger(molCount);

		pc.startProgress("Generating random start set...", 0, molCount);

		final AtomTypeList atomTypeList = createAtomTypeList("/resources/"+ TaskConstantsELib.COMPOUND_KIND_FILE[kind]);
		int threadCount = Math.min(molCount, Runtime.getRuntime().availableProcessors());
		Thread[] thread = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			thread[i] = new Thread("Molecule Generator "+(i+1)) {
				public void run() {
					final Random random = new Random();
					Mutator mutator = new Mutator(atomTypeList);
					StereoMolecule mol = new StereoMolecule();

					int m = remaining.decrementAndGet();
					while (m >= 0 && !pc.threadMustDie()) {
						pc.updateProgress(molCount - m - 1);

						new IDCodeParser().parse(mol, RANDOM_MOLECULE_SEED);

						double randomValue = random.nextDouble();
						int targetAtomCount = MIN_ATOMS + (int)Math.round(randomValue * (MAX_ATOMS - MIN_ATOMS));

						for (int i=0; i<MAX_GROW_MUTATIONS && mol.getAllAtoms()<targetAtomCount; i++)
							if (null == mutator.mutate(mol, Mutator.MUTATION_GROW | Mutator.MUTATION_KEEP_SIZE, false))
								break;

						for (int i = 0; i< FINAL_KEEP_SIZE_MUTATION_COUNT; i++)
							if (null == mutator.mutate(mol, Mutator.MUTATION_KEEP_SIZE, false))
								break;

						compounds.add(new StereoMolecule(mol));
						m = remaining.decrementAndGet();
					}
				}
			};
		}

		for (Thread t:thread)
			t.start();

		for (Thread t:thread)
			try { t.join(); } catch (InterruptedException ie) {}
		}

	private static AtomTypeList createAtomTypeList(String fileName) {
		try {
			return new AtomTypeList(fileName, AtomTypeCalculator.cPropertiesForMutator);
			}
		catch (Exception e) {
			e.printStackTrace();
			return null;
			}
		}
	}
