package com.actelion.research.table.view.card.cardsurface;

import java.util.List;

public class DefaultStackConfigurationProposer implements StackConfigurationProposerInterface {


    /**
     * Loops over slots and creates the corresponding stack slots.
     *
     * @param cdc
     * @return
     */
    @Override
    public StackDrawingConfig proposeStackConfigurationFromCardConfiguration(CardDrawingConfig cdc) {

        StackDrawingConfig sdc = new StackDrawingConfig(cdc.getColorHandler());

        for( CardDrawingConfig.CardSurfaceSlot csc : cdc.getSlots()){
            List<AbstractCardSurfacePanel.StackSurfacePanelAndColumnsAndPosition> proposedPanels = csc.getSurfacePanel().proposeCorrespondingStackSurfacePanel( csc.getColumns() , csc.getRelativePosY() , csc.getRelativeHeight() );
            for( AbstractCardSurfacePanel.StackSurfacePanelAndColumnsAndPosition pp : proposedPanels ){
                // NOTE: We have to manuall set the slots from the
                sdc.getSlots().add( new StackDrawingConfig.StackSurfaceSlot( pp.panel ,  pp.relativePosition, pp.relativeHeight));
            }
        }
        return sdc;
    }
}
