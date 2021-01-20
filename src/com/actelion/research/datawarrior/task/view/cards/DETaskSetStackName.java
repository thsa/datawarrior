package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetStackName extends ConfigurableTask {

    public static final String TASK_NAME = "Set Stack Name";

    private static final String PROPERTY_STACK_DEFINING_RECORD_ID = "stackDefiningCompoundRecordID";
    private static final String PROPERTY_NEW_STACK_NAME = "newStackName";

    private JCardPane mCardPane;

    private DataWarriorLink mLink;

    private SetStackNamePanel jDialogPanel = new SetStackNamePanel();

    private int mStackDefiningCRID = -1;

    /**
     * Name of stack that should be renamed
     */
    private String mSelectedStackName = null;

    public DETaskSetStackName(Frame owner, JCardPane cardPane, DataWarriorLink dwlink, int stack_defining_crid) {
        super(owner,false);
        mCardPane = cardPane;
        this.mLink = dwlink;
        //this.mSelectedStackName = old_stack_name;
        this.mStackDefiningCRID = stack_defining_crid;
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
        // is always ok, will just get a "_1" postfix if it already exists.
        return true;
    }

    @Override
    public void runTask(Properties configuration) {
        int compound_record_id = Integer.parseInt(configuration.getProperty(PROPERTY_STACK_DEFINING_RECORD_ID) );
        String sn_new = configuration.getProperty(PROPERTY_NEW_STACK_NAME);
        mCardPane.getCardPaneModel().renameStack( compound_record_id , sn_new);
    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }

    @Override
    public JComponent createDialogContent() {
        //SetStackNamePanel panel = new SetStackNamePanel();
        return jDialogPanel;
    }

    @Override
    public Properties getDialogConfiguration() {
        Properties p = new Properties();
        p.put( PROPERTY_STACK_DEFINING_RECORD_ID , ""+mStackDefiningCRID );
        p.put( PROPERTY_NEW_STACK_NAME , jDialogPanel.getNewStackName());
        return p;
    }

    @Override
    public void setDialogConfiguration(Properties configuration) {
        //if(configuration.containsKey( PROPERTY_OLD_STACK_NAME )) { this.jDialogPanel.setOldStackName( (String) configuration.get(PROPERTY_OLD_STACK_NAME)); }
        if(configuration.containsKey( PROPERTY_NEW_STACK_NAME )) { this.jDialogPanel.setNewStackName( (String) configuration.get(PROPERTY_NEW_STACK_NAME)); }
    }

    @Override
    public void setDialogConfigurationToDefault() {

    }


    class SetStackNamePanel extends JPanel {
        private JTextField jTextField_New = new JTextField();

        public SetStackNamePanel() {
//            jTextField_Old.setText(mSelectedStackName);
            init();
        }

//        public void setOldStackName(String stackname) {
//            jTextField_Old.setText(stackname);
//        }

        public void setNewStackName(String stackname) {
            jTextField_New.setText(stackname);
        }

        public String getNewStackName() {
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

            this.add(new JLabel("Stackname "),"1,1");
            this.add(jTextField_New,"3,1");

//            double layout[][] = new double[][] { { p4,pref,p8,pref,p4 } , {p4,pref,p8,pref,p4} };
//            this.setLayout(new TableLayout(layout));
//            this.add(new JLabel("Old Name "),"1,1");
//            this.add(jTextField_Old,"3,1");
//            this.add(new JLabel("New Name "),"1,3");
//            this.add(jTextField_New,"3,3");
        }
    }

}
