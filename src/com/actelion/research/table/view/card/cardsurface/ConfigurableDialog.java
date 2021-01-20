package com.actelion.research.table.view.card.cardsurface;

import java.util.Properties;

public interface ConfigurableDialog {

    public void        setConfiguration(Properties p);
    public Properties  getConfiguration();
    public Properties  getDefaultConfiguration();

}
