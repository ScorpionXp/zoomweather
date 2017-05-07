package com.zoomweather.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zoomweather.R;
import com.zoomweather.db.City;
import com.zoomweather.db.Country;
import com.zoomweather.db.Province;
import com.zoomweather.util.HttpUtil;
import com.zoomweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by XuPeng on 2017/5/7.
 */

public class ChooseAreaFragment extends Fragment{
    private static final int LEVEL_PROVINCE=0;
    private static final int LEVEL_CITY=1;
    private static final int LEVEL_COUNTRY=2;

    private ProgressDialog progressDialog;
    private TextView title;
    private Button back;
    private ListView listView;

    private ArrayAdapter<String> arrayAdapter;
    private List<String> dataList=new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<Country> countryList;

    //选中的省市县
    private Province selectedProvince;
    private City selectedCity;
    private Country selectedCountry;

    //当前选中级别
    private int currLevel;

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        title=(TextView)view.findViewById(R.id.text_title);
        back=(Button)view.findViewById(R.id.button_back);
        listView=(ListView)view.findViewById(R.id.list_view);
        arrayAdapter=new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(arrayAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long l) {
                if(currLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCity();
                }else if(currLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCountry();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currLevel==LEVEL_COUNTRY){
                    queryCity();
                }else if(currLevel==LEVEL_CITY){
                    queryProvince();
                }
            }
        });
        queryProvince();
    }
    //查询全国所有省份，优先从数据库查询，如果没有查询到再去服务器查询
    private void queryProvince(){
        title.setText("中国");
        back.setVisibility(View.INVISIBLE);
        provinceList= DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            currLevel=LEVEL_PROVINCE;
        }else {
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    //查询选中省内所有城市，优先从数据库查询，如果没有查询到再去服务器查询
    private void queryCity(){
        title.setText(selectedProvince.getProvinceName());
        back.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid=?",String.valueOf(
                selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            currLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }
    //查询选中城市所有的县，优先从数据库查询，如果没有查询到再去服务器查询
    private void queryCountry(){
        title.setText(selectedCity.getCityName());
        back.setVisibility(View.VISIBLE);
        countryList=DataSupport.where("cityid=?",String.valueOf(
                selectedCity.getId())).find(Country.class);
        if(countryList.size()>0){
            dataList.clear();
            for(Country country:countryList){
                dataList.add(country.getCountryName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            currLevel=LEVEL_COUNTRY;
        }else {
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"country");
        }
    }
    //若在数据库中为查询到，则在服务器上查询，根据传入的地址和类型在服务器上查询数据
    private void queryFromServer(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //回到主线程继续处理逻辑,通过runOnUiThread()方法
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT)
                                .show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,
                            selectedProvince.getId());
                }else if("country".equals(type)){
                    result=Utility.handleCountryResponse(responseText,
                            selectedCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvince();
                            }else if("city".equals(type)){
                               queryCity();
                            }else if("country".equals(type)){
                                queryCountry();
                            }
                        }
                    });
                }
            }
        });
    }
    //显示进度对话框
    private void showProgressDialog() {
        if (progressDialog == null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    //关闭进度对话框
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
