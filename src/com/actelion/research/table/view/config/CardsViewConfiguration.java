package com.actelion.research.table.view.config;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.cardsurface.CardElementLayoutLogic;
import com.actelion.research.table.view.card.cardsurface.gui.JCardWizard2;
import com.actelion.research.table.view.card.tools.PropertiesSerializationHelper;
import com.actelion.research.util.DoubleFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CardsViewConfiguration extends ViewConfiguration<JCardView> {
	public static final String VIEW_TYPE = "cardsView";

	// from DETaskSetCardViewOptions
	private static final String KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS = "HideRowsInOtherViews";
	private static final String KEY_RENDER_HQ_UB       = "RenderHqUB";
	private static final String KEY_RENDER_NORMAL_UB   = "RenderNormalUB";
	private static final String KEY_RENDER_SHADOW      = "RenderShadow";
	private static final String KEY_RENDER_BIM_RES     = "RenderBIMResolution";
	private static final String KEY_RENDER_COMPUTE_HIDDEN_CARDS  = "RenderComputeHiddenCards";
	private static final String KEY_RENDER_SMALL_PANE_UPDATE_RATE  = "RenderSmallPaneUpdateRate";
	private static final String KEY_RENDER_NUM_CARD_CACHE_THREADS  = "RenderCardCacheThreads";

	// from DETaskConfigureCard
	public static final String KEY_SELECTED_COLUMNS_STRUCTURES        = "SelectedColumnsStructures";
	public static final String KEY_SELECTED_COLUMNS_NUMERICAL_VALUES  = "SelectedColumnsNumericalValues";

	// to save Viewport:
	private static final String KEY_VIEWPORT_CX = "ViewportCenterX";
	private static final String KEY_VIEWPORT_CY = "ViewportCenterY";
	private static final String KEY_VIEWPORT_ZOOM_FACTOR = "ViewportZoomFactor";



	public CardsViewConfiguration(CompoundTableModel tableModel) {
		super(tableModel);
	}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}

	@Override
	public void apply(JCardView cardView) {
		apply_ConfigureCards(cardView);
		apply_CardViewOptions(cardView);

		apply_Viewport(cardView);
	}

	@Override
	public void learn(JCardView cardView) {
		learn_ConfigureCards(cardView);
		learn_CardViewOptions(cardView);

		learn_Viewport(cardView);
	}



	private void learn_ConfigureCards(JCardView cardView) {
		// Cards Configuration
		List<String> column_names_structure = cardView.getGlobalConfig().stream().filter(si -> si.isChemical() ).map(si -> si.getColumnName() ).collect(Collectors.toList());
		List<String> column_names_numeric   = cardView.getGlobalConfig().stream().filter( si -> si.isNumeric() ).map( si -> si.getColumnName() ).collect(Collectors.toList());
		try {
			this.setProperty(KEY_SELECTED_COLUMNS_STRUCTURES, PropertiesSerializationHelper.stringListToString( column_names_structure ) );
			this.setProperty(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES, PropertiesSerializationHelper.stringListToString( column_names_numeric ));
		}
		catch(Exception e){
			System.out.println(e);
		}
	}

	private void apply_ConfigureCards(JCardView cardView) {
		JCardPane cardPane = ((JCardView)cardView).getCardPane();
		CompoundTableModel mCTM = cardView.getTableModel();
		//MyProperties p = new MyProperties(configuration);
		String s_structure_columns  = this.getProperty(KEY_SELECTED_COLUMNS_STRUCTURES);
		String s_numeric_columns    = this.getProperty(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES);

		List<String> structure_columns  = PropertiesSerializationHelper.stringListToString_inverse(s_structure_columns );
		List<String> numeric_columns    = PropertiesSerializationHelper.stringListToString_inverse(s_numeric_columns );

		// init list of columns:
		List<JCardWizard2.ColumnWithType> columns = new ArrayList<>();

		for(String si : structure_columns){
			columns.add( new JCardWizard2.ColumnWithType(mCTM, mCTM.findColumn(si)+"/"+ JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE));
		}
		for(String si : numeric_columns){
			columns.add( new JCardWizard2.ColumnWithType(mCTM, mCTM.findColumn(si)+"/"+JCardWizard2.ColumnWithType.TYPE_NUMERIC));
		}

		CardElementLayoutLogic layout_logic = ((JCardView)cardView).getCardElementLayoutLogic();
		CardElementLayoutLogic.CardAndStackConfig config = layout_logic.createFullCardWizardConfig(columns);
		cardPane.getCardView().setGlobalConfig( columns );
		cardPane.getCardDrawer().setCardDrawingConfig(config.CDC);
		cardPane.getStackDrawer().setStackDrawingConfig(config.SDC);
		cardPane.reinitAllCards();
	}



	private void learn_CardViewOptions(JCardView cardView) {

		JCardPane cardPane = cardView.getCardPane();


		boolean hideRowsInOtherViews = cardPane.getCardPaneModel().isRowHiding();

		int numCardsFull    = cardPane.getNumCards_HQ();
		int numCardsNormal  = cardPane.getNumCards_SUPERFAST();

		boolean enableShadows = !cardPane.isDisableShadowsEnabled();

		double renderPixels = cardPane.getCachedCardProviderNumberOfPixelsToDraw();
		boolean computeHiddenCards = cardPane.isComputeHiddenCardsEnabled();
		//int fastPaneUpdateRate = cardView.getFastCardPane().getUpdateRate();
		int numCardCacheThreads = cardPane.getCardCacheThreadCount();

		this.setProperty( KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS , ""+hideRowsInOtherViews );

		this.setProperty( KEY_RENDER_HQ_UB       , ""+numCardsFull );
		this.setProperty( KEY_RENDER_NORMAL_UB   , ""+numCardsNormal );
		this.setProperty( KEY_RENDER_BIM_RES     , ""+renderPixels );

		this.setProperty( KEY_RENDER_COMPUTE_HIDDEN_CARDS , ""+computeHiddenCards );
		this.setProperty( KEY_RENDER_SHADOW , ""+enableShadows );

		//this.setProperty( KEY_RENDER_SMALL_PANE_UPDATE_RATE , ""+fastPaneUpdateRate );
		this.setProperty( KEY_RENDER_NUM_CARD_CACHE_THREADS , ""+numCardCacheThreads );
	}

	private void apply_CardViewOptions(JCardView cardView) {
		JCardPane cardPaneView = ((JCardView) cardView).getCardPane();

		boolean hideCardsInOtherViews = Boolean.parseBoolean( this.getProperty( KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS ) );

		double numCardsFull         = Double.parseDouble( this.getProperty( KEY_RENDER_HQ_UB   ) );
		double numCardsNormal       = Double.parseDouble( this.getProperty( KEY_RENDER_NORMAL_UB) );
		boolean enableShadows       = Boolean.parseBoolean( this.getProperty( KEY_RENDER_SHADOW) );
		double renderPixels         = Double.parseDouble( this.getProperty( KEY_RENDER_BIM_RES ) );
		boolean computeHiddenCards  = Boolean.parseBoolean( this.getProperty( KEY_RENDER_COMPUTE_HIDDEN_CARDS ));
		//int  fastPaneMillisec       = (int) (1000.0 / Double.parseDouble( this.getProperty( KEY_RENDER_SMALL_PANE_UPDATE_RATE ) ));
		int numCardCacheThreads     = Integer.parseInt( this.getProperty(KEY_RENDER_NUM_CARD_CACHE_THREADS) );

		if(false) {
			System.out.println("Resolution: " + renderPixels);
			System.out.println("Set compute hidden cards to: " + computeHiddenCards);
		}

		cardPaneView.setNumCards_HQ((int)   numCardsFull);
		cardPaneView.setNumCards_SUPERFAST((int) numCardsNormal);
		cardPaneView.setDisableShadowsEnabled(!enableShadows);
		cardPaneView.setPixelDrawingPerformance((int) renderPixels);
		cardPaneView.setComputeHiddenCardElementsEnabled(computeHiddenCards);
		//cardPaneView.getCardView().getFastCardPane().setUpdateRate( fastPaneMillisec );
		cardPaneView.setCardCacheThreadCount(numCardCacheThreads);

		cardPaneView.getCardPaneModel().setActiveExclusion(hideCardsInOtherViews);
		cardPaneView.reinitAllCards();
	}


	private void learn_Viewport(JCardView cardView) {
		this.put(KEY_VIEWPORT_CX, DoubleFormat.toString(cardView.getCardPane().getViewportCenterX()));
		this.put(KEY_VIEWPORT_CY, DoubleFormat.toString(cardView.getCardPane().getViewportCenterY()));
		this.put(KEY_VIEWPORT_ZOOM_FACTOR, DoubleFormat.toString(cardView.getCardPane().getViewportZoomFactor()));
	}


	private void apply_Viewport(JCardView cardView) {
		double cx = Double.parseDouble( this.getProperty(KEY_VIEWPORT_CX) );
		double cy = Double.parseDouble( this.getProperty(KEY_VIEWPORT_CY) );
		double zf = Double.parseDouble( this.getProperty(KEY_VIEWPORT_ZOOM_FACTOR) );
		cardView.getCardPane().setViewport(cx,cy,zf);
	}





}
