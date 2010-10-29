package com.xec.fuckcm.receiver;

import com.xec.fuckcm.Config;
import com.xec.fuckcm.common.Common;
import com.xec.fuckcm.services.fuckcmServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ConnectStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Config preConfig = new Config(context);
		int status = preConfig.getInt(Common.ServiceStatus,
				Common.SERVICE_STOPPED);

		if (status == Common.SERVICE_STOPPED)
			return;

		String apnNameString = Common.getAPNName(context);

		Log.d(Common.TAG, "recv apn name:" + apnNameString);
		
		
		//
		//	还是去掉这个，好像打电话会有影响
		//
		//if (apnNameString.equals("none"))
		//	return;

		if (apnNameString.equals("cmwap")) {

			// Log.d(Common.TAG, "broadcast receiver start");
			new RunThread(context, true).start();
		} else {

			Log.d(Common.TAG, "broadcast receiver stop");
			new RunThread(context, false).start();
		}
	}

	class RunThread extends Thread {
		public Context context = null;
		public Boolean bRun = false;

		public RunThread(Context ctx, Boolean bRun) {
			this.context = ctx;
			this.bRun = bRun;
		}

		@Override
		public void run() {
			Intent intent0 = new Intent(this.context, fuckcmServices.class);
			if (this.bRun) {

				intent0.putExtra("action", Common.SERVICE_RUNING);
				context.startService(intent0);
			} else {

				intent0.putExtra("action", Common.SERVICE_STOPPED);
				context.startService(intent0);
			}
		}
	}
}
