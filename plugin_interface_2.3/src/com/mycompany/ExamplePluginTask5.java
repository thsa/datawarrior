package com.mycompany;

import info.clearthought.layout.TableLayout;
import org.openmolecules.datawarrior.plugin.IConformerPanel;
import org.openmolecules.datawarrior.plugin.IPluginHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;
import org.openmolecules.datawarrior.plugin.IUserInterfaceHelper;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.Properties;

/**
 * PluginTask with a dialog that embeds a structure editor.
 */
public class ExamplePluginTask5 implements IPluginTask {
	private static final String CONFIGURATION_QUERY_TYPE = "queryType";
	private static final String CONFIGURATION_QUERY_STRUCTURE = "queryStructure";
	private static final String CONFIGURATION_SIMILARITY_LIMIT = "similarityLimit";

	private static final String[] SEARCH_TYPE_OPTIONS = {"PheSA Superposition", "Flexophore Matching"};
	private static final String[] QUERY_TYPE_CODE = {"phesa", "flexophore"};
	private static final int QUERY_TYPE_PHESA = 0;
	private static final int QUERY_TYPE_FLEXOPHORE = 1;

	private JComboBox<String> mComboBox;
	private IConformerPanel mConformerPanel;
	private JSlider mSimilaritySlider;

	@Override
	public String getTaskCode() {
		return "OpenMoleculesExample005";
	}

	@Override
	public String getTaskName() {
		return "Plugin Example: Virtual Screening";
	}

	/**
	 * This method expects a JPanel with all UI-elements for defining a database query.
	 * These may include elements to define a structure search and/or alphanumerical
	 * search criteria. 'Cancel' and 'OK' buttons are provided outside of this panel.
	 * @param dialogHelper gives access to a chemistry panel to let the user draw a chemical (sub-)structure
	 * @return
	 */
	@Override
	public JComponent createDialogContent(IUserInterfaceHelper dialogHelper) {
		mComboBox = new JComboBox<>(SEARCH_TYPE_OPTIONS);
		mComboBox.setSelectedIndex(0);

		JPanel panel1 = new JPanel();
		panel1.add(new JLabel("Search type:"));
		panel1.add(mComboBox);

		mConformerPanel = dialogHelper.getConformerEditor(IConformerPanel.MODE_CONFORMER);
		mConformerPanel.setConformerFromIDCode(QUERY_CONFORMER);

		int gap = (int)(dialogHelper.getUIScaleFactor() * 8);
		double[][] size = { { gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap },
							{ gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap } };
		JPanel panel = new JPanel();
		panel.setLayout(new TableLayout(size));
		panel.add(panel1, "1,1,3,1");
		panel.add(createSimilaritySlider(dialogHelper, ((JComponent)mConformerPanel).getPreferredSize().height), "1,3");
		panel.add((JComponent)mConformerPanel, "3,3");

		return panel;
	}

	private JComponent createSimilaritySlider(IUserInterfaceHelper dialogHelper, int height) {
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(70, new JLabel("70%"));
		labels.put(80, new JLabel("80%"));
		labels.put(90, new JLabel("90%"));
		labels.put(100, new JLabel("100%"));
		mSimilaritySlider = new JSlider(JSlider.VERTICAL, 70, 100, 85);
		mSimilaritySlider.setMinorTickSpacing(1);
		mSimilaritySlider.setMajorTickSpacing(10);
		mSimilaritySlider.setLabelTable(labels);
		mSimilaritySlider.setPaintLabels(true);
		mSimilaritySlider.setPaintTicks(true);
		int width = mSimilaritySlider.getPreferredSize().width;
		mSimilaritySlider.setMinimumSize(new Dimension(width, height));
		mSimilaritySlider.setPreferredSize(new Dimension(width, height));
		mSimilaritySlider.setEnabled(false);
		JPanel spanel = new JPanel();
		spanel.add(mSimilaritySlider);
		spanel.setBorder(BorderFactory.createTitledBorder("Similarity"));
		return spanel;
	}

	/**
	 * This method is called after the users presses the dialog's 'OK' button.
	 * At this time the dialog is still shown. This method expects a Properties
	 * object containing all UI-elements' states converted into key-value pairs
	 * describing the user defined database query. This query configuration is
	 * used later for two purposes:<br>
	 * - to run the query independent of the actual dialog<br>
	 * - to populate a dialog with a query that has been performed earlier<br>
	 * @return query configuration
	 */
	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(CONFIGURATION_QUERY_TYPE, QUERY_TYPE_CODE[mComboBox.getSelectedIndex()]);
		String idcode = mConformerPanel.getStructure(IConformerPanel.ROLE_CONFORMER, IConformerPanel.FORMAT_IDCODE);
		if (idcode != null)
			configuration.setProperty(CONFIGURATION_QUERY_STRUCTURE, idcode);
		if (mComboBox.getSelectedIndex() == QUERY_TYPE_FLEXOPHORE)
			configuration.setProperty(CONFIGURATION_SIMILARITY_LIMIT, Integer.toString(mSimilaritySlider.getValue()));
		return configuration;
	}

	/**
	 * This method populates an empty database query dialog with a previously configured database query.
	 * @param configuration
	 */
	@Override
	public void setDialogConfiguration(Properties configuration) {
		boolean isSubstructureQuery = QUERY_TYPE_CODE[0].equals(configuration.getProperty(CONFIGURATION_QUERY_TYPE));
		mComboBox.setSelectedIndex(isSubstructureQuery ? QUERY_TYPE_PHESA : QUERY_TYPE_FLEXOPHORE);
		String idcode = configuration.getProperty(CONFIGURATION_QUERY_STRUCTURE);
		if (idcode != null)
			mConformerPanel.setConformerFromIDCode(idcode);
//		mConformerPanel.setMode(isSubstructureQuery ? IChemistryPanel.MODE_FRAGMENT : IChemistryPanel.MODE_MOLECULE);
		mSimilaritySlider.setEnabled(!isSubstructureQuery);
		if (!isSubstructureQuery)
			mSimilaritySlider.setValue(Integer.parseInt(configuration.getProperty(CONFIGURATION_SIMILARITY_LIMIT, "85")));

	}

	/**
	 * Checks, whether the given database query configuration is a valid one.
	 * If not, then this method should return a short and clear error message
	 * intended for the user in order to correct the dialog setting.
	 * @param configuration
	 * @return user-interpretable error message or null, if query configuration is valid
	 */
	@Override
	public String checkConfiguration(Properties configuration) {
		if (configuration.getProperty(CONFIGURATION_QUERY_STRUCTURE) == null) {
			boolean isSubstructureQuery = QUERY_TYPE_CODE[0].equals(configuration.getProperty(CONFIGURATION_QUERY_TYPE));
			return "You need to draw a " + (isSubstructureQuery ? "sub-structure." : "molecule.");
		}
		return null;    // perfectly defined query
	}

	/**
	 * This method performes the database query. Typically, it reads the query configuration from
	 * the given Properties object, sends it to a database server, retrieves a result and populates
	 * a new window's table with the retrieved data. The passed IPluginHelper object provides
	 * all needed methods to create a new DataWarrior window, to allocate result columns,
	 * to populate these columns with chemical and alphanumerical content, and to show an error
	 * message if something goes wrong.<br>
	 * The query definition must be taken from the passed configuration object and NOT from
	 * any UI-elements of the dialog. The concept is to completely separate query definition
	 * from query execution, which is the basis for DataWarrior's macros to work.
	 * If an error occurrs, then this method should call dwInterface.showErrorMessage().
	 * @param configuration
	 * @param dwInterface
	 */
	@Override
	public void run(Properties configuration, IPluginHelper dwInterface) {
		int queryType = QUERY_TYPE_CODE[0].equals(configuration.getProperty(CONFIGURATION_QUERY_TYPE)) ? 0 : 1;
		String queryIDCode = configuration.getProperty(CONFIGURATION_QUERY_STRUCTURE);
		int similarityLimit = Integer.parseInt(configuration.getProperty(CONFIGURATION_SIMILARITY_LIMIT, "85"));

		// Now one would send this information to a server for the virtual screening and get some result back...
		// If the server returns a result table, then the following code illustrates how to create
		// a new DataWarrior window and how to populate its data table with the result data from the server.

		// Our hypothetical result contains idcodes including encoded 3D-coordinates, which represent
		// the virtual screening hit structures as their best conformers.
		// We want DataWarrior to show these conformers superposed and together with the query conformer!
		// We create three columns, one for the superposed structure, one for its ID, and one for the superposition score.
		dwInterface.initializeData(3, RESULT_ID.length, "PheSA Virtual Screening Result");
		dwInterface.setColumnTitle(0, "ID");
		dwInterface.setColumnTitle(1, "Structure");
		dwInterface.setColumnTitle(2, "PheSA Score");

		// We assume that we get idcodes from the server. If we received Molfiles instead,
		// we would need to use a different column type here.
		dwInterface.setColumnType(1, IPluginHelper.COLUMN_TYPE_3D_STRUCTURE_FROM_IDCODE);

		// We define here the query conformer to be shown together and superposed to the virtual screening hits
		int coordsColumn = dwInterface.getCoordinateColumn(1, true);
		dwInterface.setColumnTitle(coordsColumn, "Best Conformer Superposed");
		dwInterface.setColumnProperty(coordsColumn, "superposeMol", QUERY_CONFORMER);

		for (int row=0; row<RESULT_ID.length; row++) {
			// For demonstration purposes we just use dummy molfiles and structure-IDs
			dwInterface.setCellData(0, row, RESULT_ID[row]);
			dwInterface.setCellData(1, row, RESULT_CONFORMER[row]);
			dwInterface.setCellData(2, row, RESULT_VALUE[row]);
		}

		// Here we use a custom template, because we don't want default views and we want the 3D-detail-view shown much larger.
		dwInterface.finalizeData(TEMPLATE);
	}

private static final String QUERY_CONFORMER = "fi{p`@DJjBVQQQQZ[QQIQFJ\\DUm^mUTuPTQCIPp`@ #qPi]Kj[HQLAtT?jDwkRM}N\\Mp_?}qvVGAwypKqisWMlODRL]jyqn{QvNyYUeaqrEd|QMjbiBCKUYjbcLHQrnPnzMyFxLojJNZyYRIWVWtihzx`r]gvOokZV^ljf|IiUO|Q@C\\tdHiZPnOztty\\}PH@PT{o`mGzyty}l[~[Q`WowlcZ_^ldlTFZqXsMlb}}EHqE~IkV]XBJwWc{@Ifwgd@V^@KTnkFc@mnbPJf?_YQgTtconRSC_EzOTRRfgKU|eGbsLgWTfLc^^gpsfMTXGz`FxLq]yE}rjA\\{mTA{I?Fa\\w}MXr]rKX[sYq\\JfIDYT_b|fZ|BUOKkHETEJnNTPLn|]VrCfU}WV`eKVv}wQGI|nhDr_qcWEe?MBuIUxhx@Z_L~cKt^^ACEqiZ@_PAh@D";
private String[] RESULT_ID = { "A-16253", "A-04993", "A-99265", "A-22548", "A-53965", "A-18665", "A-43991", "A-27736" };
private String[] RESULT_VALUE = { "0.82178", "0.78937", "0.76881", "0.87051", "0.84722", "0.84942", "0.87159", "0.78695" };
private String[] RESULT_CONFORMER = {
		"ff}```F^bBlxDIBJrFQQQJJMJJKYI`nR`HajifjJAd@ #q_?|z_ymZuhm`okDDSbFe~z|si[LVArL^gNdlm@Sxa}ONV}[nJgl|[jqPpWxYNzJKcElKyP@\\W\\KrlC]fVBCMrUfJoJIPTK[fqsuh@bGx@hA^B]XF{@vN^OsQCayfGTa`u}Rwzcomh|OjaFwHwd`]LIjA}[BAeXlR{\\ih^pp}]|Jdx_ISs]uzTue^{DVejlvL^W{xDkmsbltpBoF|ALEWsUItXKIuK]K}tDE@DmMsu?ZMKSOtptR}T_^V^qWrz}@ArhTZcAKJc@y@eaH{L}ppOBc^ZHgj~[YdD^i|@W@A}@@",
		"fisab`DD\\DDPJHcH]dArFQQQ[SIQQZFIITYSUSULuUThuOP@ #q_KHjdIo|lZPBDgZuSDWqde{x[LHyQRc{Sv[OQO]|`FrtQNeNwxPT[G]ytBpUIZr_Jd[cqHgBBPQQ}AyiTIpdmechedLIXl~dkIcxwC]Y[LllPj_q~RjMfWyP\\kaTAVhnnptR^?JJtFgTjE[\\m?IKBXJPc_|}Mo\\^@@B~D]FA^ye[BH^b}WkNfv\\ChHkzEJU]rJOiFQ|RBcVm`}]ipxDVPeo\\lXwGeoJl[SJQaWAc^LVJ}EIyxxZxb~AzgHf`UHtEkJReRVEaTNkG[ETUX^gmGHBRFLTmtxmjy~TE`Q?KLNW`H{~rCNiAsFlE]@Y_|]YTmuno}awnpEjC_b{TFfi`@W`@Z@@",
		"feka`@H`LwHhiDcDdXmEDeIfkUUUMUMUThxkH@ #qH_SlRRlBh_{rM`@@wfaAc]rUYw]LhnXffWad}FEfWwXruyu\\dseJgRaw_YE[yaW@wJcTuXUMoSLnqwuVms_AhipF]pPRUinz_RVgSTxOUdO?uxkV~YNr}HHcHKgA?[HWxslfD?EqETHD`WuGswX]{Xvghd]kZ_ELyOWtB??c\\HM\\J{{yZBFu_reFP_^kYhQ}uOzIOObHCP[~WRhBwpHuoHJQV^NesT]YenGPcFsuCOSkIX[ieSydueUdAaEvcNmugYEp}nId|}SUQQ`qT]v_`y]iLfVcHiC]WErnda{fV^{FlySr~uDR{XzS@MOhEm@J`vkb`qVr@@GpMmydYqNoKZlIIyDQcCYuQjrRr]YM?hi~{Ucb|{MKUhGJcce[yTVsdeOaYkxJ\\DkZKH?iuRsM|aoqXP[BFXfKtZYwv]pSY}MJJ\\XJqPEKs]oSYQs_f]XrdeDiYO~IfaMjf[jrErDSmPGsOZrbR\\JrsTF^iEmq|iM_JTLr\\JhL@^HAJ@D",
		"fikpa@F^jdhI@d`dsMjzoKwOARUADEU]SMTH@@ #qq@qnzJwFxdd[kCX`Sncbfab{Yp]oqPnTuYR||OvFRWk?wUv?fYPT|MvFsncRfcb{QD`KcE`MrtRfkaUOrwSijMCFv\\`hn@eIN~BwYRBbMBQLMEwqFDqKZFxfBlcRSkD}DEEIHmwPcLp\\}J^n@}m\\V@}~EYwRyev{fCGppnCpXQs|]^txDTl?VM~V@@BDtnFvqcDyll`tcQU^dvrU\\dipbSvkF{[NToyK?@tuPhzB^zgccWWTBDh?`?@}P~qH_Ohq\\dxnt{BC@^SIfmJfJNfW@z[lKLwUsmMTNnyrqp^QNPhsgB[R_wNQAWTrr`V?ZltkajIxBUCs\\TE?nFnvrrhsuQZLJNj`@HP@B@@",
		"fnc`b@L|t@HrQQKPqKJJIQQI\\ujiZf`HabQBBP@ #q{c@JlB`APtrhyypgTTchUW[\\YGUhXDYJYSzLLg??s@aEtVAuROWhNLXiqesgXxI[OH@~B[`jF]tEg|hYgRQ^V`_MtU~_Mo]]lKtR[xRYymYuxzbWHrnZuRAT?DNOU{vqeDph{iHWeot|uqlID[iDK^[WCbo|eWkTb^edqu`SOrSDaMhy}NYlKdK{QTPcTU|F{uVdf?s~ZgF?j]RDnerVWGGQ^nd}`YOxe_ONQn]MJ[YEuba{PfrV~TwbuU{ATo[KtN{dSVaTzZKwkZw]ieD_KXDtUNyUTjsKOiwPtfmBv@gyLazt[?ut^Qw\\bhTM\\zHEWHusrxZqWii]vYFkrhK]sdDlaSVGeD[IReKqFW{slzjT@WpAK@D",
		"fns``@J`EdRbdTRJbTRQeoYZVkUUUPU@qEaDqBP@ #qt]^tkh`I@wmipqY|YoLsHzf{lnOtSA}T`Yn~lvH\\aCQkRtFShrMXEAOsVhv?FFr}MPr^JoQXIln{JxY}v}WAUQD_~UM{iiMHX[X`kU`Obf]_Q\\`BUI^b|sQ~\\kT@@GY~znpQzTopCTojRvNUhHoqbuDu`We@LxC[XykRYPbwrj_xaV|BCpTU@c}CM@jt`}\\RR}NDcRwZ_Qw|HjHrWgHIkW[|nNXsL{FROcQoboYIg|JjLd`]pWtoMZ]]ubw~`k[RotVt\\lNB_GHtCJeYA}rWPuoV\\Df|KHa]MVqlCPqUdKMvwQi~hpgaVndTwYwGmeOS`VsE@iIAR\\tw]`gtRdYjN}l~pXq`x`H~hJwV}kXLh[L}zR?RIJjLRXoKZMTww_UMrWv??ZcEjZkz@WPAP@D",
		"fk\u007F`a`JEZBlIDBBCRCi@LadTTTLRTRvdtTVvuNJmT\\uAAQUTsUUDctk@@ #qEw?jcsrRbAg`i?_mmmOW{nax|eIQ`x{oKjl?{]GbbVgYcG_D{dNUi~eAANp@@IsNVtoyNW`tRloRV_rtbGbQu_DpxLeyGE]daWvLixQao}uJQs]arcQaAEnZcGL~FSetnItAjHHr^JQ@Ihf[{WqUu\\}HfTojTkqejBI]jnPhaqbXujkVnITkOgM_e]kmJDF|czlxcbNxPOQAwNDsz\\nF~IHyOIo|um~DBDTSLykizrvQNQMlz?ImtxIyMrqrkY\\idqu]KWnRC]ihuMxijfGaFv@EYzm?FeaAHHrYNb~eqCZuRTNdMHkwua`Lg_UHBzCvs_dO_jL[nQzSYIqFOiRyi]?~U{UIZX{sShjY}KJIUstvoF[Yj~iOQ`ot{puq\\{~vcNk\\@\\`AE@@",
		"fe{ac@BRRtDQZPrDYEEDdeHiuDieEbLzNZfjjjfiik@ezQ`@ #q\\K]bvyjFniwELZrXCeWSB~^snAljb}vIZRX]hnZHL|lLoV|Rr@^pi^tLpiSUADcqWewDV?oEg^yboBzpwPy??|@FzE?m}itMPcUMCDaSM]XrXbTpvcPP~AsI\\K@ESZ]yAWXWODlHhbR_tdq~cYHhR|]@@G^\\@cNf[uD]RJMHGUOZShBS_uGtP`PHjM~NzjVnP?TizKBAYhZG\\WaklCiQ?Y|bSgM?~`WjK?Qw~S|ehZ[@jXxsTGwnA[fBJuQYDXvZfxgeILmQdSGEG\\fShgtPVR]|LURZBU|zidgdYHSIqkEUcYIsi`LqNYaBspoqncuzepXWo\\xBJCfydnKc|xGGVtNYc?r`oGwqV|[XiSMtyzKvg@DiQqukxxWVcjFhhfSlWWfCFiEsk]EH|o^L|fk\\@W@AY@@"
	};
private static final String TEMPLATE = "<datawarrior properties>\n" +
		"<columnFilter_Table=\"\">\n" +
		"<columnWidth_Table_ID=\"80\">\n" +
		"<columnWidth_Table_PheSA Score=\"80\">\n" +
		"<columnWidth_Table_Structure=\"100\">\n" +
		"<detailView=\"height[Data]=0.1439;height[Structure]=0.29455;height[Best Conformer Superposed]=0.56182\">\n" +
		"<filter0=\"#structure#\tStructure\">\n" +
		"<filter1=\"#double#\tPheSA Score\">\n" +
		"<filter2=\"#string#\tID\">\n" +
		"<filterAnimation1=\"state=stopped low2=80% high1=20% time=10\">\n" +
		"<headerLines_Table=\"2\">\n" +
		"<mainSplitting=\"0.6\">\n" +
		"<mainView=\"Table\">\n" +
		"<mainViewCount=\"1\">\n" +
		"<mainViewDockInfo0=\"root\">\n" +
		"<mainViewName0=\"Table\">\n" +
		"<mainViewType0=\"tableView\">\n" +
		"<rightSplitting=\"0.42447\">\n" +
		"<rowHeight_Table=\"80\">\n" +
		"</datawarrior properties>";
}