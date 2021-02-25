package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.VisualizationColor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class StructurePanel extends AbstractCardSurfacePanel {

    public final static String NAME = "Structure";

    private int mColumn = -1;

    @Override
    public void initalizeCardPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> records, int[] columns, int w, int h) {

    }

    @Override
    public void drawPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, CompoundRecord rec, int columns[], int w, int h) {


        if(false) {
            g.setColor(Color.orange.darker());
            g.fillRect((int) (0.1 * (double) w), (int) (0.1 * (double) h), (int) (0.8 * (double) w), (int) (0.8 * (double) h));
            g.setColor(Color.BLUE.darker());
            g.drawString("<<!!!TESTRXN!!!>>", (int) (0.1 * w), (int) (0.4 * h));
        }


        // Color the background:
        if(conf.getColorHandler().hasColorAssigned(columns[0], CompoundTableColorHandler.BACKGROUND)){
            Color bgColor = conf.getColorHandler().getVisualizationColor(columns[0],CompoundTableColorHandler.BACKGROUND).getColorForBackground(rec);
            g.setColor( bgColor );
            g.fillRect( 0,0,w,h );
        }


        // Draw the structure
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);


        StereoMolecule molContainer = new StereoMolecule();
        StereoMolecule mol = null;

        if(model!=null && columns[0] != -1) {
            try {
                //mol = model.getChemicalStructure( rec, column, CompoundTableModel.ATOM_COLOR_MODE_ALL, molContainer);
                mol = model.getChemicalStructure( rec, columns[0], CompoundTableModel.ATOM_COLOR_MODE_ALL, molContainer);
            }
            catch(Exception e){
                //System.out.println("StructurePanel Problem while drawing");
                //System.out.println(e);
            }
        }

        if(mol!=null) {
            Depictor2D depictor = new Depictor2D(mol);


            if (!conf.getColorHandler().hasColorAssigned(columns[0], CompoundTableColorHandler.FOREGROUND)) {
                Color fg = Color.black;
                Color bg = conf.getColorHandler().getVisualizationColor(columns[0], CompoundTableColorHandler.BACKGROUND).getColorForBackground(rec);
                depictor.setForegroundColor(fg, bg);
            }
            else {
                Color fg = conf.getColorHandler().getVisualizationColor(columns[0],CompoundTableColorHandler.FOREGROUND).getColorForForeground(rec);
                Color bg = conf.getColorHandler().getVisualizationColor(columns[0], CompoundTableColorHandler.BACKGROUND).getColorForBackground(rec);
                depictor.setForegroundColor(fg, bg);
                depictor.setOverruleColor(fg,bg);
            }

            //depictor.setOverruleColor(fg,bg);
            depictor.validateView(g, new Rectangle2D.Double(2, 2, w-4, h-4), AbstractDepictor.cModeInflateToMaxAVBL);
            depictor.paint(g);
        }
        else{
            int xycnt = 0;
            int rect_width = 40;
            for(int xx =0;xx<w-rect_width;xx+=rect_width ) {
                for (int yy = 0; yy < h - rect_width; yy+=rect_width) {
                    if (xycnt % 2 == 0) {
                        g.setColor(Color.black.brighter().brighter().brighter());
                    } else {
                        g.setColor(Color.orange.darker().darker().darker().darker());
                    }
                    //g.fillRect(xx,yy,rect_width,rect_width);
                    xycnt++;
                }
            }
        }


//        if (mDisplayMol != null && mDisplayMol.getAllAtoms() != 0) {
//
//            mDepictor.setDisplayMode(mDisplayMode);
//            mDepictor.setAtomText(mAtomText);
//
//            //if (!isEnabled())
//            //    mDepictor.setOverruleColor(ColorHelper.getContrastColor(Color.GRAY, getBackground()), getBackground());
//            //else
//            //    mDepictor.setForegroundColor(getForeground(), getBackground());
//
//            int avbl = HiDPIHelper.scale(AbstractDepictor.cOptAvBondLen);
//            mDepictor.validateView(g, new Rectangle2D.Double(insets.left, insets.top, theSize.width,theSize.height),
//                    AbstractDepictor.cModeInflateToMaxAVBL | mChiralTextPosition | avbl);
//            mDepictor.paint(g);
//        }

    }


    @Override
    public String getName() {
        return this.NAME;
    }


    @Override
    public JComponent getLabel() {
        return new JLabel("STRUCTURE!!");
    }

    @Override
    public JPanel getConfigDialog() {
        return new JConfigPanel();
    }


    public static class JConfigPanel extends JPanel implements ConfigurableDialog {

        public JConfigPanel(){
            this.setBackground(Color.white);
            this.add(new JLabel("STRUCTURE --> NO CONFIG"));
        }

        @Override
        public void setConfiguration(Properties p) {

        }

        @Override
        public Properties getConfiguration() {
            return null;
        }

        @Override
        public Properties getDefaultConfiguration() {
            return new Properties();
        }
    }

    @Override
    public int getNumberOfDataColumnsRequired() {
        return 1;
    }

    @Override
    public boolean canHandleColumnForSlot(CompoundTableModel model, int slot, int column) {
        return model.isColumnTypeStructure(column);
    }

    @Override
    public void setColumns(int columns[]) {
        if (columns.length != 1) {
            System.out.println("StructurePanel : Wrong number of data columns supplied! " + columns.length + " instead of " + 1);
        }
        this.mColumn = columns[0];
    }

    @Override
    public int[] getColumns() {
        return new int[]{ this.mColumn };
    }


    //@TODO implement..proposeCorrespondingStackSurfacePanel
    @Override
    public List<StackSurfacePanelAndColumnsAndPosition> proposeCorrespondingStackSurfacePanel( int columns[] , double relPos, double relHeight ) {
        StackChemStructurePanel scsp = new StackChemStructurePanel();
        scsp.setColumns( new int[]{this.mColumn} );
        List<StackSurfacePanelAndColumnsAndPosition> plist = new ArrayList<>();
        plist.add( new StackSurfacePanelAndColumnsAndPosition(scsp,new int[]{columns[0]},relPos,relHeight));
        return plist;
    }


}
