package com.actelion.research.table.view.card.tools;

import com.actelion.research.table.view.card.cardsurface.gui.JMyOptionsTable;


public final class IntOrderedPair{
    public final int A;
    public final int B;
    private final int mHash;
    public IntOrderedPair(int a, int b){
        this.A = a;
        this.B = b;
        mHash  = Long.hashCode( ((((long)this.A) <<32) | ((long)this.B)) );
    }
    @Override
    public boolean equals(Object o){
        if(!(o instanceof IntOrderedPair)){return false;}
        IntOrderedPair iop = (IntOrderedPair) o;
        return (iop.A == this.A) && (iop.B == this.B);
    }
    @Override
    public int hashCode(){
            return this.mHash;
        }
}
