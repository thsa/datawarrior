package com.actelion.research.table.view.card;

import java.util.List;

public abstract class AbstractTimedBufferedService<T> {

    private Thread mThread;

    public void processWorkPackage(List<T> elements){
        mThread = new Thread(){
            public void run(){
                computeWorkPackage(elements);
                runAfterWorkPackageComputed();
            }
        };
        mThread.start();
    }
    public abstract void computeWorkPackage(List<T> elements);
    public abstract void runAfterWorkPackageComputed();


    public Thread getThread(){
        return mThread;
    }
}