package com.leekwt.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.leek.weather.R;
import com.leekweather.db.CoolWeatherDB;
import com.leekweather.model.City;
import com.leekweather.model.County;
import com.leekweather.model.Province;
import com.leekweather.util.HttpCallbackListener;
import com.leekweather.util.HttpUtil;
import com.leekweather.util.Utility;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE=0;
	public static final int LEVEL_CITY=1;
	public static final int LEVEL_COUNTY=2;
	
	private boolean isFromWeatherActivity;
	
	private ProgressDialog pdialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	//省列表
	private List<Province> provinceList;
	//市列表
	private List<City> cityList;
	//县列表
	private List<County> countyList;
	//选中的省份
	private Province selectP;
	//选中的市
	private City selectedCity;
	//当前选中的级别
	private int currentLevel;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(prefs.getBoolean("city_selected", false)&& !isFromWeatherActivity){
			Intent intent = new Intent(this,WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText =(TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(currentLevel == LEVEL_PROVINCE){
					selectP = provinceList.get(position);
					queryCities();
				}else if(currentLevel == LEVEL_CITY){
					selectedCity = cityList.get(position);
					queryCounties();
				}else if(currentLevel ==LEVEL_COUNTY){
					String countyCode = countyList.get(position).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
				
			}
		});
		queryProvinces();
	}
	
	//查询全国所有省，优先数据库查
	private void queryProvinces(){
		provinceList = coolWeatherDB.loadProvince();
		if(provinceList.size()>0){
			dataList.clear();
			for(Province p:provinceList){
				dataList.add(p.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}

	//查询选中省内所有的城市
	private void queryCities(){
		cityList = coolWeatherDB.loadCities(selectP.getId());
		if(cityList.size()>0){
			dataList.clear();
			for(City c:cityList){
				dataList.add(c.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectP.getProvinceName());
			currentLevel = LEVEL_CITY;
		}else {
			queryFromServer(selectP.getProvinceCode(),"city");
		}
	}
	//查询选中的市内所有的县
	private void queryCounties(){
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size()>0){
			dataList.clear();
			for(County c:countyList){
				dataList.add(c.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		}else{
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	//根据传入的编号查询省市县
	private void queryFromServer(final String code,final String type){
		String address;
		if(!TextUtils.isEmpty(code)){
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else{
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		
		showProgressDialog();
		
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener(){

			@Override
			public void onFinish(String response) {
				
				boolean result = false;
				if("province".equals(type)){
					result = Utility.handleProvincesResponse(coolWeatherDB, response);
					
				}else if("city".equals(type)){
					result = Utility.handleCitiesResponse(coolWeatherDB, response, selectP.getId());
				
				}else if("county".equals(type)){
					result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				
				}
				
				if(result){
					runOnUiThread(new Runnable() {						
						@Override
						public void run() {							
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							}else if("city".equals(type)){
								queryCities();
							}else if("county".equals(type)){
								queryCounties();
							}
						}
					});
				}
				
			}

			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_LONG).show();
						
					}
				});
			}
			
		});
	}	
	
	//显示进度对话框
	private void showProgressDialog(){
		if(pdialog == null){
			pdialog = new ProgressDialog(this);
			pdialog.setMessage("正在加载...");
			pdialog.setCanceledOnTouchOutside(false);
		}
		pdialog.show();
	}
	//关闭对话框
	private void closeProgressDialog(){
		if(pdialog!=null){
			pdialog.dismiss();
		}
	}
	//捕获back键，判断是返回市、省还是退出；
	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		if(currentLevel == LEVEL_COUNTY){
			queryCities();
		}else if(currentLevel == LEVEL_CITY){
			queryProvinces();
		}else
		{
//			if(isFromWeatherActivity){
//				Intent intent = new Intent(this,WeatherActivity.class);
//				startActivity(intent);
//			}
			finish();
		}
	}
}