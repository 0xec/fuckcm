package com.xec.fuckcm.services;

import com.xec.fuckcm.common.Common;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class fuckcmServices extends Service {

	@Override
	public IBinder onBind(Intent arg0) {

		Log.d(Common.TAG, "service on bind");
		return null;
	}

	@Override
	public void onCreate() {

		Log.d(Common.TAG, "service on create");
	}

	@Override
	public void onStart(Intent intent, int startId) {

		Log.d(Common.TAG, "service on Start");
	}

}
