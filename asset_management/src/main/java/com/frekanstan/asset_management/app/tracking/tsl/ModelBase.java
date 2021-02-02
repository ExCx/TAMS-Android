//----------------------------------------------------------------------------------------------
// Copyright (c) 2013 Technology Solutions UK Ltd. All rights reserved.
//----------------------------------------------------------------------------------------------

package com.frekanstan.asset_management.app.tracking.tsl;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import androidx.multidex.BuildConfig;

import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;

import java.util.Date;

public class ModelBase {
	private static final boolean D = BuildConfig.DEBUG;

	static final int BUSY_STATE_CHANGED_NOTIFICATION = 1;
	static final int MESSAGE_NOTIFICATION = 2;

	private Handler mHandler;
	private boolean mBusy;
	private Exception mException;
	private AsciiCommander mCommander;
	private AsyncTask<Void, Void, Void> mTaskRunner;
	private double mLastTaskExecutionDuration;

	private Date mTaskStartTime;

	ModelBase() {
		mCommander = null;
		mHandler = null;
		mBusy = false;
		mLastTaskExecutionDuration = -1.00;
	}

	public boolean isBusy() { return mBusy; }

	private void setBusy(boolean isBusy) {
		if (mBusy != isBusy) {
			mBusy = isBusy;
			if (mHandler != null)
	        	mHandler.sendMessage(mHandler.obtainMessage(BUSY_STATE_CHANGED_NOTIFICATION, isBusy));
		}
	}

	void sendMessageNotification(String message) {
		if (mHandler != null)
			mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_NOTIFICATION, message));
	}

	public boolean isTaskRunning() { return mTaskRunner != null; }

	void setCommander(AsciiCommander commander) { mCommander = commander; }

	public Handler getHandler() { return mHandler; }

	void setHandler(Handler handler) { mHandler = handler; }

	public Exception error() { return mException; }

	protected void setError(Exception e) { mException = e; }

	final double getTaskExecutionDuration() {
		if (mLastTaskExecutionDuration >= 0.0)
			return mLastTaskExecutionDuration;
		else
			return (new Date().getTime() - mTaskStartTime.getTime()) / 1000.0;
	}

	@SuppressLint("StaticFieldLeak")
	@TargetApi(11)
	/*
	 * Execute the given task
	 * 
	 * The busy state is notified to the client
	 * 
	 * Tasks should throw an exception to indicate (and return) error
	 * 
	 * @param task the Runnable task to be performed 
	 */
	void performTask(Runnable task) throws Exception {
		final Runnable rTask = task;

		if (mCommander == null)
			throw(new Exception("There is no AsciiCommander set for this model!"));
		else {
			if (mTaskRunner != null)
				throw(new Exception("Task is already running!"));
			else {
				mTaskRunner = new AsyncTask<Void, Void, Void>() {
					@Override
					protected void onPreExecute() {
						mLastTaskExecutionDuration = -1.0;
						mTaskStartTime = new Date();
					}

					protected Void doInBackground(Void... voids) {
						try {
							setBusy(true);
							mException = null;
							rTask.run();
						} catch (Exception e) {
							mException = e;
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						mTaskRunner = null;
						setBusy(false);
						Date finishTime = new Date();
						mLastTaskExecutionDuration = (finishTime.getTime() - mTaskStartTime.getTime()) / 1000.0;
						if(D) Log.i(getClass().getName(), String.format("Time taken (ms): %d %.2f", finishTime.getTime() - mTaskStartTime.getTime(), mLastTaskExecutionDuration));
					}
				};

				try {
					mTaskRunner.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[])null);
				} catch( Exception e ) {
					mException = e;
				}
			}
		}
	}

	AsciiCommander getCommander() { return mCommander; }
}
