package com.example.campuscomments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.example.campuscomments.model.CampusPoi;

import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private AMap aMap;
    private ProgressBar progressBar;
    private Spinner typeSpinner;
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableMyLocation();
                } else {
                    Toast.makeText(this, "未授权定位，仍可查看兴趣点", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        progressBar = findViewById(R.id.progressBar);
        typeSpinner = findViewById(R.id.typeSpinner);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"全部", "教学楼", "食堂", "餐厅", "自习室", "宿舍", "运动场", "其他"}));
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.9042, 116.4074), 15f));
        aMap.setOnMarkerClickListener(this::onMarkerClick);
        aMap.setOnMapLongClickListener(latLng -> {
            Intent intent = new Intent(this, PoiEditActivity.class);
            intent.putExtra("latitude", latLng.latitude);
            intent.putExtra("longitude", latLng.longitude);
            startActivity(intent);
        });

        findViewById(R.id.addPoiButton).setOnClickListener(v -> startActivity(new Intent(this, PoiEditActivity.class)));
        findViewById(R.id.filterButton).setOnClickListener(v -> loadPoiMarkers());
        ensureLocationPermission();
        loadPoiMarkers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        loadPoiMarkers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void ensureLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMyLocation() {
        if (aMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            aMap.setMyLocationEnabled(true);
        }
    }

    private void loadPoiMarkers() {
        if (aMap == null) return;
        progressBar.setVisibility(View.VISIBLE);
        BmobQuery<CampusPoi> query = new BmobQuery<>();
        query.order("-createdAt");
        int selected = typeSpinner.getSelectedItemPosition();
        if (selected > 0) {
            query.addWhereEqualTo("type", AppConstants.typeFromPosition(selected - 1));
        }
        query.findObjects(new FindListener<CampusPoi>() {
            @Override
            public void done(List<CampusPoi> pois, BmobException e) {
                progressBar.setVisibility(View.GONE);
                if (e == null) {
                    aMap.clear();
                    for (CampusPoi poi : pois) {
                        addMarker(poi);
                    }
                } else {
                    Toast.makeText(MapActivity.this, "加载兴趣点失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addMarker(CampusPoi poi) {
        if (poi.getLatitude() == null || poi.getLongitude() == null) return;
        Marker marker = aMap.addMarker(new MarkerOptions()
                .position(new LatLng(poi.getLatitude(), poi.getLongitude()))
                .title(poi.getName())
                .snippet(AppConstants.displayType(poi.getType()))
                .icon(BitmapDescriptorFactory.defaultMarker(markerHue(poi.getType()))));
        if (marker != null) {
            marker.setObject(poi.getObjectId());
        }
    }

    private boolean onMarkerClick(Marker marker) {
        Object object = marker.getObject();
        if (object instanceof String) {
            Intent intent = new Intent(this, PoiDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_POI_OBJECT_ID, (String) object);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private float markerHue(String type) {
        if (AppConstants.TYPE_CANTEEN.equals(type)) return BitmapDescriptorFactory.HUE_ORANGE;
        if (AppConstants.TYPE_RESTAURANT.equals(type)) return BitmapDescriptorFactory.HUE_RED;
        if (AppConstants.TYPE_STUDY_ROOM.equals(type)) return BitmapDescriptorFactory.HUE_GREEN;
        if (AppConstants.TYPE_DORM.equals(type)) return BitmapDescriptorFactory.HUE_VIOLET;
        if (AppConstants.TYPE_SPORTS.equals(type)) return BitmapDescriptorFactory.HUE_AZURE;
        return BitmapDescriptorFactory.HUE_BLUE;
    }
}
