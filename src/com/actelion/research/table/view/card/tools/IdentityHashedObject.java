package com.actelion.research.table.view.card.tools;

public class IdentityHashedObject<T> {

    private T mObject = null;

    public IdentityHashedObject( T o){
        this.mObject = o;
    }

    public T get(){ return mObject; }

    @Override
    public boolean equals(Object o){
        if( ! (o instanceof IdentityHashedObject ) ){
            return false;
        }

        IdentityHashedObject iho = (IdentityHashedObject) o;

        Object o2 = iho.get();

        T t       = null;

        try{
            t = (T) o2;
        }
        catch(Exception e){return false;}
        if(t==null){return false;}

        return (t==mObject);
    }

    @Override
    public int hashCode(){
        return System.identityHashCode(mObject);
    }


}
