package com.kenvix.rconmanager.ui.base;

//https://stackoverflow.com/a/68395429

// used to replace the deprecated AsyncTask class
// by using thread pool executor instead
// functions almost as a drop in replacement

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AsyncTaskExecutor<Params,U,X> {
    public static final String TAG = "AsyncTaskRunner";

    private static final Executor THREAD_POOL_EXECUTOR =
            new ThreadPoolExecutor(20, 128, 1,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final AtomicBoolean mIsInterrupted = new AtomicBoolean(false);

    protected void onPreExecute(){}
    protected abstract Object doInBackground(Params... params) throws InterruptedException;

    protected void onPostExecute(X x){}

    protected void onCancelled(){}

    @SafeVarargs
    public final void executeAsync(Params... params) {
        THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                checkInterrupted();
                mHandler.post(this::onPreExecute);

                checkInterrupted();
                X x = (X) doInBackground(params);

                checkInterrupted();
                mHandler.post(()->onPostExecute(x));
            } catch (InterruptedException ex) {
                mHandler.post(this::onCancelled);
            } catch (Exception ex) {
                Log.e(TAG, "executeAsync: " + ex.getMessage() + "\n" );//+ Debug.getStackTrace(ex));
            }
        });
    }

    public void cancel(boolean mayInterruptIfRunning){
        setInterrupted(mayInterruptIfRunning);
    }

    public boolean isCancelled(){
        return isInterrupted();
    }

    protected void checkInterrupted() throws InterruptedException {
        if (isInterrupted()){
            throw new InterruptedException();
        }
    }

    protected boolean isInterrupted() {
        return mIsInterrupted.get();
    }

    protected void setInterrupted(boolean interrupted) {
        mIsInterrupted.set(interrupted);
    }
}