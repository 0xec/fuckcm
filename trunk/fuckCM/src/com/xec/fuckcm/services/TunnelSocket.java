package com.xec.fuckcm.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.xec.fuckcm.common.Common;
import android.util.Log;

public class TunnelSocket extends Thread {

	// 隧道监听套接字
	public ServerSocket srvTunnelSocket;
	public Boolean isRuning = false;

	// 保存连接状态，以备下次查找
	private static Hashtable<String, String> connReq = new Hashtable<String, String>();

	// 线程池
	private ExecutorService tunnelPool = Executors.newCachedThreadPool();

	Timer timer = new Timer();
	TimerTask timerTask = new TimerTask() {

		@Override
		public void run() {

			// 设置一个不存在的端口，清空日志，并将已存在的写入HASH表
			getTarget("99999");
		}
	};

	public TunnelSocket(ServerSocket srvSkt) {

		this.srvTunnelSocket = srvSkt;
		isRuning = true;

		//	root权限执行命令时会造成焦点的切换，貌似是android的问题
	//	timer.schedule(timerTask, 10000, 10000);
	}

	@Override
	public void run() {

		String dstHost;
		int srcPort;

		while (isRuning) {

			try {

				// 等待客户端连入..
				Socket clientSocket = srvTunnelSocket.accept();

				Log.d(Common.TAG, "Client Accept...");

				clientSocket.setSoTimeout(120 * 1000); // 操作超时120S

				// 获得连入的端口，用来在表中查找
				srcPort = clientSocket.getPort();

				Log.d(Common.TAG, "Find Remote Address...");

				// 查找原始的目标IP地址
				dstHost = getTarget(Integer.toString(srcPort).trim());

				if (dstHost == null || dstHost.trim().equals("")) {

					// 没有找到
					Log.e(Common.TAG, "SPT:" + srcPort + " doesn't match");
					clientSocket.close();
					continue;
				} else {

					Log.d(Common.TAG, srcPort + "-------->" + dstHost);

					//
					tunnelPool
							.execute(new ConnectSession(clientSocket, dstHost));
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

			Log.w(Common.TAG, "find target in cache key:" + sourcePort
					+ " value:" + result);

			return result;
		}

		final String command = "dmesg -c";

		DataOutputStream os = null;
		InputStream out = null;
		try {
			Process process = Runtime.getRuntime().exec("dmesg");

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(" -c \n");
			os.flush();
			os.writeBytes("exit \n");
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

				if (line.equals(""))
					break;

				boolean match = false;

				Log.d("logs", line);

				if (line.contains("fuckCM")) {

					// Log.d(Common.TAG, line);

					String addr = "", destPort = "", srcPort = "";
					String[] parmArr = line.split(" ");
					for (String parm : parmArr) {
						String trimParm = parm.trim();
						if (trimParm.startsWith("DST")) {
							addr = getValue(trimParm);
						}

						if (trimParm.startsWith("SPT")) {

							srcPort = getValue(trimParm);
							if (sourcePort.equals(srcPort))
								match = true;
						}

						if (trimParm.startsWith("DPT")) {
							destPort = getValue(trimParm);
						}

					}

					if (match) {

						result = addr + ":" + destPort;

						if (connReq.containsKey(sourcePort)) {
							connReq.remove(sourcePort);
						}

					} else {

						if (addr.length() > 0 && destPort.length() > 0
								&& srcPort.length() > 0) {

							String strAddr = addr + ":" + destPort;

							if (!connReq.contains(srcPort)) {

								connReq.put(srcPort, strAddr);

								Log.w(Common.TAG, "put in cache key:"
										+ srcPort + " value:" + strAddr);

							}
						}
					}

				} // end if
			} // end while

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
		try {
			result = org.substring(org.indexOf("=") + 1);
		} catch (Exception e) {
			Log.e(Common.TAG, "function getValue error", e);
		}
		return result;
	}

	public void CloseAll() {

		try {
			isRuning = false;
			tunnelPool.shutdownNow();

			if (!srvTunnelSocket.isClosed())
				srvTunnelSocket.close();

			timer.cancel();

		} catch (Exception e) {
			Log.e(Common.TAG, "function CloseAll error", e);
		}
	}
}
