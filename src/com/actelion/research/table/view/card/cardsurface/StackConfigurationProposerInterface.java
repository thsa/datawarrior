package com.actelion.research.table.view.card.cardsurface;

public interface StackConfigurationProposerInterface {

    /**
     * Propose stack configuration based on card configuration.
     *
     * @param cdc
     * @return
     */
    public StackDrawingConfig proposeStackConfigurationFromCardConfiguration(CardDrawingConfig cdc);

}
