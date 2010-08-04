package com.xec.fuckcm;

import android.preference.PreferenceManager;
import android.content.Context;
import android.content.SharedPreferences;

public class Config {
	
	public Context ctxContext;	
	public Config(Context context) {
		this.ctxContext = context;		
	}
	
	public void saveInt(String name, int value) {
		
		SharedPreferences sharePerference = PreferenceManager.getDefaultSharedPreferences(ctxContext);
		SharedPreferences.Editor editor = sharePerference.edit();
		editor.putInt(name, value);
		editor.commit();
	}
	
	public void saveString(String name, String value) {
		
		SharedPreferences sharePerference = PreferenceManager.getDefaultSharedPreferences(ctxContext);
		SharedPreferences.Editor editor = sharePerference.edit();
		editor.putString(name, value);
		editor.commit();
	}
	
	public String getString(String name, String defaultValue) {
		
		SharedPreferences sharePerference = PreferenceManager.getDefaultSharedPreferences(ctxContext);
		return sharePerference.getString(name, defaultValue);
	}
	
	public int getString(String name, int defaultValue) {
		
		SharedPreferences sharePerference = PreferenceManager.getDefaultSharedPreferences(ctxContext);
		return sharePerference.getInt(name, defaultValue);
	}

}
