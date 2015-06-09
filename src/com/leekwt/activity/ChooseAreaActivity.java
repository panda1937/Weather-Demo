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
	//ʡ�б�
	private List<Province> provinceList;
	//���б�
	private List<City> cityList;
	//���б�
	private List<County> countyList;
	//ѡ�е�ʡ��
	private Province selectP;
	//ѡ�е���
	private City selectedCity;
	//��ǰѡ�еļ���
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
	
	//��ѯȫ������ʡ���������ݿ��
	private void queryProvinces(){
		provinceList = coolWeatherDB.loadProvince();
		if(provinceList.size()>0){
			dataList.clear();
			for(Province p:provinceList){
				dataList.add(p.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}

	//��ѯѡ��ʡ�����еĳ���
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
	//��ѯѡ�е��������е���
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
	//���ݴ���ı�Ų�ѯʡ����
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
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_LONG).show();
						
					}
				});
			}
			
		});
	}	
	
	//��ʾ���ȶԻ���
	private void showProgressDialog(){
		if(pdialog == null){
			pdialog = new ProgressDialog(this);
			pdialog.setMessage("���ڼ���...");
			pdialog.setCanceledOnTouchOutside(false);
		}
		pdialog.show();
	}
	//�رնԻ���
	private void closeProgressDialog(){
		if(pdialog!=null){
			pdialog.dismiss();
		}
	}
	//����back�����ж��Ƿ����С�ʡ�����˳���
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