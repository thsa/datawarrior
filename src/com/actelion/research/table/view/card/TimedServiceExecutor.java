package com.actelion.research.table.view.card;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides a service to enable fast cached access to some object of type T which requires occasional recomputation.
 * Recomputation takes place in a separate thread, and recomputation only happens after a specific minimial time
 * interval.
 *
 *
 *
 * @param <T>
 */
public class TimedServiceExecutor<T> {


    private T mComputedObject = null;

    private RecomputeTask mTask = null;

    private Lock mLockComputation = new ReentrantLock();

    private long mTimestampLastRecomputation = 0;

    private volatile boolean mRecomputationScheduled = false;

    private volatile boolean mSubsequentRecomputationRequested = false;

    private int mTimeIntervalMilliseconds = 10;


    public void setTimeIntervalMilliseconds(int ms){
        this.mTimeIntervalMilliseconds = ms;
    }

    public int getTimeIntervalMilliseconds(){return this.mTimeIntervalMilliseconds;}

    public TimedServiceExecutor( RecomputeTask task ){
        this.mTask = task;

        // to initialize we have to compute an initial version of the object..

        T initialObject = (T) task.compute(null);
        try{
            mLockComputation.tryLock(1000, TimeUnit.MILLISECONDS);
        }
        catch(Exception e){
            System.out.println("WARNING: TimedServiceExecutor::Contructor failed to acquire lock!!");
        }
        mComputedObject = initialObject;
        mLockComputation.unlock();
    }

    public void requestRecomputation(){
        if(mRecomputationScheduled){
            mSubsequentRecomputationRequested = true;
            return;
        }

        Thread doRecomputation = new Thread( new ScheduledRecomputation() );
        doRecomputation.start();
    }

    public T getComputedObject(){
        requestRecomputation();
        try{
            mLockComputation.tryLock(1000, TimeUnit.MILLISECONDS);
        }
        catch(Exception e){
            System.out.println("WARNING: TimedServiceExecutor::getComputedObject failed to acquire lock!!");
            T returnObject = mComputedObject;
            return returnObject;
        }
        T returnObject = mComputedObject;
        mLockComputation.unlock();

        return returnObject;
    }

    class ScheduledRecomputation implements Runnable{

        @Override
        public void run() {
            mRecomputationScheduled = true;
            try{
                Thread.sleep( Math.max(0, mTimestampLastRecomputation + mTimeIntervalMilliseconds - System.currentTimeMillis() ) );
            }
            catch(Exception e){}

            // recompute new object:
            T newObject = (T) mTask.compute(mComputedObject);

            // grab the lock and replace cached object
            try {
                mLockComputation.tryLock(1000, TimeUnit.MILLISECONDS);
            }
            catch(Exception e){
                // what to do now?
                System.out.println("WARNING: TimedServiceExecutor::ScheduledRecomputation::run failed to acquire lock!!");
                return;
            }

            mComputedObject = newObject;

            // done, update timestamp and release lock
            mTimestampLastRecomputation = System.currentTimeMillis();
            mLockComputation.unlock();
            mRecomputationScheduled = false;
            if(mSubsequentRecomputationRequested) {
                requestRecomputation();
                mSubsequentRecomputationRequested = false;
            }
        }
    }

    /**
     * NOTE! When you implement this, you do NOT have to use the template, you can return any object.
     *
     * @param <T2>
     */
    public static abstract class RecomputeTask<T2> {
        public abstract T2 compute(T2 template);
    }

}
