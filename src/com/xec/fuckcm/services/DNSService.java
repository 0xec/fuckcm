package com.xec.fuckcm.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Hashtable;
import android.util.Log;

import com.xec.fuckcm.common.Common;

public class DNSService extends Thread {

	public DatagramSocket socket;
	public Boolean isRuning = false;
	public Hashtable<String, String> dnsCache = new Hashtable<String, String>();

	public DNSService(DatagramSocket skt) {

		this.socket = skt;
		isRuning = true;
	}

	@Override
	public void run() {

		byte[] byteBuffer = new byte[1024];

		while (isRuning) {
			try {
				Arrays.fill(byteBuffer, (byte) 0);
				DatagramPacket dataPacket = new DatagramPacket(byteBuffer, 1024);

				socket.receive(dataPacket);

				Log.i(Common.TAG, "dns packet recv");

				int len = dataPacket.getLength();
				byte[] data = dataPacket.getData();

				if (len < 13)
					continue;

				// 获取域名
				String strDomain = getRequestDomain(data);

				Log.d(Common.TAG, "Query Domain: " + strDomain);

				String strIPString = "";

				// 解析域名
				strIPString = QueryDomainName(strDomain);

				if (strIPString.length() <= 0) {
					continue;
				}

				/*
				 * // 构建回应报文 byte[] responseBuffer =
				 * BuildDNSResponsePacket(data, len, strIPString); int replen =
				 * responseBuffer.length;
				 * 
				 * DatagramPacket resp = new DatagramPacket(responseBuffer, 0,
				 * replen); resp.setPort(dataPacket.getPort());
				 * resp.setAddress(dataPacket.getAddress());
				 * 
				 * socket.send(resp);
				 * 
				 * Log.i(Common.TAG, "response dns request success" +
				 * dataPacket.getAddress().toString() + ":" +
				 * dataPacket.getPort());
				 */

				SaveToHosts(strDomain, strIPString);

			} catch (IOException e) {
				Log.e(Common.TAG, "DNS Recv IO Exception", e);
			} catch (Exception e) {
				Log.e(Common.TAG, "DNS Recv Exception", e);
			}
		}
	}

	//
	// 获取域名
	//
	private synchronized String getRequestDomain(byte[] request) {

		String requestDomain = "";
		int reqLength = request.length;

		if (reqLength > 13) { // 包含包体

			byte[] question = new byte[reqLength - 12];
			System.arraycopy(request, 12, question, 0, reqLength - 12);
			requestDomain = getPartialDomain(question);
			requestDomain = requestDomain.substring(0,
					requestDomain.length() - 1);
		}
		return requestDomain;
	}

	// 拼接域名字符串
	private String getPartialDomain(byte[] request) {

		String result = "";
		int length = request.length;
		int partLength = request[0];
		if (partLength == 0)
			return result;

		try {
			byte[] left = new byte[length - partLength - 1];
			System.arraycopy(request, partLength + 1, left, 0, length
					- partLength - 1);
			result = new String(request, 1, partLength) + ".";
			result += getPartialDomain(left);

		} catch (Exception e) {
			Log.e(Common.TAG, "getPartialDomin error", e);
		}
		return result;
	}

	//
	// 解析域名
	//
	public synchronized String QueryDomainName(String domainString) {

		String result = "";

		// 在缓存中查找IP地址
		if (dnsCache.containsKey(domainString)) {

			result = dnsCache.get(domainString);
			Log.d(Common.TAG, "(cache)DNS Success: " + domainString + "---->"
					+ result);
			return result;
		}

		result = QueryDomainOnNetwork(domainString);

		return result;
	}

	//
	// 在 www.ip138.com 上解析域名
	//
	public String QueryDomainOnNetwork(String domainString) {

		String result = "";
		try {

			Socket skt = new Socket(Common.proxyHost, Common.proxyPort);

			skt.setSoTimeout(30000);

			BufferedReader din = new BufferedReader(new InputStreamReader(
					skt.getInputStream()));
			BufferedWriter dout = new BufferedWriter(new OutputStreamWriter(
					skt.getOutputStream()));

			String strRequest = "GET http://wap.ip138.com/ip.asp?ip="
					+ domainString + " HTTP/1.1\r\n"
					+ "Host: wap.ip138.com\r\n" + "Accept: */*\r\n"
					+ "User-Agent: " + Common.strUserAgent + "\r\n"
					+ "Connection: Keep-Alive\r\n" + "\r\n";

			dout.write(strRequest);
			dout.flush();

			String line = "";
			String httpStatusString = din.readLine();

			if (!httpStatusString.contains("200")) {

				skt.close();
				return result;
			}

			while ((line = din.readLine()) != null) {

				if (line.contains(domainString)) {

					String startTagString = "&gt;&gt; ";
					String endTagString = "<br/>";

					int start = line.indexOf(startTagString);
					if (start <= 0) {

						startTagString = ">> ";
						start = line.indexOf(">> ");
					}
					int end = line.indexOf(endTagString, start);
					result = line.substring(start + startTagString.length(),
							end);
					if (result.contains(".")) {
						Log.d(Common.TAG, "DNS Success: " + domainString
								+ "---->" + result);

						dnsCache.put(domainString, result);
					} else {

						Log.d(Common.TAG, "DNS Faild: " + domainString
								+ "---->" + result);
						result = "";
					}

					break;
				}

				if (line.contains("</body>")) {

					break;
				}

				if (line.contains("</wml>")) {

					break;
				}
			}

			skt.close();

		} catch (IOException e) {

			Log.e(Common.TAG, "Dns on network io excpetion", e);
		} catch (Exception e) {
			Log.e(Common.TAG, "Dns on network excpetion", e);
		}

		return result;
	}

	//
	// 构建DNS响应报文
	//
	public synchronized byte[] BuildDNSResponsePacket(byte[] request,
			int reqLen, String ipAddress) {

		byte[] repPacket = new byte[1024];
		byte[] realResponsePacket = null;
		int length = 0;

		try {

			// 先获取ID编号
			byte[] byteID = new byte[2];
			System.arraycopy(request, 0, byteID, 0, 2);

			System.arraycopy(byteID, 0, repPacket, length, 2);
			length += 2;

			// 构建其他信息，一直到answer列表
			byte[] other = new byte[] { (byte) 0x81, (byte) 0x80, (byte) 0x00,
					(byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00 };
			System.arraycopy(other, 0, repPacket, length, 10);
			length += 10;

			// 请求原始报文
			System.arraycopy(request, 12, repPacket, length, reqLen - 12);
			length += (reqLen - 12);

			// 响应报文部分
			byte[] answer = new byte[] { (byte) 0xc0, (byte) 0x0c, (byte) 0x00,
					(byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
					(byte) 0x00, (byte) 0x01, (byte) 0xbf, (byte) 0x00,
					(byte) 0x04 };
			System.arraycopy(answer, 0, repPacket, length, 12);
			length += 12;

			// 补上IP地址
			byte[] addr = conver_inet_addr(ipAddress);
			System.arraycopy(addr, 0, repPacket, length, 4);
			length += 4;

			realResponsePacket = new byte[length];

			System.arraycopy(repPacket, 0, realResponsePacket, 0, length);

		} catch (Exception e) {
			Log.e(Common.TAG, "build dns packet error", e);
		}

		return realResponsePacket;
	}

	public byte[] conver_inet_addr(String ipaddr) {

		byte[] addr = new byte[4];
		String strTempString = ipaddr;
		int iTemp = 0;
		try {

			for (int i = 0; i < 4; i++) {

				int pos = ipaddr.indexOf(".");
				if (pos == -1) {
					pos = ipaddr.length();
				}
				strTempString = ipaddr.substring(0, pos);
				iTemp = Integer.valueOf(strTempString);
				addr[i] = (byte) (iTemp & 0xff);

				if (i != 3)
					ipaddr = ipaddr.substring(pos + 1);
			}

		} catch (Exception e) {
			Log.e(Common.TAG, "conver_inet_addr error", e);
		}

		return addr;
	}

	public void CloseAll() {

		isRuning = false;
		socket.close();
	}

	public void SaveToHosts(String domain, String ip) {

		try {

			// MountFileSystem();

			String dnsHosts = ip + "\t\t\t" + domain;

			File fe = new File("/sdcard/fuckcm_etc/hosts");
			if (!fe.exists()) {
				fe.createNewFile();
			}

			FileReader fr = new FileReader(fe);
			FileOutputStream fos = new FileOutputStream(fe, true);

			BufferedReader bReader = new BufferedReader(fr);

			String line = "";
			while ((line = bReader.readLine()) != null) {

				Log.d("hosts", line);
				if (line.contains(domain))
					return;
			}
			fr.close();

			dnsHosts = "\n" + dnsHosts;

			fos.write(dnsHosts.getBytes());
			fos.close();

			Log.d(Common.TAG, "Save To Hosts" + ip + "-->" + domain);

			// UnmountFileSystem();

		} catch (Exception e) {

			Log.e(Common.TAG, "save Dns error", e);
		}
	}
}
