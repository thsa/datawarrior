package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.mcs.MCSFunctions;
import com.actelion.research.gui.generic.GenericDepictor;
import com.actelion.research.gui.swing.SwingDrawContext;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


/**
 * NOTE: hashes computed StereoMolecule MCS.
 * NOTE: for sets where the computation fails, an empty StereoMolecule is added to the hash.
 *
 *
 */
public class StackChemStructurePanel extends AbstractStackSurfacePanel {

    public static final String NAME = "Chem. Structure [Stack]";

    //Map<HashableStereoMoleculeSet,StereoMolecule> mMCS = new HashMap<>();
    Map<FastHash,StereoMolecule> mMCS = new HashMap<>();

    private int mColumns[] = new int[]{-1};

    @Override
    public void drawPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> recList , CardElement ce, int[] columns, int w, int h) {

        // draw Background:
        //Color bgColor = conf.getColorHandler().getVisualizationColor(columns[0], CompoundTableColorHandler.BACKGROUND).getColor(rec);
//        Color bgColor = Color.white;
//        g.setColor( bgColor );
//        g.fillRect( 0,0,w,h );


        // avoid ConcurrentModificationException..
        recList = new ArrayList<>(recList);

        if(this.mColumns[0]<0){
            return;
        }
        if(!model.isColumnTypeStructure(this.mColumns[0])){
            return;
        }

        // check for hashed version:
        List<Integer> rIDs = recList.stream().map( ri -> ri.getID() ).collect(  Collectors.toList() );
        FastHash fhash = new FastHash( columns[0] , rIDs );

        StereoMolecule mcs = null;
        if(mMCS.containsKey( fhash )){
            mcs = mMCS.get( fhash );
        }
        else{
            // fetch molecules:
            List<StereoMolecule> molecules = new ArrayList<>();
            for( CompoundRecord r : recList ) {
                StereoMolecule m = null;
                try {
                    m = model.getChemicalStructure(r, columns[0], CompoundTableModel.ATOM_COLOR_MODE_ALL, null);
                }
                catch(Exception e){
                    //System.out.println("ERROR: Fetching StereoMolecule failed.. "+e.toString());
                }
                if(m!=null) {
                    molecules.add(m);
                }
                else{
                    //System.out.println("ERROR: Fetching StereoMolecule returned null.. ");
                }
            }

            // compute mcs
            mcs = null;
            try {
                mcs = new MCSFunctions().getMaximumCommonSubstructure(molecules);
            } catch (Exception e) {
                System.out.println("WARNING: MCS calculation threw exception: " + e.toString());
                mcs = new StereoMolecule();
            }
            //mMCS.put( hsms , mcs );
            // PUT EMPTY StereoMolecule: (else we always try to recompute, not good..)
            mMCS.put( fhash , mcs );
        }

        // draw the mcs StereoMolecule:
        // draw the mcs ------------------------------------------------------------------------------------------------

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        StereoMolecule mol = mcs;

        if(mol!=null) {
            GenericDepictor depictor = new GenericDepictor(mol);
            SwingDrawContext context = new SwingDrawContext((Graphics2D)g);
            depictor.validateView(context, new Rectangle2D.Double(2, 2, w-4, h-4), AbstractDepictor.cModeInflateToMaxAVBL);
            depictor.paint(context);
        }
        else{

        }
    }

//    public void drawPanel_old(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> recList , int[] columns, int w, int h) {
//
//        if(this.mColumns[0]<0){
//            return;
//        }
//        if(!model.isColumnTypeStructure(this.mColumns[0],false)){
//            return;
//        }
//
//        List<StereoMolecule> molecules = new ArrayList<>();
//        for( CompoundRecord r : recList ) {
//            StereoMolecule m = null;
//            try {
//                m = model.getChemicalStructure(r, columns[0], CompoundTableModel.ATOM_COLOR_MODE_ALL, null);
//            }
//            catch(Exception e){
//                System.out.println("ERROR: Fetching StereoMolecule failed.. "+e.toString());
//            }
//            if(m!=null) {
//                molecules.add(m);
//            }
//            else{
//                System.out.println("ERROR: Fetching StereoMolecule returned null.. ");
//            }
//        }
//
//        // check if we have the MCS hashed:
//        HashableStereoMoleculeSet hsms = new HashableStereoMoleculeSet(molecules);
//        StereoMolecule mcs = null;
//
//        if( mMCS.containsKey(hsms) ){
//            mcs = mMCS.get( hsms );
//        }
//        else {
//            mcs = null;
//            try {
//                mcs = new MCSFunctions().getMaximumCommonSubstructure(molecules);
//            } catch (Exception e) {
//                System.out.println("WARNING: MCS calculation threw exception: " + e.toString());
//                mcs = new StereoMolecule();
//            }
//            //mMCS.put( hsms , mcs );
//            // PUT EMPTY StereoMolecule: (else we always try to recompute, not good..)
//            mMCS.put( hsms , mcs );
//        }
//
//        // draw the mcs ------------------------------------------------------------------------------------------------
//
//        Graphics2D g2 = (Graphics2D)g;
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
//        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
//        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//
//        StereoMolecule mol = mcs;
//
//        if(mol!=null) {
//            Depictor2D depictor = new Depictor2D(mol);
//            depictor.validateView(g, new Rectangle2D.Double(2, 2, w-4, h-4), AbstractDepictor.cModeInflateToMaxAVBL);
//            depictor.paint(g);
//        }
//        else{
//            int xycnt = 0;
//            int rect_width = 40;
//            for(int xx =0;xx<w-rect_width;xx+=rect_width ) {
//                for (int yy = 0; yy < h - rect_width; yy+=rect_width) {
//                    if (xycnt % 2 == 0) {
//                        g.setColor(Color.black.brighter().brighter().brighter());
//                    } else {
//                        g.setColor(Color.orange.darker().darker().darker().darker());
//                    }
//                    //g.fillRect(xx,yy,rect_width,rect_width);
//                    xycnt++;
//                }
//            }
//        }
//    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public JComponent getLabel() {
        return null;
    }

    @Override
    public JPanel getConfigDialog() {
        return null;
    }

    @Override
    public int getNumberOfDataColumnsRequired() {
        return 1;
    }

    @Override
    public boolean canHandleColumnForSlot(CompoundTableModel model, int slot, int column) {
        if(slot>0){
            System.out.println("ERROR: Asked for wrong slot of StackChemStructurePanel..");
        }
        return model.isColumnTypeStructure(column);
    }

    @Override
    public void setColumns(int[] dataLinks) {
        if(dataLinks.length!=1){
            System.out.println("SingleDataPanel : Wrong number of data columns supplied! "+dataLinks.length+" instead of "+1);
        }
        this.mColumns[0] = dataLinks[0];
    }

    @Override
    public int[] getColumns() {
        return mColumns;
    }


    @Override
    public void initalizeStackPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, int focusListFlag, List<CompoundRecord> records, int[] columns, int w, int h) {

    }

    private static class FastHash {
        int mColumn;
        Set<Integer> mIDs;

        public FastHash( int column, List<Integer> cr_ids){
            this.mColumn = column;
            this.mIDs    = new HashSet<>(cr_ids);
        }

        public int getCol(){return this.mColumn;}
        public Set<Integer> getSet(){return this.mIDs;}

        public boolean equals(Object o){
            if( !( o instanceof FastHash ) ){
                return false;
            }
            FastHash fh = (FastHash) o;
            return this.mColumn == fh.mColumn && this.mIDs.equals(fh.getSet());
        }

        public int hashCode(){
            return Integer.hashCode(this.mColumn) ^ mIDs.hashCode();
        }

    }

    public static class HashableStereoMoleculeSet implements Comparable<HashableStereoMoleculeSet>{

        private List<StereoMolecule> mMolecules = null;

        private List<StereoMolecule> mMoleculesWithIDCode = null;

        public HashableStereoMoleculeSet( Collection<StereoMolecule> molecules ){
            mMolecules = new ArrayList<>(molecules);

            List<StereoMolecule> molecules2 = mMolecules.stream().filter( mi -> mi.getIDCode() != null ).collect(Collectors.toList());
            molecules2.sort( (x,y) -> x.getIDCode().compareTo(y.getIDCode()));
            mMoleculesWithIDCode = molecules2;
        }

        public List<StereoMolecule> getMolecules(){
            return mMolecules;
        }

        public List<StereoMolecule> getMoleculesWithIDCode(){
            return mMoleculesWithIDCode;
        }

        @Override
        public int compareTo(HashableStereoMoleculeSet o) {
            return 0;
        }

        @Override
        public int hashCode(){
            return mMoleculesWithIDCode.stream().map(m -> m.getIDCode()).reduce("",(x,y) -> x+y ).hashCode();
        }

        @Override
        public boolean equals(Object o){
            if(!(o instanceof HashableStereoMoleculeSet)){
                return false;
            }
            HashableStereoMoleculeSet hsms = (HashableStereoMoleculeSet) o;

            if(hsms.getMoleculesWithIDCode().size() != getMoleculesWithIDCode().size()){
                return false;
            }

            boolean equal = true;
            for(int zi=0;zi<getMoleculesWithIDCode().size();zi++){
                equal = getMoleculesWithIDCode().get(zi).getIDCode().equals( hsms.getMoleculesWithIDCode().get(zi).getIDCode() );
                if(!equal){break;}
            }
            return equal;
        }



    }

}
