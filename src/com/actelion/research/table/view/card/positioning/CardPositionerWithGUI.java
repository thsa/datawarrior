package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class CardPositionerWithGUI extends AbstractCardPositioner {




    private List<SortingConfigurationListener> mListeners = new ArrayList<>();

    public abstract JPanel getGUI();

    public abstract String getName();


    /**
     * Initializes the card sorter with a configuration created with .getConfiguration()
     *
     * @param p
     */
    public abstract void configure(Properties p);

    /**
     * Returns a Properties object which serializes the configuration of this card positioner.
     * To restore the card positioner, use the .configure(Properties p) function.
     *
     * @return
     */
    public abstract Properties getConfiguration();



    public void registerSortingConfigListener(SortingConfigurationListener listener ){
        mListeners.add(listener);
    }

    public void fireSortingConfigEvent(CardPositionerWithGUI source, String msg, boolean forceRepositioning){
        for(SortingConfigurationListener scl : mListeners){
            scl.sortingConfigurationChanged( new SortingConfigurationListener.SortingCofigurationEvent(source,msg,forceRepositioning));
        }
    }


}
