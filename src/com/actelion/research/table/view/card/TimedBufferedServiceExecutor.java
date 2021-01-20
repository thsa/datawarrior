package com.actelion.research.table.view.card;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class TimedBufferedServiceExecutor<T> extends Thread {

    /**
     * Minimal execution interval in milliseconds
     */
    private int  mExecutionInterval      = 200;

    /**
     * Sleep interval when waiting for starting new executions
     */
    private int  mSleepInterval          = 50;

    /**
     * Timestamp of earliest (oldest) unprocessed request
     */
    private long mCurrentRequest  = -1;

    /**
     * Timestamp of last execution
     */
    private long mLastExecution   = -1;


    private AbstractTimedBufferedService<T> mService;

    private ConcurrentLinkedDeque<T> mQueuedElements = new ConcurrentLinkedDeque<>();


    public TimedBufferedServiceExecutor(AbstractTimedBufferedService<T> service){
        this.mService = service;
    }

    public void addRequests(List<T> elements){
        for(T e : elements) {
            mQueuedElements.push(e);
        }
        if(mCurrentRequest < 0){ mCurrentRequest = System.currentTimeMillis(); }
        else{  }
    }

    public void addRequest(T element){
        mQueuedElements.push(element);
        if(mCurrentRequest < 0){ mCurrentRequest = System.currentTimeMillis(); }
        else{  }
    }


    @Override
    public void run(){
        while(true){
            try{ Thread.sleep(mSleepInterval);} catch(Exception e){}
            if( mCurrentRequest + mExecutionInterval <= System.currentTimeMillis() ){
                try{mService.getThread().join(0);}catch(Exception e){}
                if(!this.mQueuedElements.isEmpty()) {
                    boolean threadNullOrTerminated = false;
                    threadNullOrTerminated = mService.getThread() == null;
                    if (!threadNullOrTerminated) {
                        threadNullOrTerminated = mService.getThread().getState() == State.TERMINATED;
                    }

                    if (threadNullOrTerminated) {
                        System.out.println("Process " + this.mQueuedElements.size() + " elements..");
                        // Start new execution:
                        this.mLastExecution = System.currentTimeMillis();
                        this.mCurrentRequest = -1;
                        List<T> eList = new ArrayList<>();
                        eList = mQueuedElements.stream().distinct().collect(Collectors.toList());
//                        Iterator<T> iterator = mQueuedElements.iterator();
//                        while (iterator.hasNext()) {
//                            eList.add(iterator.next());
//                        }

                        this.mService.processWorkPackage(eList);
                        mQueuedElements.clear();
                    }
                }
            }
        }
    }


}
