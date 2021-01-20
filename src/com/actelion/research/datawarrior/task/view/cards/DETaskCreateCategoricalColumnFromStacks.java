package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DETaskCreateCategoricalColumnFromStacks extends ConfigurableTask {

    public static final String TASK_NAME = "Create Categorical Column From Stacks";

    private static final String KEY_COLUMN_NAME = "columnName";

    private JCardPane mCardPane;

    private DataWarriorLink mLink;

    private DETaskCreateCategoricalColumnFromStacks.JDialogPanel jDialogPanel = new DETaskCreateCategoricalColumnFromStacks.JDialogPanel();


    public DETaskCreateCategoricalColumnFromStacks(Frame owner, JCardPane cardPane, DataWarriorLink dwlink) {
        super(owner,false);

        mLink = dwlink;
        mCardPane = cardPane;
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
//        String col_name = configuration.getProperty(KEY_COLUMN_NAME);
//        boolean name_exists = mLink.getCTM().findColumn(col_name) >= 0;
//        return name_exists;
    }

    @Override
    public void runTask(Properties configuration) {
        String col_name = configuration.getProperty(KEY_COLUMN_NAME);

        int col_idx = mLink.getCTM().addNewColumns(new String[]{col_name});

        // loop over card elements..
        List<CardElement> all_elements = mCardPane.getCardPaneModel().getAllElements();
        List<CompoundRecord> no_category = new ArrayList<>();

        for( CardElement ce : all_elements ) {
            boolean stackname_assigned = false;
            if(ce.getStackName()!=null) {
                if(!ce.getStackName().equals("")){
                    stackname_assigned = true;
                }
            }
            if(stackname_assigned) {
                for( CompoundRecord cr : ce.getAllRecords() ) {
                    mLink.getCTM().setTotalValueAt( ce.getStackName() , cr.getID(), col_idx);
                }
            }
            else{
                no_category.addAll(ce.getAllRecords());
            }
        }

        for(CompoundRecord cri : no_category) {
            mLink.getCTM().setTotalValueAt( "" , cri.getID() , col_idx);
        }

        mLink.getCTM().finalizeNewColumns(col_idx, null);

    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }

    @Override
    public JComponent createDialogContent() {
        return jDialogPanel;
    }

    @Override
    public Properties getDialogConfiguration() {
        Properties p = new Properties();
        p.setProperty( KEY_COLUMN_NAME , jDialogPanel.getNewColumnName() );

        return p;
    }

    @Override
    public void setDialogConfiguration(Properties configuration) {
        String col_name = configuration.getProperty(KEY_COLUMN_NAME);
        if(col_name!=null) {
            jDialogPanel.setNewColumnName(col_name);
        }
    }

    @Override
    public void setDialogConfigurationToDefault() {

    }

    class JDialogPanel extends JPanel {
        private JTextField jTextField_New = new JTextField();

        public JDialogPanel() {
//            jTextField_Old.setText(mSelectedStackName);
            init();
        }

//        public void setOldStackName(String stackname) {
//            jTextField_Old.setText(stackname);
//        }

        public void setNewColumnName(String colName) {
            jTextField_New.setText(colName);
        }

        public String getNewColumnName() {
            return jTextField_New.getText();
        }

        private void init() {
            double pref = TableLayout.PREFERRED;
            double p4   = HiDPIHelper.scale(4);
            double p8   = HiDPIHelper.scale(8);

            //jTextField_Old.setColumns(24);
            jTextField_New.setColumns(24);

            //jTextField_Old.setEditable(false);
            jTextField_New.setEditable(true);

            double layout[][] = new double[][] { { p4,pref,p8,pref,p4 } , {p4,pref,p4} };
            this.setLayout(new TableLayout(layout));

            this.add(new JLabel("Column Name "),"1,1");
            this.add(jTextField_New,"3,1");

        }
    }

}
