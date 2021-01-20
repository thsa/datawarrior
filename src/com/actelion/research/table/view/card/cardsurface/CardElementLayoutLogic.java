package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.view.card.cardsurface.gui.JCardWizard2;
import com.actelion.research.table.view.card.tools.DataWarriorLink;

import java.util.List;
import java.util.stream.Collectors;

public class CardElementLayoutLogic {


    public static class CardAndStackConfig {
        public final CardDrawingConfig CDC;
        public final StackDrawingConfig SDC;

        public CardAndStackConfig(CardDrawingConfig cdc, StackDrawingConfig sdc) {
            CDC = cdc;
            SDC = sdc;
        }
    }


    private DataWarriorLink mLink;


    public CardElementLayoutLogic(DataWarriorLink link) {
        mLink = link;
    }


    public CardAndStackConfig createFullCardWizardConfig(List<JCardWizard2.ColumnWithType> columns) {

        JCardWizard2.CardWizardGlobalConfig cwgc = new JCardWizard2.CardWizardGlobalConfig(columns);

        JCardWizard2.CardWizardCardConfig  cwcc = new JCardWizard2.CardWizardCardConfig(Double.NaN,Double.NaN ,Double.NaN  , 0.75 , 0.0 , JCardWizard2.CardWizardCardConfig.MODE_DATA_SHOW[0]  );
        JCardWizard2.CardWizardStackConfig cwsc = new JCardWizard2.CardWizardStackConfig(Double.NaN,Double.NaN ,Double.NaN , 0.75 , 0.25 );

        //JCardWizard2.FullCardWizardConfig conf = new JCardWizard2;
        CardDrawingConfig   cdc = createCardConfiguration(cwgc, cwcc);
        StackDrawingConfig  sdc = createStackConfiguration(cwgc, cwsc);

        return new CardAndStackConfig(cdc,sdc);
    }


    public CardDrawingConfig createCardConfiguration(JCardWizard2.CardWizardGlobalConfig gconf , JCardWizard2.CardWizardCardConfig config ) {

        CardDrawingConfig cdc = new CardDrawingConfig( mLink.getTableColorHandler() );

        // compute layout:
        List<JCardWizard2.ColumnWithType> listCS = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
        List<JCardWizard2.ColumnWithType> listNu = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());

        double layout[] = computeLayout_Card( listCS.size() , listNu.size() );
        double h_structure  = layout[0];
        double h_numeric    = layout[1];


        // Add structure panel
        double currentPosition = 0.0;
        for( JCardWizard2.ColumnWithType cc : listCS ) {
            StructurePanel sp = new StructurePanel();
            sp.setColumns(new int[]{cc.column});
            cdc.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(cdc, sp, currentPosition, h_structure));
            currentPosition += h_structure;
        }

        // Create MultiDataPanel to show numeric stuff

        MultiDataPanel mdp = new MultiDataPanel();
        mdp.setNumberOfColumns(listNu.size());
        mdp.setColumns( listNu.stream().mapToInt(  nci -> nci.column).toArray() );
        mdp.setDivA( config.getDivA() ); mdp.setDivB( config.getDivB() );

        mdp.setDataShowMode( JCardWizard2.CardWizardCardConfig.MAP_DATA_SHOW_MODES.get( config.getMode() ) );
        cdc.getSlots().add( new CardDrawingConfig.CardSurfaceSlot( cdc , mdp, currentPosition , listNu.size() * h_numeric ) );

        return cdc;
    }

    public StackDrawingConfig createStackConfiguration(JCardWizard2.CardWizardGlobalConfig gconf , JCardWizard2.CardWizardStackConfig config ) {

        CardDrawingConfig cdc = new CardDrawingConfig( mLink.getTableColorHandler() );

        // compute layout:
        List<JCardWizard2.ColumnWithType> listCS = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
        List<JCardWizard2.ColumnWithType> listNu = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());

        double layout[] = computeLayout_Stack( listCS.size() , listNu.size() );
        double h_structure  = layout[0];
        double h_numeric    = layout[1];

        StackDrawingConfig sdc = new StackDrawingConfig(mLink.getTableColorHandler());

        // add themz:
        double currentPosition = 0.0;

        //NumCardsIndicatorPanel ssip = new NumCardsIndicatorPanel();
        NumCardsIndicatorPanel2 ssip = new NumCardsIndicatorPanel2();

        sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot(ssip,currentPosition,conf_stack_card_inicator_size));
        currentPosition += conf_stack_card_inicator_size;

        for( JCardWizard2.ColumnWithType cc : listCS ){
            if(cc.type== JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE) {
                StackChemStructurePanel sp = new StackChemStructurePanel();
                sp.setColumns(new int[]{cc.column});
                sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot( sp, currentPosition, h_structure));
                currentPosition += h_structure;
            }
        }

        // add multipanel for numerical values below:
        StackMultiDataPanel mdp = new StackMultiDataPanel();

        mdp.setDivA(config.getDivA()); mdp.setDivB(config.getDivB());
        mdp.setNumberOfColumns(listNu.size());
        mdp.setColumns( listNu.stream().mapToInt(  nci -> nci.column).toArray() );

        sdc.getSlots().add( new StackDrawingConfig.StackSurfaceSlot( mdp, currentPosition , listNu.size() * h_numeric ) );

        return sdc;
    }




    // values for card, not for stack obviously
    private final double conf_max_numerical_height     = 0.12;
    // values for card, not for stack obviously
    private final double conf_optimal_structure_height = 0.5;

    // additional conf values for stacks. Here, obviously we want numerical values to be as big as possible,
    // to show the density stuff
    private final double conf_stack_card_inicator_size       = 0.1;
    private final double conf_stack_optimal_numerical_height = 0.3;
    private final double conf_stack_optimal_structure_height = 0.25;


    /**
     * Computes the y layout for the card.
     *
     *
     * @param countStructures and countNumerical
     * @return double array, where first value is structure height, second value is numerical height
     */
    public double[] computeLayout_Card( int countStructures , int countNumerical ) {

//        int countStructures = (int) slots.stream().filter( si -> si.intValue() == 1 ).count();
//        int countNumerical  = (int) slots.stream().filter( si -> si.intValue() == 2 ).count();


        // this is what we want to compute..
        double final_height_structure  = Double.NaN;
        double final_height_numerical  = Double.NaN;

        // check if optimal layout is possible
        double optimal_space = countStructures * conf_optimal_structure_height + countNumerical * conf_max_numerical_height;

        // check what to do if we have space left.. that's pretty simple
        if(optimal_space <= 1.0){
            final_height_numerical = conf_max_numerical_height;
            if( countStructures >= 1 ){
                final_height_structure = ( 1.0 - ( countNumerical * conf_max_numerical_height ) ) / countStructures;
            }
        }

        // check what to do if we need too much space
        if(optimal_space > 1.0){

            double total_space = countStructures * conf_optimal_structure_height + countNumerical * conf_max_numerical_height;

            // divide "optimal" values by total_space..
            final_height_numerical = conf_max_numerical_height / total_space;
            final_height_structure = conf_optimal_structure_height / total_space;
        }

        return new double[]{ final_height_structure , final_height_numerical };
    }

    /**
     * Computes the y layout for the stack.
     *
     * @param countStructures , countNumerical
     * @return double array, where first value is structure height, second value is numerical height
     */
    public double[] computeLayout_Stack( int countStructures , int countNumerical ) {

//        int countStructures = (int) slots.stream().filter( si -> si.intValue() == 1 ).count();
//        int countNumerical  = (int) slots.stream().filter( si -> si.intValue() == 2 ).count();

        // this is what we want to compute..
        double final_height_structure  = Double.NaN;
        double final_height_numerical  = Double.NaN;

        // check if optimal layout is possible
        double available_space  = 1.0 - conf_stack_card_inicator_size;
        double optimal_space    = countStructures * conf_stack_optimal_structure_height + countNumerical * conf_stack_optimal_numerical_height;

        // check what to do if we have space left.. that's pretty simple
        if(optimal_space <= available_space){
            final_height_numerical = conf_stack_optimal_numerical_height * ( available_space / optimal_space )  ;
            final_height_structure = conf_stack_optimal_structure_height * ( available_space / optimal_space )  ;
        }

        // check what to do if we need too much space.. we actually do the same..
        if(optimal_space > 1.0){
            final_height_numerical = conf_stack_optimal_numerical_height * ( available_space / optimal_space )  ;
            final_height_structure = conf_stack_optimal_structure_height * ( available_space / optimal_space )  ;
//            double total_space = countStructures * conf_optimal_structure_height + countNumerical * conf_max_numerical_height;
//
//            // divide "optimal" values by total_space..
//            final_height_numerical = conf_max_numerical_height / total_space;
//            final_height_structure = conf_optimal_structure_height / total_space;
        }

        return new double[]{ final_height_structure , final_height_numerical };
    }




}
