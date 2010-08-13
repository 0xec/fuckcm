package com.xec.fuckcm.services;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import com.xec.fuckcm.R;
import com.xec.fuckcm.mainActivity;
import com.xec.fuckcm.common.Common;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class fuckcmServices extends Service {

	// 隧道端口
	public ServerSocket srvTunnelSocket = null;
	public DatagramSocket srvDNSSocket = null;

	private TunnelSocket tunnelSocket = null;
	private DNSService dnsService = null;

	public Boolean isRuning = false;

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

		try {
			int status = Common.SERVICE_STOPPED;

			// 如果保存的状态是停止，则停止服务
			Bundle bundle = intent.getExtras();
			if (bundle != null)
				status = bundle.getInt("action");

			if (status == Common.SERVICE_STOPPED) {

				if (isRuning)
					fuckcmStopService();

				return;
			}

			if (status == Common.SERVICE_RUNING) {

				if (isRuning)
					return;

				Log.d(Common.TAG, "service on Start");

				fuckcmStartService();
			}

		} catch (Exception e) {
			Log.e(Common.TAG, "Create Tunnel Socket Exception", e);
			PostUIMessage(e.getMessage());
		}
	}

	@Override
	public void onDestroy() {

		fuckcmStopService();
		UnmountFileSystem();
	}

	// 打开Ipforward
	public Boolean EnableIPForward() {

		return (0 == Common.rootCMD(Common.enableIPForward));
	}

	// 关闭IpForward
	public Boolean DisableIPForward() {

		return (0 == Common.rootCMD(Common.disableIPForward));
	}

	// 设置 IPTables
	public Boolean SetIPTablesRules() {

		Log.d(Common.TAG, "set ip tables");

		for (String rule : Common.ipTable_command) {
			try {
				int ret = Common.rootCMD(rule);
				if (ret != 0)
					return false;

				Common.testSleep(500);

			} catch (Exception e) {
				Log.e(Common.TAG, e.getLocalizedMessage());
			}
		}

		return true;
	}

	// 清空IPTables规则
	public Boolean CleanIPTablesRules() {

		Log.d(Common.TAG, "clean ip tables");
		if (0 != Common.rootCMD(Common.cleanIPTables))
			return false;
		else
			return true;
	}

	/**
	 * 传递一个消息给UI
	 */
	public void PostUIMessage(String strMessage) {

		try {

			Intent intent0 = new Intent(Common.actionString);
			intent0.putExtra("msg", strMessage);
			sendBroadcast(intent0);

		} catch (Exception e) {

			Log.e(Common.TAG, "Post UI Message Error", e);
		}
	}

	/**
	 * 显示状态栏通知
	 */
	public void PostNotificationMessage(int Icon, String Title, String Message) {

		try {

			NotificationManager notifiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Notification note = new Notification(Icon, Message,
					System.currentTimeMillis());

			// note.flags = Notification.FLAG_ONGOING_EVENT;
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, mainActivity.class), 0);
			note.setLatestEventInfo(this, Title, Message, pendingIntent);
			notifiManager.notify(0, note);

		} catch (Exception e) {

			Log.e(Common.TAG, "PostNotificationMessage error", e);
		}
	}

	/**
	 * 清除状态栏通知
	 */
	public void CleanNotificationMessage() {

		NotificationManager notifiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifiManager.cancel(0);
	}

	public Boolean MountFileSystem() {

		Common.runCMD("rmdir", "/sdcard/fuckcm_etc");
		Common.runCMD("mkdir", "/sdcard/fuckcm_etc");
		
		Common.testSleep(1000);
		int ret = Common.rootCMD("mount /system/etc /sdcard/fuckcm_etc");
		if (ret != 0)
			return false;
		
		Common.testSleep(1000);
		ret = Common.rootCMD("mount -o rw,remount /sdcard/fuckcm_etc");
		if (ret != 0) {
			ret = Common.rootCMD("mount -o rw,remount /mnt/sdcard/fuckcm_etc");
			if (ret != 0)
				return false;
		}

		return true;
	}

	public Boolean UnmountFileSystem() {

		int ret = Common.rootCMD("umount /sdcard/fuckcm_etc");
		if (ret == 0)
			return true;
		
		ret = Common.rootCMD("umount /mnt/sdcard/fuckcm_etc");
		if (ret == 0)
			return true;
		
	//	ret = Common.rootCMD("rmdir /sdcard/fuckcm_etc");
		ret = Common.runCMD("rmdir", "/sdcard/fuckcm_etc");
		if (ret == 0)
			return true;
		
		return false;
	}

	/**
	 * 启动服务
	 */
	public Boolean fuckcmStartService() {

		Boolean mountBoolean = false;
		Boolean ipRulesBoolean = false;

		try {

			// 挂载目录
			mountBoolean = MountFileSystem();

			// 打开IPForwad选项
			ipRulesBoolean = EnableIPForward();
			if (!ipRulesBoolean) {
				Exception myException = new Exception("Enable IP Forwad Error");
				throw myException;
			}

			// 如果套接字已存在，则关闭套接字
			if (tunnelSocket != null)
				tunnelSocket.CloseAll();

			if (dnsService != null)
				dnsService.CloseAll();

			if (srvTunnelSocket != null && !srvTunnelSocket.isClosed())
				srvTunnelSocket.close();

			if (srvDNSSocket != null && !srvDNSSocket.isClosed())
				srvDNSSocket.close();

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
			if (mountBoolean) {
				srvDNSSocket = new DatagramSocket(Common.SERVICE_DNSPORT);

				dnsService = new DNSService(srvDNSSocket);
				dnsService.start();
			} else {
				
				
			}

			// 清除规则
			ipRulesBoolean = CleanIPTablesRules();
			if (!ipRulesBoolean) {
				Exception myException = new Exception("Reset iptables Error");
				throw myException;
			}

			// 设置规则
			ipRulesBoolean = SetIPTablesRules();
			if (!ipRulesBoolean) {
				Exception myException = new Exception(
						"Set iptables rules Error");
				throw myException;
			}

			Log.i(Common.TAG, "Service Started...");

			PostUIMessage(getString(R.string.START_SUCCESS));
			PostNotificationMessage(R.drawable.icon,
					getString(R.string.app_name),
					getString(R.string.START_SUCCESS));

			isRuning = true;
			ipRulesBoolean = true;

		} catch (Exception e) {

			Log.e(Common.TAG, "start service error", e);
			PostUIMessage(e.getMessage());
		}

		return ipRulesBoolean;

	}

	public void fuckcmStopService() {

		Log.d(Common.TAG, "Service Stop entry");

		try {

			UnmountFileSystem();

			// 关闭IP转向
			CleanIPTablesRules(); // 清除规则
			DisableIPForward(); // 关闭IpForward

			// 关闭套接字服务
			try {
				tunnelSocket.CloseAll();
			} catch (Exception e) {
			}

			try {
				dnsService.CloseAll();
			} catch (Exception e) {
			}

		} catch (Exception e) {

			Log.e(Common.TAG, "close exception", e);
		} finally {

			Log.i(Common.TAG, "Service Stop Success");
			PostUIMessage(getString(R.string.STOP_SUCCESS));
			CleanNotificationMessage();

			isRuning = false;
		}

	}
}
