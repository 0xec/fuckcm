package com.xec.fuckcm.services;

import java.io.IOException;
import java.net.ServerSocket;

import com.xec.fuckcm.common.Common;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class fuckcmServices extends Service {

	// 隧道端口
	public ServerSocket srvTunnelSocket = null;

	@Override
	public IBinder onBind(Intent arg0) {

		Log.d(Common.TAG, "service on bind");
		return null;
	}

	@Override
	public void onCreate() {

		Log.d(Common.TAG, "service on create");
		
		//	打开IP转向
		EnableIPForward();
	}

	@Override
	public void onStart(Intent intent, int startId) {

		Log.d(Common.TAG, "service on Start");

		try {

			// 启动隧道服务
			srvTunnelSocket = new ServerSocket(Common.SERVICE_PORT);
			
			new TunnelSocket(srvTunnelSocket).start();

		} catch (IOException e) {
			Log.e(Common.TAG, "Create Tunnel Socket Error", e);
		}
		
		
		//	启动IP转向
		SetIPTablesRules();
		
		Log.d(Common.TAG, "Set Iptables Rules");
	}
	
	@Override
	public void onDestroy() {
		
		//	关闭IP转向
		DisableIPForward();
		
		try {
			Log.d(Common.TAG, "Service Destroy entry");
			srvTunnelSocket.close();
			
		} catch (IOException e) {
			Log.e(Common.TAG, "Service Destroy Error", e);
		}
	}
	
	
	//	打开Ipforward
	public void EnableIPForward() {
		
		Common.rootCMD(Common.enableIPForward);
	}
	
	//	关闭IpForward
	public void DisableIPForward() {
	
		Common.rootCMD(Common.disableIPForward);
	}

	//	设置 IPTables
	public void SetIPTablesRules() {
		
		for (String rule : Common.ipTable_command) {
			try {
				Common.rootCMD(rule);

			} catch (Exception e) {
				Log.e(Common.TAG, e.getLocalizedMessage());
			}
		}
	}
	
	//	清空IPTables规则
	public void CleanIPTablesRules() {
		
		Common.rootCMD(Common.cleanIPTables);
	}
}
