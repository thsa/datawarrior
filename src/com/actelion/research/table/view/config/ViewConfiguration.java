package com.actelion.research.table.view.config;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;

import java.awt.*;

import static com.actelion.research.chem.io.CompoundTableConstants.cViewConfigTagName;

public abstract class ViewConfiguration<T extends CompoundTableView> extends AbstractConfiguration {
	public static final String cViewFontSize = "fontSize";
	public static final String cColorColumn = "colorColumn";
	private static final String cColorCount = "colorCount";
	private static final String cColor = "color";
	private static final String cDefaultColor = "defaultColor";
	private static final String cMissingColor = "missingColor";
	private static final String cColorMin = "colorMin";
	private static final String cColorMax = "colorMax";
	private static final String cColorListMode = "colorListMode";

	private CompoundTableModel mTableModel;

	public ViewConfiguration(CompoundTableModel tableModel) {
		super(cViewConfigTagName);
		mTableModel = tableModel;
		}

	public abstract void apply(T view);
	public abstract void learn(T view);
	public abstract String getViewType();

	public void applyViewColorProperties(String vColorName, VisualizationColor vColor) {
		try {
			String value = getProperty(cDefaultColor+vColorName);
			if (value != null)
				vColor.setDefaultDataColor(Color.decode(value));

			value = getProperty(cMissingColor+vColorName);
			if (value != null)
				vColor.setMissingDataColor(Color.decode(value));

			value = getProperty(cColorColumn + vColorName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;

				// to be compatible with format prior V2.7.0
				if (value.equals("colorBySimilarity"))
					column = mTableModel.findColumn(DescriptorConstants.DESCRIPTOR_FFP512.shortName);

				else if (value.startsWith("colorByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getListHandler().getListIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableListHandler.getColumnFromList(hitlistIndex);
				}
				else {
					column = mTableModel.findColumn(value);
				}

				if (column == JVisualization.cColumnUnassigned) {
					vColor.setColor(JVisualization.cColumnUnassigned);
				}
				else {
					Color[] colorList = null;
					value = getProperty(cColorCount + vColorName);
					if (value != null) {
						int colorCount = Integer.parseInt(value);
						colorList = new Color[colorCount];
						for (int j=0; j<colorCount; j++) {
							value = getProperty(cColor + vColorName + "_" + j);
							colorList[j] = Color.decode(value);
						}
					}

					value = getProperty(cColorListMode + vColorName);
					int colorListMode = VisualizationColor.cColorListModeStraight;	// default
					if (value != null) {
						if (value.equals("Categories"))
							colorListMode = VisualizationColor.cColorListModeCategories;
						else if (value.equals("HSBShort"))
							colorListMode = VisualizationColor.cColorListModeHSBShort;
						else if (value.equals("HSBLong"))
							colorListMode = VisualizationColor.cColorListModeHSBLong;
						else if (value.equals("straight"))
							colorListMode = VisualizationColor.cColorListModeStraight;
					}

					if (colorList == null) {	// cColorCount is only available if mode is cColorListModeCategory
						Color color1 = Color.decode(getProperty(cColor + vColorName + "_0"));
						Color color2 = Color.decode(getProperty(cColor + vColorName + "_1"));
						colorList = VisualizationColor.createColorWedge(color1, color2, colorListMode, null);
					}

					vColor.setColor(column, colorList, colorListMode);

					value = getProperty(cColorMin + vColorName);
					float min = (value == null) ? Float.NaN : Float.parseFloat(value);
					value = getProperty(cColorMax + vColorName);
					float max = (value == null) ? Float.NaN : Float.parseFloat(value);
					if (!Double.isNaN(min) || !Double.isNaN(max))
						vColor.setColorRange(min, max);
				}
			}
		}
		catch (Exception e) {
//				JOptionPane.showMessageDialog(mParentFrame, "Invalid color settings");
			}
		}

	public void learnViewColorProperties(String vColorName, VisualizationColor vColor) {
		if (!vColor.isDefaultDefaultDataColor())
			setProperty(cDefaultColor+vColorName, ""+vColor.getDefaultDataColor().getRGB());

		if (!vColor.isDefaultMissingDataColor())
			setProperty(cMissingColor+vColorName, ""+vColor.getMissingDataColor().getRGB());

		int column = vColor.getColorColumn();
		if (column != JVisualization.cColumnUnassigned) {
			String key = cColorColumn+vColorName;
			if (CompoundTableListHandler.isListColumn(column))
				setProperty(key, "colorByHitlist\t"
						+ mTableModel.getListHandler().getListName(
						CompoundTableListHandler.convertToListIndex(column)));
			else {
				setProperty(key, mTableModel.getColumnTitleNoAlias(column));
			}

			int mode = vColor.getColorListMode();
			key = cColorListMode+vColorName;
			if (mode == VisualizationColor.cColorListModeCategories)
				setProperty(key, "Categories");
			else if (mode == VisualizationColor.cColorListModeHSBShort)
				setProperty(key, "HSBShort");
			else if (mode == VisualizationColor.cColorListModeHSBLong)
				setProperty(key, "HSBLong");
			else if (mode == VisualizationColor.cColorListModeStraight)
				setProperty(key, "straight");

			Color[] colorList = vColor.getColorListWithoutDefaults();
			if (mode == VisualizationColor.cColorListModeCategories) {
				setProperty(cColorCount+vColorName, ""+colorList.length);
				for (int j=0; j<colorList.length; j++)
					setProperty(cColor+vColorName+"_"+j, ""+colorList[j].getRGB());
			}
			else {
				setProperty(cColor+vColorName+"_0", ""+colorList[0].getRGB());
				setProperty(cColor+vColorName+"_1", ""+colorList[colorList.length-1].getRGB());
			}

			if (!Double.isNaN(vColor.getColorMin()))
				setProperty(cColorMin+vColorName, ""+vColor.getColorMin());
			if (!Double.isNaN(vColor.getColorMax()))
				setProperty(cColorMax+vColorName, ""+vColor.getColorMax());
		}
	}
}
