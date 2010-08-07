package com.xec.fuckcm.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class Common {

	// 调试标志
	public static String TAG = "fuckCM";

	// 服务运行状态
	public static String ServiceStatus = "SERVICESTATUS";
	public static int SERVICE_STOPPED = 0; // 服务停止
	public static int SERVICE_RUNING = 1; // 服务运行

	// 工作参数
	public static int SERVICE_PORT = 58866; // 服务端工作端口

	// IPtables命令
	public static String enableIPForward = "echo 1 > /proc/sys/net/ipv4/ip_forward";
	public static String disableIPForward = "echo 1 > /proc/sys/net/ipv4/ip_forward";
	public static String ipTable_command[] = {
			"iptables -t nat -A OUTPUT  -o rmnet0  -p tcp -m multiport --destination ! 10.0.0.0/8 --destination-port ! 8866 -j LOG --log-level info --log-prefix \"fuckCM \"",
			"iptables -t nat -A OUTPUT  -o rmnet0  -p tcp -m multiport --destination ! 10.0.0.0/8 --destination-port ! 8866 -j DNAT  --to-destination 127.0.0.1:58866" };

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

			os.writeBytes(cmd + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();

			String resp;
			while ((resp = bre.readLine()) != null) {
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
			Log.e(TAG, "线程意外终止", e);
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
}
