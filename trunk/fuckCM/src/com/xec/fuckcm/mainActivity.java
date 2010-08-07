package com.xec.fuckcm;

import com.xec.fuckcm.services.fuckcmServices;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class mainActivity extends Activity implements OnClickListener {

	private ToggleButton runBtn = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		runBtn = (ToggleButton) findViewById(R.id.RunButton);
		runBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.RunButton:
			if (runBtn.isChecked()) {
				
				Intent intent0 = new Intent(this, fuckcmServices.class);
				startService(intent0);
			}
			break;

		default:
			break;
		}
	}
}