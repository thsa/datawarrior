package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.cardsurface.gui.JComboBoxForTableColumns;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class DETaskArrangeCards extends ConfigurableTask {

    private static final String TASK_NAME = "Arrange Cards";

    public static final String KEY_COL = "ColTitle";

    private DECardsViewHelper.Applicability mApplicability;

    private DataWarriorLink    mLink = null;
    private CompoundTableModel mCTM = null;
    private JCardPane          mCardPane = null;

    private int mColumn;

    public DETaskArrangeCards(Frame owner, DataWarriorLink link , JCardPane cardPane, DECardsViewHelper.Applicability applicability,  int col) {
        super(owner,false);
        mLink = link;
        mCTM  = mLink.getCTM();
        mCardPane = cardPane;

        mColumn = col;
        mApplicability = applicability;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
        return true;
    }

    @Override
    public Properties getPredefinedConfiguration() {
        if(mColumn >= 0 ) {
            Properties p = new Properties();
            p.setProperty(KEY_COL,""+mCTM.getColumnTitle(mColumn));
            return p;
        }
        else{
            return null;
        }
    }

    @Override
    public void runTask(Properties configuration) {
        int idx = mCTM.findColumn( configuration.getProperty(KEY_COL) );
        arrange( idx );
    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }


    public static final String VALUE_NO_COL_SELECTED = "<none>";

    /**
     * Idx==-1 means it is "none"
     */
    class Col {
        public final String Name;
        public final int Idx;
        public final boolean isNumeric;
        public final boolean isCategorical;
        public Col(String name, int idx, boolean isnumeric, boolean iscategorical){
            this.Name = name;
            this.Idx  = idx;
            this.isNumeric = isnumeric;
            this.isCategorical = iscategorical;
        }

        @Override
        public String toString() {
            if(Idx<0){
                return VALUE_NO_COL_SELECTED;
            }
            return Name+ ((isNumeric)?"[Numeric]":"") + ((isCategorical)?"[Categorical]":"");
        }
    }


    // All numerical / categorical
    java.util.List<Col> mColumns_X = new ArrayList<>();


    public void initColumns() {
        mColumns_X = new ArrayList<>();

        for(int zi=0;zi<mCTM.getColumnCount();zi++){
            String name = mCTM.getColumnTitle(zi);
            int    idx  = zi;
            boolean isnumeric     = mCTM.isColumnTypeDouble(zi);
            boolean iscategorical = mCTM.isColumnTypeCategory(zi);

            if(isnumeric || iscategorical){
                mColumns_X.add(new DETaskArrangeCards.Col(name,idx,isnumeric,iscategorical));
            }
        }
    }

    private JComboBox<Col> cb_cols = null;

    @Override
    public JComponent createDialogContent() {

        initColumns();

        JPanel p = new JPanel();
        double p4   = HiDPIHelper.scale(4);
        double p8   = HiDPIHelper.scale(8);
        double pref = TableLayout.PREFERRED;
        p.setLayout(new TableLayout( new double[][]{ {p4,pref,p4} , {p4,pref,p4} } ) );

        cb_cols = new JComboBox( new Vector(mColumns_X) );
        p.add(cb_cols,"1,1");

        if(mColumn > 0) {
            int cci = 0;
            boolean found_col = false;
            while( ! found_col && cci < this.mColumns_X.size() ) {
                found_col = this.mColumns_X.get(cci).Idx == mColumn;
                if(!found_col){ cci++; }
            }
            cb_cols.setSelectedIndex(cci);
        }

        return p;
    }

    @Override
    public Properties getDialogConfiguration() {
        Properties p = new Properties();
        p.setProperty( KEY_COL , ( (Col) cb_cols.getSelectedItem() ).Name );
        return p;
    }

    @Override
    public void setDialogConfiguration(Properties configuration) {

        if(cb_cols==null) {
            initColumns();
        }

        if(configuration.containsKey(KEY_COL));
        String col_title = configuration.getProperty(KEY_COL);

        if(col_title != null) {
            boolean found = false;
            int found_idx = -1;
             for(int zi=0;zi<cb_cols.getItemCount();zi++) {
                 if(cb_cols.getItemAt(zi).Name.equals(col_title)) {
                     found_idx = zi;
                     found = true;
                     break;
                 }
             }
             if(found) {
                 cb_cols.setSelectedIndex(found_idx);
             }
        }
    }

    @Override
    public void setDialogConfigurationToDefault() {

    }






//------------------ LOGIC ---------------------------------------------------------------------------------------------

    public void arrange(int col_x) {

        // 1. determine set of cards to arrange

        List<Integer> to_arrange  = new ArrayList<>();
        List<Integer> all_visible = new ArrayList<>();
        List<Integer> all_with_invisible = new ArrayList<>();

        List<Integer> remaining   = new ArrayList<>();

        boolean selected_only = (mApplicability == DECardsViewHelper.Applicability.SELECTED);

        if( selected_only ) {
            for (int zi = 0; zi < mCTM.getTotalRowCount(); zi++) {
                all_with_invisible.add(zi);
                if (mCTM.isVisible(mCTM.getTotalRecord(zi))) {
                    all_visible.add(zi);
                    if( mCTM.isSelected(mCTM.getTotalRecord(zi)) ) {
                        to_arrange.add(zi);
                    }
                }
            }
            remaining.addAll(all_visible);
            remaining.removeAll(to_arrange);
        }
        else{
            for (int zi = 0; zi < mCTM.getTotalRowCount(); zi++) {
                all_with_invisible.add(zi);
                if (mCTM.isVisible(mCTM.getTotalRecord(zi))) {
                    all_visible.add(zi);
                    //if( mCTM.isSelected(mCTM.getTotalRecord(zi)) ) {
                    to_arrange.add(zi);
                    //}
                }
            }
            remaining.addAll(all_visible);
            remaining.removeAll(to_arrange);
        }

        // check if we can just return
        if(to_arrange.size()==0) {
            // no cards, so just return..
            System.out.println("[INFO] DETaskArrangeCards : no cards selected --> return");
            return;
        }


        // 2. determine "starting pos x" and "anchor y":
        // for x we also consider invisible cards
        // for y we take the mean of visible cards


        double start_x = mCardPane.getCardPaneModel().getAllElements().stream().mapToDouble( ei -> ei.getPosX() ).max().getAsDouble();
        double pos_y   = mCardPane.getCardPaneModel().getReducedAllElements().stream().mapToDouble( ei -> ei.getPosY() ).average().getAsDouble();


        // 3. Compute some basic alignment values
        // maybe make relative to card width..
        double cw = mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth();
        double ch = mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight();

        double gap_x = 6*cw;

        double pos_x = start_x + gap_x;

        boolean invert = false;
        to_arrange.sort( (Integer a , Integer b) -> (invert?-1:1) * Double.compare(  mCTM.getTotalDoubleAt(a,col_x) , mCTM.getTotalDoubleAt(b,col_x) )  );

        double shift_x_afterwards = 0.0;

        // 4.1 Compute positions:
        Map<Integer, Point2D> pos = null;
        int nPos = to_arrange.size();
        pos = computeGrid(nPos, cw, ch);

        // 4.2. put right to pos_x
        double x_pos_min = pos.values().stream().mapToDouble(pi -> pi.getX()).min().getAsDouble();
        double x_pos_max = pos.values().stream().mapToDouble(pi -> pi.getX()).max().getAsDouble();

        double shift_this = -x_pos_min;
        shift_x_afterwards = x_pos_max - x_pos_min;

        List<Double> px_all = new ArrayList<Double>();
        List<Double> py_all = new ArrayList<Double>();
        List<CardElement> new_card_elements = new ArrayList<>();

        for (int zi = 0; zi < nPos; zi++) {
            ArrayList<CompoundRecord> crlist = new ArrayList<>();
            crlist.add(mCTM.getTotalRecord(to_arrange.get(zi)));

            double pxi = pos_x + shift_this + pos.get(zi).getX();
            double pyi = pos.get(zi).getY();
            px_all.add(pxi); px_all.add(pyi);

            mCardPane.getCardPaneModel().removeRecords(crlist);
            CardElement ce_new = mCardPane.getCardPaneModel().addCE(crlist,pos_x + shift_this + pos.get(zi).getX(), pos.get(zi).getY());

            new_card_elements.add(ce_new);
            //mFastCardPane.getCardPaneModel().addCE(crlist, pos_x + shift_this + pos.get(zi).getX(), pos.get(zi).getY());
        }

        // 5. move viewport
        DoubleSummaryStatistics dss_x = px_all.stream().mapToDouble(di -> di.doubleValue()).summaryStatistics();
        DoubleSummaryStatistics dss_y = px_all.stream().mapToDouble(di -> di.doubleValue()).summaryStatistics();

        mCardPane.setViewportAroundSpecificCardElements( new_card_elements , 0.2, 8 , 3);
    }

    public Map<Integer,Point2D> computeGrid( int n , double cw , double ch ) {
        // define gap
        double gap = 0.2 * cw;

        Map<Integer,Point2D> grid_points = new HashMap<>();

        // we loop over all possible rectangles and take the one closest to 4/3:
        double target_ratio = 4.0/3.0;

        int n_best = 100; double ratio_best=Double.MAX_VALUE;
        for(int zi=1;zi<n;zi++){
            int y = n / zi;
            double ratio = (1.0*zi*cw )/ (1.0*y*ch);
            double ratio_distance = Math.abs( ratio-target_ratio );
            if(ratio_distance < ratio_best) {
                n_best = zi; ratio_best = ratio_distance;
            }
        }

        // position cards at this ratio.. first place just place the square, afterward make it zero centered..
        double x = 0; double y = 0;
        for( int zi=0;zi<n;zi++ ){
            if( zi % n_best == 0 ){ y += gap + ch; }
            x = (zi % n_best) * ( gap + cw ) ;
            grid_points.put( zi , new Point2D.Double( x , y ) );
        }

        // find center of gravity and subtract..
        double xmean = grid_points.values().stream().mapToDouble( pi -> pi.getX() ).average().getAsDouble();
        double ymean = grid_points.values().stream().mapToDouble( pi -> pi.getY() ).average().getAsDouble();

        Map<Integer,Point2D> grid_points_centered = new HashMap<>();
        for( Integer ki : grid_points.keySet() ){
            double xc = grid_points.get(ki).getX() - xmean;
            double yc = grid_points.get(ki).getY() - ymean;
            grid_points_centered.put( ki , new Point2D.Double(xc,yc) );
        }

        return grid_points_centered;
    }














}
