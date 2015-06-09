 package com.leekweather.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.leekweather.db.CoolWeatherDB;
import com.leekweather.model.City;
import com.leekweather.model.County;
import com.leekweather.model.Province;

public class Utility {
	
	//解析和处理服务器返回的省级数据
	public synchronized static boolean handleProvincesResponse(CoolWeatherDB coolWeatherDB,String response){
		if(!TextUtils.isEmpty(response)){
			String[] allProvinces = response.split(",");
			if(allProvinces!=null&&allProvinces.length>0){
				for(String p:allProvinces){
					String[] array = p.split("\\|");
					Province province = new Province();
					province.setProvinceCode(array[0]);
					province.setProvinceName(array[1]);
					coolWeatherDB.saveProvince(province);
				}
				return true;
			}
		}
		return false;
	}
	
	//解析和处理服务器返回的市级数据
	public static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB,String response,int provinceId){
		if(!TextUtils.isEmpty(response)){
			String[] allCities = response.split(",");
			if(allCities!=null&&allCities.length>0){
				for(String c :allCities){
					String[] array = c.split("\\|");
					City city =new City();
					city.setCityCode(array[0]);
					city.setCityName(array[1]);
					city.setProvinceId(provinceId);
					coolWeatherDB.saveCity(city);
				}
				return true;
			}
			
		}
		return false;
	}
	
	//解析和处理服务器返回的县级数据
	public static boolean handleCountiesResponse(CoolWeatherDB coolWeatherDB,String response,int cityId){
		if(!TextUtils.isEmpty(response)){
			String[] allCounties = response.split(",");
			if(allCounties!=null&&allCounties.length>0){
				for(String c:allCounties){
					String[] array = c.split("\\|");
					County county = new County();
					county.setCountyCode(array[0]);
					county.setCountyName(array[1]);
					county.setCityId(cityId);
					coolWeatherDB.saveCounty(county);
				}
				return true;
			}
		}
		return false;
	}
	
	//解析服务器返回的json数据，并将解析出来的数据存储到本地
	public static void handleWeatherResponse(Context context,String response){
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONObject weatherinfo = jsonObject.getJSONObject("weatherinfo");
			String cityName = weatherinfo.getString("city");
			String watherCode = weatherinfo.getString("cityid");
			String temp1 = weatherinfo.getString("temp1");
			String temp2 = weatherinfo.getString("temp2");
			String weatherDesp = weatherinfo.getString("weather");
			String publishTime = weatherinfo.getString("ptime");
			saveWeatherInfo(context, cityName, watherCode, temp1, temp2, weatherDesp, publishTime);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	//把解析出的数据存储到SharedPreferences中
	public static void saveWeatherInfo(Context context,String cityname,String weathercode,String temp1,String temp2,String weatherdesp,String publishtime){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日",Locale.CHINA);		
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean("city_selected", true);
		editor.putString("city_name", cityname);
		editor.putString("weather_code", weathercode);
		editor.putString("temp1", temp1);
		editor.putString("temp2", temp2);
		editor.putString("weather_desp", weatherdesp);
		editor.putString("publish_time", publishtime);
		editor.putString("current_data", sdf.format(new Date()));
		
		editor.commit();
		
	}
}
