package com.xec.fuckcm.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.System;
import java.net.Socket;
import android.util.Log;

import com.xec.fuckcm.common.Common;

public class ConnectSession implements Runnable {

	public Socket proxySocket = null;
	public Socket clientSocket = null;
	public Boolean isConnected = false;
	long starTime = 0;
	DataInputStream clientInput, proxyInput;
	DataOutputStream clientOutput, proxyOutput;

	public ConnectSession(Socket clientSocket, // 连入的链接
			String strHost) // 目标地址
	{
		try {
			Log.i(Common.TAG, "Connect Sesion Initialize");
			
			starTime = System.currentTimeMillis(); // 记录用时
			this.clientSocket = clientSocket;
			
			clientInput = new DataInputStream(this.clientSocket.getInputStream());
			clientOutput = new DataOutputStream(this.clientSocket.getOutputStream());
			
			//	连接到代理服务器
			isConnected = ConnectTarget(strHost);
			
		} catch (Exception e) {
			Log.e(Common.TAG, "function ConnectSession error", e);
		}
	}

	@Override
	public void run() {
		
		if (this.isConnected) {

			// 重定向数据
			try {
				RediectData go = new RediectData(clientSocket, proxySocket, clientInput, proxyOutput);
				RediectData come = new RediectData(clientSocket, proxySocket, proxyInput, clientOutput);
				go.start();
				come.start();
				
				Log.i(Common.TAG, "Create Tunnel Succcess use time:" + (System.currentTimeMillis() - starTime) / 1000);
				Log.i(Common.TAG, "Tunnel Redirect...");

			} catch (Exception e) {
				Log.e(Common.TAG, "Get stream error:", e);
			}

		}

	}

	// 连接到代理服务器
	public boolean ConnectTarget(String strTarget) {

		Boolean Result = false;

		try {

			this.proxySocket = new Socket(Common.proxyHost, Common.proxyPort);
			this.proxySocket.setKeepAlive(true);
			this.proxySocket.setSoTimeout(120 * 1000);

			String[] parmArr = strTarget.split(":");
			// String remoteHost = parmArr[0];
			String remotePort = parmArr[1];
			
			Log.d(Common.TAG, "remote port: " + remotePort);
			
			proxyInput = new DataInputStream(proxySocket.getInputStream());
			proxyOutput = new DataOutputStream(proxySocket.getOutputStream());

			if (remotePort.equals("80")) {
				
				/*
				 * 如果目标端口是80端口，则按一般HTTP代理转发
				 */
				
				Log.d(Common.TAG, "Http Mode");
			
				String strGetRequest = "";
				String ProxyRequest = "";
				String hostName = "";
				Boolean needFormat = false;
				String strMethod = "GET";
				
				while ((strGetRequest = clientInput.readLine()) != null) {
					
					//	处理 GET POST 请求的URL
					if (strGetRequest.startsWith("GET") || strGetRequest.startsWith("POST")) {
						
						String[] parameArray = strGetRequest.split(" ");
						strMethod = parameArray[0];	//	操作方法
						
						//	如果本身带有http代理URL，则直接转发
						if (parameArray[1].startsWith("http")) {
							
							ProxyRequest = strGetRequest + "\r\n";
							break;
						} else {
						
							//	先写入后面的内容，最后来补上URL
							needFormat = true;
							ProxyRequest = parameArray[1] + " " + parameArray[2] + "\r\n";
						}
					} else {
						
						//	其它参数直接保存
						if (strGetRequest.length() > 0)
							ProxyRequest += strGetRequest + "\r\n";
					}
					
					//	通过Host段，找到目标域名
					strGetRequest = strGetRequest.toLowerCase();
					if (strGetRequest.startsWith("host:")) {
						
						hostName = strGetRequest.substring(5).trim();
						break;
					}
				}
				
				//	补上 URL
				if (needFormat)					
					ProxyRequest = strMethod + " http://" + hostName + ProxyRequest;
				
				Log.v(Common.TAG, ProxyRequest);
				
				proxyOutput.write(ProxyRequest.getBytes());
				proxyOutput.flush();
				
				Result = true;
				
			} else {
				
				Log.d(Common.TAG, "Https Mode");

				/*
				 * 不是80端口，则打洞
				 */
				// 获取输入数据

				// 连接到代理服务器，并打洞
//				if (strTarget.equals("72.14.213.139:443")) {
//					
//					strTarget = "android.clients.google.com:443";
//				}
				String connectStr = "CONNECT " + strTarget + " HTTP/1.1\r\n"
						+ "User-agent: " + Common.strUserAgent + "\r\n\r\n";

				proxyOutput.write(connectStr.getBytes());
				proxyOutput.flush();

				Log.d(Common.TAG, connectStr);

				String result = proxyInput.readLine();
				String line = "";

				Log.d(Common.TAG, result);
				while ((line = proxyInput.readLine()) != null) {

					if (line.trim().equals(""))
						break;

					Log.d(Common.TAG, line);
				}

				if (result != null && result.contains("200")) {

					Log.d(Common.TAG, result);
					// isConnected = true;
					Result = true;
				} else {
					
					Log.e(Common.TAG, "Create Tunnel failed" + result);
				}

			}

		} catch (IOException e) {

			Log.e(Common.TAG, "Proxy Server Connect Failed", e);
			Result = false;
		} catch (Exception e) {
			
			Log.e(Common.TAG, "Proxy Server Exception", e);
			Result = false;
		}

		return Result;
	}

	public class RediectData extends Thread {

		public DataInputStream dataInputStream;
		public DataOutputStream dataOutputStream;
		public Socket localSocket;
		public Socket remoteSocket;

		public RediectData(Socket ls, Socket rs, DataInputStream dis, DataOutputStream dos) {

			this.localSocket = ls;
			this.remoteSocket = rs;
			this.dataInputStream = dis;
			this.dataOutputStream = dos;
		}

		@Override
		public void run() {

			int count = 0;
			try {

				while (isConnected) {

					byte[] buff = new byte[1024];
					count = 0;
					
					count = dataInputStream.read(buff);
					
					Log.w(Common.TAG, "transfer byte: " + count);

					if (count > 0) {
						dataOutputStream.write(buff, 0, count);
						dataOutputStream.flush();
						
					} else if (count < 0) {
						
						Log.e(Common.TAG, "length < 0 Exit");
						break;
					}
				}
				
				remoteSocket.close();
				localSocket.close();
				
				Log.e(Common.TAG, "Connect Close Recv Exit");
			} catch (IOException e) {

				Log.e(Common.TAG, "Redirect Data Error Connection Close");
				isConnected = false;
			} catch (Exception e) {
				
				Log.e(Common.TAG, "Redirect Data Error Connection Close", e);
				isConnected = false;
			}

		}
	}
}
