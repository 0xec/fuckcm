package com.xec.fuckcm.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class Common {

	// 调试标志
	public static String TAG = "fuckCM";

	// 服务运行状态
	public static String ServiceStatus = "SERVICESTATUS";
	public static int SERVICE_STOPPED = 0; // 服务停止
	public static int SERVICE_RUNING = 1; // 服务运行

	public static String actionString = "com.xec.fuckcm.recv";

	// 工作参数
	public static int SERVICE_PORT = 48966; // 服务端工作端口
	public static int SERVICE_DNSPORT = 48953; // DNS工作端口

	// IPtables命令
	public static String enableIPForward = "echo 1 > /proc/sys/net/ipv4/ip_forward";
	public static String disableIPForward = "echo 0 > /proc/sys/net/ipv4/ip_forward";
	public static String ipTable_command[] = {
			"iptables -t nat -A OUTPUT  -o rmnet0  -p tcp -m multiport --destination ! 10.0.0.0/8 --destination-port ! "
					+ SERVICE_PORT
					+ " -j LOG --log-level info --log-prefix \"fuckCM \"",
			"iptables -t nat -A OUTPUT  -o rmnet0  -p tcp -m multiport --destination ! 10.0.0.0/8 --destination-port ! "
					+ SERVICE_PORT
					+ " -j DNAT  --to-destination 127.0.0.1:"
					+ SERVICE_PORT,
			"iptables -t nat -A OUTPUT  -o rmnet0  -p udp --dport 53  -j DNAT  --to-destination 127.0.0.1:"
					+ SERVICE_DNSPORT };

	public static String cleanIPTables = "iptables -F -t nat";

	// cmwap的代理服务器
	public static String proxyHost = "10.0.0.172";
	public static int proxyPort = 80;

	// User-Agent
	public static String strUserAgent = "Mozilla/5.0 (Linux; U; Android 1.0; en-us; generic) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2";

	/*
	 * 公用函数
	 */
	public static synchronized int rootCMD(String cmd) {
		int result = -1;
		DataOutputStream os = null;
		InputStream err = null;
		try {

			Process process = Runtime.getRuntime().exec("su");
			err = process.getErrorStream();
			BufferedReader bre = new BufferedReader(new InputStreamReader(err),
					1024 * 8);

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(cmd + " \n");
			os.flush();
			os.writeBytes("exit \n");
			os.flush();

			String resp;
			while ((resp = bre.readLine()) != null) {

				if (resp.equals(""))
					break;

				Log.d(TAG, resp);
			}

			result = process.waitFor();
			if (result == 0)
				Log.d(TAG, cmd + " exec success");
			else {
				Log.d(TAG, cmd + " exec with result " + result);
			}

			os.close();
			process.destroy();

		} catch (IOException e) {

			Log.e(TAG, "Failed to exec command", e);
		} catch (InterruptedException e) {

			Log.e(TAG, "Thread Exception error", e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
			}
		}

		return result;
	}

	public static synchronized int runCMD(String cmd, String param) {
		int result = -1;
		DataOutputStream os = null;
		InputStream err = null;
		try {

			Process process = Runtime.getRuntime().exec(cmd);
			err = process.getErrorStream();
			BufferedReader bre = new BufferedReader(new InputStreamReader(err),
					1024 * 8);

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(" " + param + "\n");
			os.flush();
			os.writeBytes("exit \n");
			os.flush();

			String resp;
			while ((resp = bre.readLine()) != null) {

				if (resp.equals(""))
					break;

				Log.e(TAG, resp);
			}

			InputStream out = process.getInputStream();
			BufferedReader outR = new BufferedReader(new InputStreamReader(out));
			String line = "";

			// 根据输出构建以源端口为key的地址表
			while ((line = outR.readLine()) != null) {

				Log.d(Common.TAG, line);
			}

			result = process.waitFor();
			if (result == 0)
				Log.d(TAG, "normal " + cmd + " exec success");
			else {
				Log.d(TAG, "normal " + cmd + " exec with result " + result);
			}

			os.close();
			process.destroy();

		} catch (IOException e) {

			Log.e(TAG, "Failed to exec normal command", e);
		} catch (InterruptedException e) {

			Log.e(TAG, "Thread Exception error", e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
			}
		}

		return result;
	}

	public static synchronized BufferedReader Rundmesg(String cmd, String param) {
		DataOutputStream os = null;
		try {

			Process process = Runtime.getRuntime().exec(cmd);
			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(" " + param + "\n");
			os.flush();
			os.writeBytes("exit \n");
			os.flush();

			InputStream out = process.getInputStream();
			BufferedReader outR = new BufferedReader(new InputStreamReader(out));
			
			class MyTimerTask extends TimerTask {
				
				public Process process;
				
				public MyTimerTask(Process proce) {
					this.process = proce;
				}
				
				public void run() {
					
					Log.w(TAG, "Abort Process Run");
					process.destroy();
				}
			}
			
			Timer timer = new Timer();
			MyTimerTask myTimerTask = new MyTimerTask(process);
			timer.schedule(myTimerTask, 10000);

			// 根据输出构建以源端口为key的地址表

			int result = process.waitFor();
			if (result == 0) {
				Log.d(TAG, "normal " + cmd + " exec success");
			} else {
				Log.d(TAG, "normal " + cmd + " exec with result " + result);
			}
			
			timer.cancel();

			os.close();
			process.destroy();

			return outR;

		} catch (IOException e) {

			Log.e(TAG, "Failed to exec normal command", e);
		} catch (InterruptedException e) {

			Log.e(TAG, "Thread Exception error", e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {}
		}

		return null;
	}

	/**
	 * 延时函数
	 */
	public static void testSleep(long time) {
		try {
			TimeUnit.MILLISECONDS.sleep(time);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * 获取apn的名字
	 */
	public static String getAPNName(Context context) {

		String apnName = "";

		// 判断是否为wifi
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		if (networkInfo == null) {

			apnName = "none";
			return apnName;
		}

		if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			apnName = "wifi";
			return apnName;
		}

		if (networkInfo.getExtraInfo().length() > 0) {

			apnName = networkInfo.getExtraInfo();
			return apnName;
		}

		// // 查询 apn 的名字
		// Cursor cursor =
		// context.getContentResolver().query(Uri.parse("content://telephony/carriers"),
		// new String[] {"apn"}, "current=1", null, null);
		//
		// if (cursor != null) {
		//
		// try {
		// if (cursor.moveToFirst()) {
		// apnName = cursor.getString(0);
		// }
		// } catch (Exception e) {
		// Log.e(TAG, "Can not get Network info", e);
		// } finally {
		// cursor.close();
		// }
		// }

		return apnName;

	}
}
