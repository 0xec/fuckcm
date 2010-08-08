package com.xec.fuckcm.services;

import java.io.IOException;
import java.net.DatagramSocket;
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
	public DatagramSocket srvDNSSocket = null;

	private TunnelSocket tunnelSocket = null;
	private DNSService dnsService = null;

	Config preConfig = new Config(this);

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

		try {

			int status = preConfig.getInt(Common.ServiceStatus,
					Common.SERVICE_STOPPED);
			if (status == Common.SERVICE_STOPPED) {

				Stop();
				return;
			}

			if (srvTunnelSocket != null)
				srvTunnelSocket.close();

			if (srvDNSSocket != null)
				srvDNSSocket.close();

			if (tunnelSocket != null)
				tunnelSocket.CloseAll();

			/*
			 * TCP服务
			 */

			// 创建监听套接字
			srvTunnelSocket = new ServerSocket(Common.SERVICE_PORT);

			// 启动监听服务
			tunnelSocket = new TunnelSocket(srvTunnelSocket);
			tunnelSocket.start();

			/*
			 * DNS服务
			 */
			// 这个有点BUG,暂时停了
			srvDNSSocket = new DatagramSocket(Common.SERVICE_DNSPORT);

			dnsService = new DNSService(srvDNSSocket);
			dnsService.start();

			// 启动IP转向
			Common.rootCMD("dmesg -c"); // 清空记录，以便提高查找时的速度
			EnableIPForward();
			CleanIPTablesRules();
			SetIPTablesRules();

			Log.i(Common.TAG, "Service Started...");

		} catch (IOException e) {
			Log.e(Common.TAG, "Create Tunnel Socket Error", e);
		} catch (Exception e) {
			Log.e(Common.TAG, "Create Tunnel Socket Exception", e);
		}
	}

	@Override
	public void onDestroy() {

		Stop();
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

	public void Stop() {

		Log.d(Common.TAG, "Service Stop entry");

		// 关闭IP转向
		DisableIPForward(); // 关闭IpForward
		CleanIPTablesRules(); // 清除规则

		try {
			tunnelSocket.CloseAll();
			dnsService.CloseAll();

			preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_STOPPED);

		} catch (Exception e) {
			Log.e(Common.TAG, "Service Stop Exception", e);
		}
	}
}
