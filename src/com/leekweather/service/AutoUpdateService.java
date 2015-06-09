package com.leekweather.service;

import com.leekweather.receiver.AutoUpdateReceiver;
import com.leekweather.util.HttpCallbackListener;
import com.leekweather.util.HttpUtil;
import com.leekweather.util.Utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class AutoUpdateService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				updateweather();
				
			}
		}).start();
				
	AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
	int anHour = 8*60*60*1000;
	long triggerAtTime = SystemClock.elapsedRealtime()+anHour;
	Intent intent1 = new Intent(this,AutoUpdateReceiver.class);
	PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent1, 0);
	manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
	return super.onStartCommand(intent1, flags, startId);
	
	}
	
	private void updateweather(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String weatherCode = prefs.getString("weather_code", "");
		String address = "Http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				Utility.handleWeatherResponse(AutoUpdateService.this, response);
				
			}
			
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	

}
