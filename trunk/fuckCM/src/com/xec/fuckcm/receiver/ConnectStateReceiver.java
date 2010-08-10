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

		String apnNameString = Common.getAPNName(context);
		
		if (apnNameString.equals("cmwap")) {
			
	//		Log.d(Common.TAG, "broadcast receiver start");
			new RunThread(context, true).start();
		} else {
			
			Log.d(Common.TAG, "broadcast receiver stop");
			new RunThread(context, false).start();
		}
	}

	class RunThread extends Thread
	{
		public Context context = null;
		public Config preConfig = null;
		public Boolean bRun = false;
		
		public RunThread(Context ctx, Boolean bRun)
		{
			this.context = ctx;
			preConfig = new Config(ctx);
			this.bRun = bRun;
		}
		
		@Override
		public void run()
		{
			Intent intent0 = new Intent(this.context, fuckcmServices.class);
			if (this.bRun) {
				
				preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_RUNING);
				context.startService(intent0);
			} else {
				
				preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_STOPPED);
				context.stopService(intent0);
			}
		}
	}
}
