package com.example.campuscomments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.example.campuscomments.adapter.AmapPoiResultAdapter;
import com.example.campuscomments.model.CampusPoi;
import com.example.campuscomments.util.WindowInsetUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;

public class MapActivity extends AppCompatActivity {
    private static final LatLng BJTU_MAIN_CAMPUS = new LatLng(39.9512, 116.3486);
    private static final int POI_SEARCH_RADIUS_METERS = 5000;

    private MapView mapView;
    private AMap aMap;
    private ProgressBar progressBar;
    private Spinner typeSpinner;
    private TextInputEditText mapSearchInput;
    private PoiSearch poiSearch;
    private MaterialButton searchButton;
    private LatLonPoint currentLocation;
    private boolean userMovedMap;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableMyLocation();
                } else {
                    Toast.makeText(this, "未授权定位，仍可查看和添加兴趣点", Toast.LENGTH_SHORT).show();
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
        WindowInsetUtils.applySystemBars(this);

        progressBar = findViewById(R.id.progressBar);
        typeSpinner = findViewById(R.id.typeSpinner);
        mapSearchInput = findViewById(R.id.mapSearchInput);
        searchButton = findViewById(R.id.searchButton);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"全部", "教学楼", "食堂", "餐厅", "自习室", "宿舍", "运动场", "其他"}));
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(false);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BJTU_MAIN_CAMPUS, 16f));
        aMap.setOnMarkerClickListener(this::onMarkerClick);
        aMap.setOnMapClickListener(latLng -> showAddPoiSheet(latLng, null));
        aMap.setOnMapTouchListener(event -> {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                userMovedMap = true;
            }
        });
        aMap.setOnMyLocationChangeListener(location -> {
            if (location != null && location.getLatitude() != 0 && location.getLongitude() != 0) {
                currentLocation = new LatLonPoint(location.getLatitude(), location.getLongitude());
            }
        });

        searchButton.setOnClickListener(v -> searchAmapPoi());
        findViewById(R.id.filterButton).setOnClickListener(v -> loadPoiMarkers());
        mapSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            searchAmapPoi();
            return true;
        });

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
            MyLocationStyle style = new MyLocationStyle()
                    .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                    .interval(3000);
            aMap.setMyLocationStyle(style);
            aMap.setMyLocationEnabled(true);
        }
    }

    private void searchAmapPoi() {
        String keyword = mapSearchInput.getText() == null ? "" : mapSearchInput.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "请输入地点关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        LatLonPoint searchCenter = resolveSearchCenter();
        boolean usingMapCenter = userMovedMap || currentLocation == null;
        PoiSearch.Query query = new PoiSearch.Query(keyword, "", "北京");
        query.setPageSize(20);
        query.setPageNum(1);
        query.setCityLimit(true);
        query.setDistanceSort(true);
        query.setLocation(searchCenter);
        query.setExtensions("all");
        searchButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        try {
            poiSearch = new PoiSearch(this, query);
            poiSearch.setBound(new PoiSearch.SearchBound(
                    searchCenter,
                    POI_SEARCH_RADIUS_METERS,
                    true));
            poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult result, int rCode) {
                    searchButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    if (result == null || result.getPois() == null || result.getPois().isEmpty()) {
                        Toast.makeText(MapActivity.this, "5 公里内没有找到相关地点", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<PoiItem> sorted = new ArrayList<>(result.getPois());
                    updateAndSortDistances(sorted, searchCenter);
                    showPoiResults(sorted, usingMapCenter);
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int rCode) {
                }
            });
            poiSearch.searchPOIAsyn();
        } catch (AMapException e) {
            searchButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "搜索初始化失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private LatLonPoint resolveSearchCenter() {
        if (!userMovedMap && currentLocation != null) {
            return currentLocation;
        }
        LatLng target = aMap.getCameraPosition().target;
        return new LatLonPoint(target.latitude, target.longitude);
    }

    private void updateAndSortDistances(List<PoiItem> pois, LatLonPoint center) {
        LatLng centerLatLng = new LatLng(center.getLatitude(), center.getLongitude());
        for (PoiItem item : pois) {
            if (item.getLatLonPoint() != null) {
                LatLng itemLatLng = new LatLng(
                        item.getLatLonPoint().getLatitude(),
                        item.getLatLonPoint().getLongitude());
                item.setDistance(Math.round(AMapUtils.calculateLineDistance(centerLatLng, itemLatLng)));
            }
        }
        pois.sort(Comparator.comparingInt(item ->
                item.getDistance() > 0 ? item.getDistance() : Integer.MAX_VALUE));
    }

    private void showPoiResults(List<PoiItem> pois, boolean usingMapCenter) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(R.layout.bottom_sheet_poi_results, null);
        dialog.setContentView(content);

        TextView centerHintText = content.findViewById(R.id.searchCenterHintText);
        centerHintText.setText(usingMapCenter
                ? "以当前地图中心搜索，5 公里内按距离排序"
                : "以当前位置搜索，5 公里内按距离排序");

        RecyclerView recyclerView = content.findViewById(R.id.amapPoiResultRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AmapPoiResultAdapter adapter = new AmapPoiResultAdapter(item -> {
            if (item.getLatLonPoint() == null) {
                Toast.makeText(this, "该地点缺少坐标", Toast.LENGTH_SHORT).show();
                return;
            }
            LatLng latLng = new LatLng(
                    item.getLatLonPoint().getLatitude(),
                    item.getLatLonPoint().getLongitude());
            dialog.dismiss();
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
            showAddPoiSheet(latLng, item);
        });
        recyclerView.setAdapter(adapter);
        adapter.submitList(pois);

        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = Math.round(getResources().getDisplayMetrics().heightPixels * 0.72f);
                bottomSheet.setLayoutParams(params);
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        dialog.show();
    }

    private void showAddPoiSheet(LatLng latLng, PoiItem poiItem) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(R.layout.bottom_sheet_add_poi, null);
        dialog.setContentView(content);

        TextInputEditText nameInput = content.findViewById(R.id.sheetNameInput);
        TextInputEditText addressInput = content.findViewById(R.id.sheetAddressInput);
        TextInputEditText descriptionInput = content.findViewById(R.id.sheetDescriptionInput);
        Spinner sheetTypeSpinner = content.findViewById(R.id.sheetTypeSpinner);
        MaterialButton cancelButton = content.findViewById(R.id.sheetCancelButton);
        MaterialButton saveButton = content.findViewById(R.id.sheetSaveButton);

        sheetTypeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"教学楼", "食堂", "餐厅", "自习室", "宿舍", "运动场", "其他"}));
        if (poiItem != null) {
            nameInput.setText(poiItem.getTitle());
            addressInput.setText(poiItem.getSnippet());
        } else {
            addressInput.setText("地图选点");
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        saveButton.setOnClickListener(v -> savePoiFromSheet(
                dialog,
                saveButton,
                latLng,
                poiItem == null ? "" : poiItem.getPoiId(),
                nameInput,
                addressInput,
                descriptionInput,
                sheetTypeSpinner));

        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = getResources().getDisplayMetrics().heightPixels / 2;
                bottomSheet.setLayoutParams(params);
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        dialog.show();
    }

    private void savePoiFromSheet(BottomSheetDialog dialog,
                                  MaterialButton saveButton,
                                  LatLng latLng,
                                  String amapPoiId,
                                  TextInputEditText nameInput,
                                  TextInputEditText addressInput,
                                  TextInputEditText descriptionInput,
                                  Spinner typeSpinner) {
        CampusUser user = BmobUser.getCurrentUser(CampusUser.class);
        if (user == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请填写地点名称", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        CampusPoi poi = new CampusPoi();
        poi.setName(name);
        poi.setType(AppConstants.typeFromPosition(typeSpinner.getSelectedItemPosition()));
        poi.setLatitude(latLng.latitude);
        poi.setLongitude(latLng.longitude);
        poi.setAddress(addressInput.getText() == null ? "" : addressInput.getText().toString().trim());
        poi.setDescription(descriptionInput.getText() == null ? "" : descriptionInput.getText().toString().trim());
        poi.setAmapPoiId(amapPoiId);
        poi.setAvgScore(0.0);
        poi.setReviewCount(0);
        poi.setCreatedBy(user);
        poi.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                saveButton.setEnabled(true);
                if (e == null) {
                    Toast.makeText(MapActivity.this, "兴趣点已保存", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadPoiMarkers();
                } else {
                    Toast.makeText(MapActivity.this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
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
                    if (pois == null) {
                        pois = new ArrayList<>();
                    }
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
            android.content.Intent intent = new android.content.Intent(this, PoiDetailActivity.class);
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
