package com.android.gphonemanager.clear;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.android.gphonemanager.clear.CleanerService.ProcessorCompleteListener;

import android.util.Log;

public abstract class ProcessorBase implements RunnableFuture<Object> {

	private final static String TAG = "jingyi";
	
	protected volatile boolean mCanceled;
    protected volatile boolean mDone;
    protected int type;
	
    protected ProcessorCompleteListener mListener;
    
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return mCanceled;
	}

	@Override
	public boolean isCancelled() {
        if (mDone || mCanceled) {
            return false;
        }
        mCanceled = true;

        return true;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public boolean isDone() {
		return mDone;
	}

	@Override
	public Object get() throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException();
	}
	
	public abstract void doWork();

	@Override
	public void run() {
		// TODO Auto-generated method stub
        try {
            doWork();
        } finally {
//            Log.d(TAG, "[run]finish: type = " + getType() + ",mDone = " + mDone
//                    + ",thread id = " + Thread.currentThread().getId());
//            mDone = true;
//            if (mListener != null && !mCanceled) {
//                mListener.onProcessorCompleted(getType());
//            }
        }
	}
	
	protected void processorCompleted() {
      Log.d(TAG, "[run]finish: type = " + getType() + ",mDone = " + mDone
    		  + ",thread id = " + Thread.currentThread().getId());
      mDone = true;
      if (mListener != null && !mCanceled) {
    	  mListener.onProcessorCompleted(getType());
      }
	}
	
}
