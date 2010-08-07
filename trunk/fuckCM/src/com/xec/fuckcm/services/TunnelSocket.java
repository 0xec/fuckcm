package com.xec.fuckcm.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.xec.fuckcm.common.Common;
import android.util.Log;

public class TunnelSocket extends Thread {

	// 隧道监听套接字
	public ServerSocket srvTunnelSocket;
	
	//	保存连接状态，以备下次查找
	private static Hashtable<String, String> connReq = new Hashtable<String, String>();

	//	线程池
	private ExecutorService tunnelPool = Executors.newCachedThreadPool();
	
	public TunnelSocket(ServerSocket srvSkt) {

		this.srvTunnelSocket = srvSkt;
	}

	@Override
	public void run() {
		
		String dstHost;
		int srcPort;

		while (true) {

			try {

				Socket clientSocket = srvTunnelSocket.accept();

				Log.d(Common.TAG, "Client Accept...");

				clientSocket.setSoTimeout(120 * 1000);	//	操作超时120S
				
				srcPort = clientSocket.getPort();
				
				dstHost = getTarget(Integer.toString(srcPort));
				if (dstHost == null || dstHost.trim().equals("")) {
					Log.d(Common.TAG, "SPT:" + srcPort + " doesn't match");
					clientSocket.close();
					continue;
				} else {
					Log.d(Common.TAG, srcPort + "-------->" + dstHost);
				}
				

			} catch (IOException e) {

				Log.e(Common.TAG, "Tunnel Socket Error", e);
			}
		}
	}
	
	/**
	 * 根据源端口号，由dmesg找出iptables记录的目的地址
	 * 
	 * @param sourcePort
	 *            连接源端口号
	 * @return 目的地址，形式为 addr:port
	 */
	private synchronized String getTarget(String sourcePort) {
		String result = "";

		// 在表中查找已匹配项目
		if (connReq.containsKey(sourcePort)) {
			result = connReq.get(sourcePort);
			connReq.remove(sourcePort);
			return result;
		}

		final String command = "dmesg -c"; // 副作用未知

		DataOutputStream os = null;
		InputStream out = null;
		try {
			Process process = Runtime.getRuntime().exec("su");

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(command + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();

			int execResult = process.waitFor();
			if (execResult == 0)
				Log.d(Common.TAG, command + " exec success");
			else {
				Log.d(Common.TAG, command + " exec with result " + execResult);
			}

			out = process.getInputStream();
			BufferedReader outR = new BufferedReader(new InputStreamReader(out));
			String line = "";

			// 根据输出构建以源端口为key的地址表
			while ((line = outR.readLine()) != null) {
				
				Log.d(Common.TAG, line);

				boolean match = false;

				if (line.contains("fuckCM")) {
					String addr = "", destPort = "";
					String[] parmArr = line.split(" ");
					for (String parm : parmArr) {
						String trimParm = parm.trim();
						if (trimParm.startsWith("DST")) {
							addr = getValue(trimParm);
						}

						if (trimParm.startsWith("SPT")) {
							if (sourcePort.equals(getValue(trimParm)))
								match = true;
						}

						if (trimParm.startsWith("DPT")) {
							destPort = getValue(trimParm);
						}

					}

					if (match)
						result = addr + ":" + destPort;
					else
						connReq.put(addr, destPort);

				}
			}

			os.close();
			process.destroy();
		} catch (IOException e) {
			Log.e(Common.TAG, "Failed to exec command", e);
		} catch (InterruptedException e) {
			Log.e(Common.TAG, "thread error terminate", e);
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
	
	private String getValue(String org) {
		String result = "";
		result = org.substring(org.indexOf("=") + 1);
		return result;
	}
}
