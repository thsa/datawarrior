package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.form.JFXConformerPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;
import javafx.application.Platform;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public class DockingFitnessPanel extends FitnessPanel {
	private static final long serialVersionUID = 20211021L;

	protected static final String CODE_SKIP_PROTONATION = "protonate=false";

	protected JFXConformerPanel mConformerPanel;
	protected JCheckBox mCheckBoxProtonateLigand;

	/**
	 * Creates a new DockingFitnessPanel, which is configured according to the given configuration.
	 * @param owner
	 * @param configuration without leading fitness option type
	 */
	protected DockingFitnessPanel(Frame owner, UIDelegateELib delegate, String configuration) {
		this(owner, delegate);

		String[] param = configuration.split("\\t");
		int index = 0;

		boolean protonateLigand = true;
		if (param.length - index > 3 && param[0].equals(DockingFitnessPanel.CODE_SKIP_PROTONATION)) {
			protonateLigand = false;
			index++;
		}
		mCheckBoxProtonateLigand.setSelected(protonateLigand);

		if (param.length - index == 3) {
			mSlider.setValue(Integer.parseInt(param[index++]));

			StereoMolecule cavity = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(param[index++]);
			StereoMolecule ligand = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(param[index++]);
			if (cavity != null && ligand != null) {
				Platform.runLater(() -> {
					mConformerPanel.setProteinCavity(cavity, ligand, true);
					V3DMolecule ligand3D = new V3DMolecule(ligand, 0, V3DMolecule.MoleculeRole.LIGAND);
					ligand3D.setColor(javafx.scene.paint.Color.CORAL);
					mConformerPanel.getV3DScene().addMolecule(ligand3D);
				} );
			}
		}
	}

	protected DockingFitnessPanel(Frame owner, UIDelegateELib delegate) {
		super();

		EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
		settings.add(V3DScene.ViewerSettings.EDITING);
		mConformerPanel = new JFXConformerPanel(false, settings);
		mConformerPanel.setPopupMenuController(new DockingPanelController(mConformerPanel));
		mConformerPanel.setBackground(new java.awt.Color(24, 24, 96));
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(240), HiDPIHelper.scale(180)));
		mCheckBoxProtonateLigand = new JCheckBox("Use likely ligand protonation states", true);

		int gap = HiDPIHelper.scale(4);
		double[][] cpsize = {
				{gap, TableLayout.PREFERRED, TableLayout.PREFERRED, 4*gap, TableLayout.FILL, 4*gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, gap} };
		setLayout(new TableLayout(cpsize));

		add(new JLabel("Create molecules with high docking scores"), "1,1,2,1");
		add(mConformerPanel, "4,1,4,5");
		add(mSliderPanel, "1,3,2,3");
		add(mCheckBoxProtonateLigand, "1,5,2,5");
		add(createCloseButton(), "6,1");
	}

	/**
	 * returns the configuration string including the leading type code.
	 */
	@Override
	protected String getConfiguration() {
		StringBuilder sb = new StringBuilder(DOCKING_OPTION_CODE);
		sb.append('\t').append(mSlider.getValue());
		if (!mCheckBoxProtonateLigand.isSelected())
			sb.append('\t').append(CODE_SKIP_PROTONATION);

		List<StereoMolecule> protein = mConformerPanel.getMolecules(V3DMolecule.MoleculeRole.MACROMOLECULE);
		List<StereoMolecule> ligand = mConformerPanel.getMolecules(V3DMolecule.MoleculeRole.LIGAND);
		if (protein.size() == 1 && ligand.size() == 1) {
			Canonizer pc = new Canonizer(protein.get(0));
			sb.append('\t').append(pc.getIDCode()+" "+pc.getEncodedCoordinates());
			Canonizer lc = new Canonizer(ligand.get(0));
			sb.append('\t').append(lc.getIDCode()+" "+lc.getEncodedCoordinates());
		}
		return sb.toString();
	}
}
