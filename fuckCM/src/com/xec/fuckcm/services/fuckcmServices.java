package com.xec.fuckcm.services;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import com.xec.fuckcm.Config;
import com.xec.fuckcm.R;
import com.xec.fuckcm.mainActivity;
import com.xec.fuckcm.common.Common;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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

			// 如果保存的状态是停止，则停止服务
			int status = preConfig.getInt(Common.ServiceStatus,
					Common.SERVICE_STOPPED);
			if (status == Common.SERVICE_STOPPED && isRuning) {

				Stop();
				return;
			}
			
			if (isRuning) {
				
				return;
			}
			
			Log.d(Common.TAG, "service on Start");
			
			MountFileSystem();
			
			EnableIPForward();

			// 如果套接字已存在，则关闭套接字
			if (tunnelSocket != null)
				tunnelSocket.CloseAll();
			
			if (dnsService != null)
				dnsService.CloseAll();
			
			if (srvTunnelSocket != null)
				srvTunnelSocket.close();

			if (srvDNSSocket != null)
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
			// 这个有点BUG，暂时不起作用，以后找原因
			srvDNSSocket = new DatagramSocket(Common.SERVICE_DNSPORT);

			dnsService = new DNSService(srvDNSSocket);
			dnsService.start();

			// 启动IP转向
			Common.rootCMD("dmesg -c"); // 清空记录，以便提高查找时的速度
			CleanIPTablesRules();
			SetIPTablesRules();

			Log.i(Common.TAG, "Service Started...");

			PostUIMessage(getString(R.string.START_SUCCESS));
			PostNotificationMessage(R.drawable.icon,
					getString(R.string.app_name),
					getString(R.string.START_SUCCESS));
			
			isRuning = true;

		} catch (IOException e) {
			Log.e(Common.TAG, "Create Tunnel Socket Error", e);
			PostUIMessage(getString(R.string.START_EXCEPTION));
		} catch (Exception e) {
			Log.e(Common.TAG, "Create Tunnel Socket Exception", e);
			PostUIMessage(getString(R.string.START_EXCEPTION));
		}
	}

	@Override
	public void onDestroy() {

		Stop();
		UnmountFileSystem();
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
		
		UnmountFileSystem();

		// 关闭IP转向
		CleanIPTablesRules(); // 清除规则
		DisableIPForward(); // 关闭IpForward

		//	关闭套接字服务
		tunnelSocket.CloseAll();
		dnsService.CloseAll();

		preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_STOPPED);

		Log.i(Common.TAG, "Service Stop Success");
		PostUIMessage(getString(R.string.STOP_SUCCESS));
		CleanNotificationMessage();
		
		isRuning = false;

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
	
	public void MountFileSystem() {
		
		Common.rootCMD("mkdir /sdcard/fuckcm_etc");
		Common.rootCMD("mount /system/etc /sdcard/fuckcm_etc");
		Common.rootCMD("mount -o rw,remount /sdcard/fuckcm_etc");
	}
	
	public void UnmountFileSystem() {
		
		Common.rootCMD("umount /sdcard/fuckcm_etc");
	}
}
