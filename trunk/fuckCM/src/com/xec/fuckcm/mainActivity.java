package com.xec.fuckcm;

import com.xec.fuckcm.common.Common;
import com.xec.fuckcm.services.fuckcmServices;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class mainActivity extends Activity implements OnClickListener {

	private ToggleButton runBtn = null;
	public Config preConfig = new Config(this);

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		runBtn = (ToggleButton) findViewById(R.id.RunButton);
		runBtn.setOnClickListener(this);

		int status = preConfig.getInt(Common.ServiceStatus,
				Common.SERVICE_STOPPED);
		if (status == Common.SERVICE_RUNING) {

			runBtn.setChecked(true);
			Intent intent0 = new Intent(this, fuckcmServices.class);
			startService(intent0);
		}
	}

	@Override
	public void onClick(View v) {

		Intent intent0 = new Intent(this, fuckcmServices.class);

		switch (v.getId()) {
		case R.id.RunButton:
			if (runBtn.isChecked()) {

				preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_RUNING);

				startService(intent0);
			} else {

				preConfig.saveInt(Common.ServiceStatus, Common.SERVICE_STOPPED);

				stopService(intent0);
			}
			break;

		default:
			break;
		}
	}
}