package com.example.campuscomments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.example.campuscomments.model.CampusPoi;
import com.example.campuscomments.util.WindowInsetUtils;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;

public class PoiEditActivity extends AppCompatActivity {
    private EditText nameInput;
    private Spinner typeSpinner;
    private EditText addressInput;
    private EditText descriptionInput;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText amapPoiIdInput;
    private EditText searchInput;
    private Button saveButton;
    private PoiSearch poiSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_edit);
        WindowInsetUtils.applySystemBars(this);

        nameInput = findViewById(R.id.nameInput);
        typeSpinner = findViewById(R.id.typeSpinner);
        addressInput = findViewById(R.id.addressInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        latitudeInput = findViewById(R.id.latitudeInput);
        longitudeInput = findViewById(R.id.longitudeInput);
        amapPoiIdInput = findViewById(R.id.amapPoiIdInput);
        searchInput = findViewById(R.id.searchInput);
        Button searchButton = findViewById(R.id.searchButton);
        saveButton = findViewById(R.id.saveButton);

        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"教学楼", "食堂", "餐厅", "自习室", "宿舍", "运动场", "其他"}));

        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);
        if (lat != 0 || lng != 0) {
            latitudeInput.setText(String.valueOf(lat));
            longitudeInput.setText(String.valueOf(lng));
        }

        searchButton.setOnClickListener(v -> searchPoi());
        saveButton.setOnClickListener(v -> savePoi());
    }

    private void searchPoi() {
        String keyword = searchInput.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        PoiSearch.Query query = new PoiSearch.Query(keyword, "", "");
        query.setPageSize(10);
        query.setPageNum(1);
        try {
            poiSearch = new PoiSearch(this, query);
            poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int rCode) {
                    if (result == null || result.getPois() == null || result.getPois().isEmpty()) {
                        Toast.makeText(PoiEditActivity.this, "没有找到相关地点", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showPoiResults(result.getPois());
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int rCode) {
                }
            });
            poiSearch.searchPOIAsyn();
        } catch (AMapException e) {
            Toast.makeText(this, "搜索初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPoiResults(List<PoiItem> pois) {
        List<String> names = new ArrayList<>();
        for (PoiItem item : pois) {
            names.add(item.getTitle() + "\n" + item.getSnippet());
        }
        new AlertDialog.Builder(this)
                .setTitle("选择地点")
                .setItems(names.toArray(new String[0]), (dialog, which) -> fillFromAmapPoi(pois.get(which)))
                .show();
    }

    private void fillFromAmapPoi(PoiItem item) {
        nameInput.setText(item.getTitle());
        addressInput.setText(item.getSnippet());
        amapPoiIdInput.setText(item.getPoiId());
        if (item.getLatLonPoint() != null) {
            latitudeInput.setText(String.valueOf(item.getLatLonPoint().getLatitude()));
            longitudeInput.setText(String.valueOf(item.getLatLonPoint().getLongitude()));
        }
    }

    private void savePoi() {
        CampusUser user = BmobUser.getCurrentUser(CampusUser.class);
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String latText = latitudeInput.getText().toString().trim();
        String lngText = longitudeInput.getText().toString().trim();

        if (user == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(latText) || TextUtils.isEmpty(lngText)) {
            Toast.makeText(this, "名称、纬度、经度不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(latText);
            longitude = Double.parseDouble(lngText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "经纬度格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        CampusPoi poi = new CampusPoi();
        poi.setName(name);
        poi.setType(AppConstants.typeFromPosition(typeSpinner.getSelectedItemPosition()));
        poi.setLatitude(latitude);
        poi.setLongitude(longitude);
        poi.setAddress(address);
        poi.setDescription(description);
        poi.setAmapPoiId(amapPoiIdInput.getText().toString().trim());
        poi.setAvgScore(0.0);
        poi.setReviewCount(0);
        poi.setCreatedBy(user);
        poi.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                saveButton.setEnabled(true);
                if (e == null) {
                    Toast.makeText(PoiEditActivity.this, "兴趣点已保存", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PoiEditActivity.this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
