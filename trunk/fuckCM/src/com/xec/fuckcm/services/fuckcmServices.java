package com.xec.fuckcm.services;

import java.io.IOException;
import java.net.ServerSocket;

import com.xec.fuckcm.Config;
import com.xec.fuckcm.common.Common;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class fuckcmServices extends Service {

	// 隧道端口
	public ServerSocket srvTunnelSocket = null;
	TunnelSocket tunnelSocket = null;
	Config preConfig = new Config(this);

	// private ArrayList<TunnelSocket> TunnelServices = new
	// ArrayList<TunnelSocket>();

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
		
		int status = preConfig.getInt(Common.ServiceStatus, Common.SERVICE_STOPPED);
		if (status == Common.SERVICE_RUNING)
			return;

		try {

			if (srvTunnelSocket != null)
				srvTunnelSocket.close();

			if (tunnelSocket != null)
				tunnelSocket.CloseAll();

			// 创建监听套接字
			srvTunnelSocket = new ServerSocket(Common.SERVICE_PORT);

			//	启动监听服务
			tunnelSocket = new TunnelSocket(srvTunnelSocket);
			tunnelSocket.start();

		} catch (IOException e) {
			Log.e(Common.TAG, "Create Tunnel Socket Error", e);
		}

		// 启动IP转向
		Common.rootCMD("dmesg -c"); // 清空记录，以便提高查找时的速度
		EnableIPForward();
		CleanIPTablesRules();
		SetIPTablesRules();

		Log.i(Common.TAG, "Service Started...");
		
		preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_RUNING);
	}

	@Override
	public void onDestroy() {

		Log.d(Common.TAG, "Service Destroy entry");
		
		// 关闭IP转向
		DisableIPForward();		//	关闭IpForward
		CleanIPTablesRules();	//	清除规则

		tunnelSocket.CloseAll();

		try {
			srvTunnelSocket.close();
			
			preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_STOPPED);

		} catch (IOException e) {
			Log.e(Common.TAG, "Service Destroy Error", e);
		}
	}

	// 打开Ipforward
	public void EnableIPForward() {

		Common.rootCMD(Common.enableIPForward);
	}

	// 关闭IpForward
	public void DisableIPForward() {

		Common.rootCMD(Common.disableIPForward);
	}

	// 设置 IPTables
	public void SetIPTablesRules() {

		for (String rule : Common.ipTable_command) {
			try {
				Common.rootCMD(rule);

			} catch (Exception e) {
				Log.e(Common.TAG, e.getLocalizedMessage());
			}
		}
	}

	// 清空IPTables规则
	public void CleanIPTablesRules() {

		Common.rootCMD(Common.cleanIPTables);
	}
}
