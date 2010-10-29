package com.xec.fuckcm;

import com.xec.fuckcm.common.Common;
import com.xec.fuckcm.services.fuckcmServices;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class mainActivity extends Activity implements OnClickListener {

	private ToggleButton runBtn = null; // 按钮
	private TextView logWin = null; // 日志框
	private ScrollView scrollView = null; // 滚动窗口

	private IntentFilter intentFilter = null;

	public Config preConfig = new Config(this);

	Handler mHandle = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			try {

				String strMsg = msg.getData().getString("msg");
				if (strMsg.length() > 0)
					logWin.append(strMsg + "\n");

				scrollView.scrollTo(0, scrollView.getHeight());

			} catch (Exception e) {

				Log.e(Common.TAG, "handleMessage error", e);
			}
		}
	};

	BroadcastReceiver mBroadRecv = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			try {

				Message msgMessage = mHandle.obtainMessage();
				Bundle bundle = new Bundle();

				bundle.putString("msg", intent.getExtras().getString("msg"));

				msgMessage.setData(bundle);
				mHandle.sendMessage(msgMessage);

			} catch (Exception e) {

				Log.e(Common.TAG, "recv Message error", e);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//
		// 获取控件
		//
		runBtn = (ToggleButton) findViewById(R.id.RunButton);
		runBtn.setOnClickListener(this);

		// 日志框
		logWin = (TextView) findViewById(R.id.logwindow);

		// 滚动框
		scrollView = (ScrollView) findViewById(R.id.ScrollView01);

		int status = preConfig.getInt(Common.ServiceStatus,
				Common.SERVICE_STOPPED);
		if (status == Common.SERVICE_RUNING) {

			runBtn.setChecked(true);
		}

		intentFilter = new IntentFilter(Common.actionString);
		registerReceiver(mBroadRecv, intentFilter);

		String apnName = Common.getAPNName(this);
		if (!apnName.equals("cmwap")) {

			runBtn.setEnabled(false);
		}
	}

	@Override
	public void onClick(View v) {

		Intent intent0 = new Intent(this, fuckcmServices.class);

		switch (v.getId()) {
		case R.id.RunButton:
			if (runBtn.isChecked()) {

				PostUIMessage(getString(R.string.PRE_START_SERVICE));

				// 保存状态，当recv接收到消息时候确定是运行还是不做任何操作
				preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_RUNING);

				// 控制服务的状态
				intent0.putExtra("action", Common.SERVICE_RUNING);

				startService(intent0);
			} else {

				PostUIMessage(getString(R.string.PRE_STOP_SERVICE));
				preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_STOPPED);

				intent0.putExtra("action", Common.SERVICE_STOPPED);
				startService(intent0);
			}
			break;

		default:
			break;
		}

	}
	
	/**
	 * 菜单消息
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.ReplaceHosts:
			
			//	替换 hosts文件
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle(getString(R.string.REQUEST));
	        builder.setMessage(getString(R.string.REPLACE_HOSTS));
	        builder.setIcon(R.drawable.software_update);
	        builder.setPositiveButton(getString(R.string.OK), 
	        		new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							int ret = Common.rootCMD(Common.remountFileSystem);
							if (ret == 0) {
								
								installFiles("/system/etc/hosts", R.raw.hosts, null);
								PostUIMessage(getString(R.string.REPLACE_FINISH));
							}
							
						}
					});
	        builder.setNegativeButton(getString(R.string.CANCEL), 
	        		new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							dialog.cancel();							
						}
					});
	        
	        builder.show();
			return true;

		}
		return false;
	}
	
	/**
	 * 安装文件
	 * @param 目标位置
	 * @param 资源ID
	 * @param 属性
	 * @return
	 */
	private int installFiles(String dest, int resId, String mod) {

		int result = -1;
		BufferedInputStream bin = null;
		FileOutputStream fo = null;
		try {
			bin = new BufferedInputStream(getResources().openRawResource(resId));

			if (mod == null)
				mod = "644";

			File destF = new File(dest);

			// 如果文件不存在，则随便touch一个先
			if (!destF.exists())
				Common.rootCMD("busybox touch " + dest);

			Common.rootCMD("chmod 666 " + dest);

			fo = new FileOutputStream(destF);
			int length;
			byte[] content = new byte[1024];

			while ((length = bin.read(content)) > 0) {
				fo.write(content, 0, length);
			}

			fo.close();
			bin.close();
			Common.rootCMD("chmod " + mod + "  " + dest);
			result = 0;

		} catch (IOException e) {
			Log.e(Common.TAG, "Install File Error", e);
		}
		return result;
	}

	/**
	 * 传递一个消息给UI
	 */
	public void PostUIMessage(String strMessage) {

		try {

			Intent intent0 = new Intent(Common.actionString);
			intent0.putExtra("msg", strMessage);
			sendBroadcast(intent0);
			Toast.makeText(this, strMessage, Toast.LENGTH_LONG).show();

		} catch (Exception e) {

			Log.e(Common.TAG, "Post UI Message Error", e);
		}
	}
}