package com.example.campuscomments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.ScaleAnimation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;

public class MapActivity extends AppCompatActivity {
    private static final LatLng BJTU_MAIN_CAMPUS = new LatLng(39.9512, 116.3486);
    private static final int POI_SEARCH_RADIUS_METERS = 5000;
    private static final int POI_PAGE_SIZE = 100;
    private static final long LOCATION_WAIT_MS = 1800L;
    private static final long MARKER_SCALE_DURATION_MS = 220L;

    private static final String[] FILTER_LABELS = {
            "全部兴趣点", "教学楼", "食堂", "餐厅", "自习室", "宿舍", "运动场", "其他"
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Marker> poiMarkers = new ArrayList<>();
    private final List<CampusPoi> visiblePois = new ArrayList<>();
    private final Map<String, BitmapDescriptor> markerIconCache = new HashMap<>();

    private MapView mapView;
    private AMap aMap;
    private ProgressBar progressBar;
    private TextInputEditText mapSearchInput;
    private PoiSearch poiSearch;
    private MaterialButton searchButton;
    private MaterialButton filterButton;
    private LinearLayout filterOptionsContainer;

    private View summarySheet;
    private BottomSheetBehavior<View> summaryBehavior;
    private TextView summaryTitleText;
    private TextView summaryTypeText;
    private TextView summaryAddressText;
    private TextView summaryDescriptionText;
    private TextView summaryScoreText;
    private MaterialButton summaryActionButton;

    private LatLonPoint currentLocation;
    private String requestedFocusPoiId;
    private double requestedFocusLatitude = Double.NaN;
    private double requestedFocusLongitude = Double.NaN;
    private Marker selectedMarker;
    private Marker candidateMarker;
    private PoiItem candidatePoi;
    private int selectedFilterPosition;
    private int markerLoadGeneration;
    private int activeLoadingOperations;
    private boolean userMovedMap;
    private boolean hasStartedInitialLoad;
    private boolean refreshMarkersOnResume;
    private boolean markerDataReady;
    private boolean locationResolutionComplete;
    private boolean initialCameraApplied;
    private boolean showingCandidateSummary;
    private boolean filterOptionsExpanded;

    private final Runnable locationFallback = () -> {
        locationResolutionComplete = true;
        applyInitialCameraIfReady();
    };

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableMyLocation();
                    scheduleLocationFallback();
                } else {
                    locationResolutionComplete = true;
                    Toast.makeText(this, "未授权定位，已展示地图兴趣点", Toast.LENGTH_SHORT).show();
                    applyInitialCameraIfReady();
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
        WindowInsetUtils.bindBackButton(this);

        readFocusRequest();
        bindViews();
        setupMap(savedInstanceState);
        setupSummarySheet();
        setupFilterOptions();
        setupActions();
        ensureLocationPermission();
    }

    private void readFocusRequest() {
        Intent intent = getIntent();
        requestedFocusPoiId = intent.getStringExtra(AppConstants.EXTRA_FOCUS_POI_OBJECT_ID);
        requestedFocusLatitude = intent.getDoubleExtra(
                AppConstants.EXTRA_FOCUS_LATITUDE,
                Double.NaN);
        requestedFocusLongitude = intent.getDoubleExtra(
                AppConstants.EXTRA_FOCUS_LONGITUDE,
                Double.NaN);
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressBar);
        mapSearchInput = findViewById(R.id.mapSearchInput);
        searchButton = findViewById(R.id.searchButton);
        filterButton = findViewById(R.id.filterButton);
        filterOptionsContainer = findViewById(R.id.filterOptionsContainer);
        mapView = findViewById(R.id.mapView);

        summarySheet = findViewById(R.id.poiSummarySheet);
        summaryTitleText = findViewById(R.id.summaryTitleText);
        summaryTypeText = findViewById(R.id.summaryTypeText);
        summaryAddressText = findViewById(R.id.summaryAddressText);
        summaryDescriptionText = findViewById(R.id.summaryDescriptionText);
        summaryScoreText = findViewById(R.id.summaryScoreText);
        summaryActionButton = findViewById(R.id.summaryActionButton);
    }

    private void setupMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(false);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BJTU_MAIN_CAMPUS, 16f));
        aMap.setOnMarkerClickListener(this::onMarkerClick);
        aMap.setOnMapClickListener(this::onMapClick);
        aMap.setOnMapTouchListener(event -> {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                userMovedMap = true;
            }
        });
        aMap.setOnMyLocationChangeListener(location -> {
            if (location == null || location.getLatitude() == 0 || location.getLongitude() == 0) {
                return;
            }
            currentLocation = new LatLonPoint(location.getLatitude(), location.getLongitude());
            locationResolutionComplete = true;
            handler.removeCallbacks(locationFallback);
            applyInitialCameraIfReady();
        });
    }

    private void setupSummarySheet() {
        summaryBehavior = BottomSheetBehavior.from(summarySheet);
        summaryBehavior.setHideable(true);
        summaryBehavior.setSkipCollapsed(true);
        summaryBehavior.setDraggable(true);
        summaryBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        summaryBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    restoreSelectedMarker();
                    showingCandidateSummary = false;
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
            }
        });
    }

    private void setupActions() {
        searchButton.setOnClickListener(v -> {
            collapseFilterOptions();
            searchAmapPoi();
        });
        filterButton.setOnClickListener(v -> toggleFilterOptions());
        mapSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            collapseFilterOptions();
            searchAmapPoi();
            return true;
        });
    }

    private void setupFilterOptions() {
        filterOptionsContainer.removeAllViews();
        for (int i = 0; i < FILTER_LABELS.length; i++) {
            final int position = i;
            MaterialButton option = new MaterialButton(
                    this,
                    null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            option.setText(FILTER_LABELS[i]);
            option.setContentDescription("筛选：" + FILTER_LABELS[i]);
            option.setMinHeight(dpToPx(48));
            option.setMinWidth(0);
            option.setInsetTop(0);
            option.setInsetBottom(0);
            option.setCornerRadius(dpToPx(24));
            option.setStrokeWidth(dpToPx(1));
            option.setAllCaps(false);
            option.setPadding(
                    dpToPx(16),
                    0,
                    dpToPx(16),
                    0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(48));
            params.gravity = android.view.Gravity.END;
            params.topMargin = dpToPx(6);
            option.setLayoutParams(params);
            option.setOnClickListener(v -> selectFilter(position));
            filterOptionsContainer.addView(option);
        }
        updateFilterOptionStyles();
    }

    private void selectFilter(int position) {
        selectedFilterPosition = position;
        filterButton.setText(FILTER_LABELS[position]);
        updateFilterOptionStyles();
        collapseFilterOptions();
        loadPoiMarkers(true);
    }

    private void updateFilterOptionStyles() {
        for (int i = 0; i < filterOptionsContainer.getChildCount(); i++) {
            MaterialButton option = (MaterialButton) filterOptionsContainer.getChildAt(i);
            boolean selected = i == selectedFilterPosition;
            int background = ContextCompat.getColor(
                    this,
                    selected ? R.color.campus_primary : R.color.campus_surface_container);
            int foreground = ContextCompat.getColor(
                    this,
                    selected ? R.color.campus_on_primary : R.color.campus_on_surface);
            int outline = ContextCompat.getColor(
                    this,
                    selected ? R.color.campus_primary : R.color.campus_outline);
            option.setBackgroundTintList(ColorStateList.valueOf(background));
            option.setTextColor(foreground);
            option.setStrokeColor(ColorStateList.valueOf(outline));
            option.setSelected(selected);
        }
    }

    private void toggleFilterOptions() {
        if (filterOptionsExpanded) {
            collapseFilterOptions();
        } else {
            expandFilterOptions();
        }
    }

    private void expandFilterOptions() {
        if (filterOptionsExpanded) {
            return;
        }
        filterOptionsExpanded = true;
        filterOptionsContainer.setVisibility(View.VISIBLE);
        filterOptionsContainer.setAlpha(1f);
        for (int i = 0; i < filterOptionsContainer.getChildCount(); i++) {
            View option = filterOptionsContainer.getChildAt(i);
            option.animate().cancel();
            option.setAlpha(0f);
            option.setTranslationY(-dpToPx(12));
            option.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 28L)
                    .setDuration(190L)
                    .start();
        }
    }

    private void collapseFilterOptions() {
        if (!filterOptionsExpanded) {
            return;
        }
        filterOptionsExpanded = false;
        int childCount = filterOptionsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View option = filterOptionsContainer.getChildAt(i);
            option.animate().cancel();
            option.animate()
                    .alpha(0f)
                    .translationY(-dpToPx(8))
                    .setStartDelay((childCount - 1L - i) * 18L)
                    .setDuration(130L)
                    .start();
        }
        filterOptionsContainer.animate().cancel();
        filterOptionsContainer.animate()
                .alpha(0f)
                .setStartDelay(childCount * 18L + 90L)
                .setDuration(80L)
                .withEndAction(() -> {
                    filterOptionsContainer.setVisibility(View.GONE);
                    filterOptionsContainer.setAlpha(1f);
                })
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (!hasStartedInitialLoad) {
            hasStartedInitialLoad = true;
            loadPoiMarkers(false);
        } else if (refreshMarkersOnResume) {
            refreshMarkersOnResume = false;
            loadPoiMarkers(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(locationFallback);
        clearPoiMarkers();
        clearCandidateMarker();
        for (BitmapDescriptor descriptor : markerIconCache.values()) {
            descriptor.recycle();
        }
        markerIconCache.clear();
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void ensureLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            scheduleLocationFallback();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void scheduleLocationFallback() {
        handler.removeCallbacks(locationFallback);
        handler.postDelayed(locationFallback, LOCATION_WAIT_MS);
    }

    private void enableMyLocation() {
        if (aMap == null || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        MyLocationStyle style = new MyLocationStyle()
                .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                .interval(3000);
        aMap.setMyLocationStyle(style);
        aMap.setMyLocationEnabled(true);
    }

    private void searchAmapPoi() {
        String keyword = mapSearchInput.getText() == null
                ? ""
                : mapSearchInput.getText().toString().trim();
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
        beginLoading();
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
                    endLoading();
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
            endLoading();
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
            if (item.getLatLonPoint() == null) {
                continue;
            }
            LatLng itemLatLng = new LatLng(
                    item.getLatLonPoint().getLatitude(),
                    item.getLatLonPoint().getLongitude());
            item.setDistance(Math.round(AMapUtils.calculateLineDistance(centerLatLng, itemLatLng)));
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
            dialog.dismiss();
            showSearchCandidate(item);
        });
        recyclerView.setAdapter(adapter);
        adapter.submitList(pois);

        configureScrollableDialog(dialog, 0.72f);
        dialog.show();
    }

    private void showSearchCandidate(PoiItem item) {
        LatLonPoint point = item.getLatLonPoint();
        if (point == null) {
            return;
        }
        hideSummary();
        clearCandidateMarker();
        candidatePoi = item;
        LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
        candidateMarker = aMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(nonEmpty(item.getTitle(), "候选地点"))
                .icon(markerIcon("candidate", false))
                .anchor(0.5f, 0.5f)
                .zIndex(20f));
        if (candidateMarker != null) {
            candidateMarker.setObject(item);
        }
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
        showCandidateSummary(item);
    }

    private void loadPoiMarkers(boolean frameResults) {
        if (aMap == null) {
            return;
        }
        int generation = ++markerLoadGeneration;
        beginLoading();
        loadPoiPage(generation, 0, new ArrayList<>(), frameResults);
    }

    private void loadPoiPage(int generation,
                             int skip,
                             List<CampusPoi> accumulator,
                             boolean frameResults) {
        BmobQuery<CampusPoi> query = new BmobQuery<>();
        query.order("-createdAt");
        query.setLimit(POI_PAGE_SIZE);
        query.setSkip(skip);
        if (selectedFilterPosition > 0) {
            query.addWhereEqualTo("type", AppConstants.typeFromPosition(selectedFilterPosition - 1));
        }
        query.findObjects(new FindListener<CampusPoi>() {
            @Override
            public void done(List<CampusPoi> pois, BmobException e) {
                if (generation != markerLoadGeneration) {
                    endLoading();
                    return;
                }
                if (e != null) {
                    endLoading();
                    Toast.makeText(MapActivity.this,
                            "加载兴趣点失败：" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                List<CampusPoi> page = pois == null ? new ArrayList<>() : pois;
                accumulator.addAll(page);
                if (page.size() == POI_PAGE_SIZE) {
                    loadPoiPage(generation, skip + page.size(), accumulator, frameResults);
                    return;
                }

                endLoading();
                renderPoiMarkers(accumulator);
                markerDataReady = true;
                if (focusRequestedPoiIfReady()) {
                    return;
                }
                if (frameResults) {
                    initialCameraApplied = true;
                    framePois(accumulator);
                } else {
                    applyInitialCameraIfReady();
                }
            }
        });
    }

    private void renderPoiMarkers(List<CampusPoi> pois) {
        hideSummary();
        clearPoiMarkers();
        visiblePois.clear();
        visiblePois.addAll(pois);
        for (CampusPoi poi : pois) {
            addPoiMarker(poi);
        }
    }

    private boolean focusRequestedPoiIfReady() {
        if (TextUtils.isEmpty(requestedFocusPoiId)) {
            return false;
        }
        for (Marker marker : poiMarkers) {
            Object object = marker.getObject();
            if (!(object instanceof CampusPoi)) {
                continue;
            }
            CampusPoi poi = (CampusPoi) object;
            if (!requestedFocusPoiId.equals(poi.getObjectId())) {
                continue;
            }
            requestedFocusPoiId = null;
            initialCameraApplied = true;
            LatLng position = marker.getPosition();
            mapView.post(() -> {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 17.5f));
                selectPoiMarker(marker, poi);
            });
            return true;
        }

        if (!Double.isNaN(requestedFocusLatitude)
                && !Double.isNaN(requestedFocusLongitude)) {
            LatLng fallback = new LatLng(
                    requestedFocusLatitude,
                    requestedFocusLongitude);
            requestedFocusPoiId = null;
            initialCameraApplied = true;
            mapView.post(() ->
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fallback, 17.5f)));
            Toast.makeText(this, "已定位到测评对应位置", Toast.LENGTH_SHORT).show();
            return true;
        }
        requestedFocusPoiId = null;
        return false;
    }

    private void addPoiMarker(CampusPoi poi) {
        if (poi.getLatitude() == null || poi.getLongitude() == null) {
            return;
        }
        Marker marker = aMap.addMarker(new MarkerOptions()
                .position(new LatLng(poi.getLatitude(), poi.getLongitude()))
                .title(nonEmpty(poi.getName(), "未命名兴趣点"))
                .snippet(AppConstants.displayType(poi.getType()))
                .icon(markerIcon(poi.getType(), false))
                .anchor(0.5f, 0.5f)
                .zIndex(10f));
        if (marker != null) {
            marker.setObject(poi);
            poiMarkers.add(marker);
        }
    }

    private boolean onMarkerClick(Marker marker) {
        if (marker == candidateMarker && candidatePoi != null) {
            marker.setToTop();
            showCandidateSummary(candidatePoi);
            return true;
        }
        Object object = marker.getObject();
        if (object instanceof CampusPoi) {
            selectPoiMarker(marker, (CampusPoi) object);
            return true;
        }
        return false;
    }

    private void selectPoiMarker(Marker marker, CampusPoi poi) {
        if (selectedMarker != marker) {
            restoreSelectedMarker();
            selectedMarker = marker;
            marker.setIcon(markerIcon(poi.getType(), true));
            marker.setZIndex(30f);
            marker.setToTop();

            ScaleAnimation animation = new ScaleAnimation(0.78f, 1f, 0.78f, 1f);
            animation.setDuration(MARKER_SCALE_DURATION_MS);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.setFillMode(Animation.FILL_MODE_FORWARDS);
            marker.setAnimation(animation);
            marker.startAnimation();
        }
        showPoiSummary(poi);
    }

    private void showPoiSummary(CampusPoi poi) {
        showingCandidateSummary = false;
        summaryTitleText.setText(nonEmpty(poi.getName(), "未命名兴趣点"));
        summaryTypeText.setText(AppConstants.displayType(poi.getType()));
        summaryAddressText.setVisibility(View.GONE);
        summaryDescriptionText.setVisibility(View.VISIBLE);
        summaryScoreText.setVisibility(View.VISIBLE);
        summaryDescriptionText.setText(nonEmpty(poi.getDescription(), "暂无描述"));
        double score = poi.getAvgScore() == null ? 0 : poi.getAvgScore();
        int count = poi.getReviewCount() == null ? 0 : poi.getReviewCount();
        summaryScoreText.setText(String.format(
                Locale.CHINA,
                "%.1f 分 · %d 条测评",
                score,
                count));
        summaryActionButton.setText("了解详细");
        summaryActionButton.setIconResource(R.drawable.ic_chevron_right_24);
        summaryActionButton.setOnClickListener(v -> {
            refreshMarkersOnResume = true;
            Intent intent = new Intent(this, PoiDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_POI_OBJECT_ID, poi.getObjectId());
            startActivity(intent);
        });
        showSummary();
    }

    private void showCandidateSummary(PoiItem item) {
        showingCandidateSummary = true;
        summaryTitleText.setText(nonEmpty(item.getTitle(), "候选地点"));
        summaryTypeText.setText(nonEmpty(item.getTypeDes(), "高德地点"));
        summaryAddressText.setVisibility(View.VISIBLE);
        summaryDescriptionText.setVisibility(View.GONE);
        summaryScoreText.setVisibility(View.GONE);
        summaryAddressText.setText(nonEmpty(item.getSnippet(), "暂无详细地址"));
        summaryActionButton.setText("添加为兴趣点");
        summaryActionButton.setIconResource(R.drawable.ic_add_24);
        summaryActionButton.setOnClickListener(v -> {
            if (item.getLatLonPoint() == null) {
                return;
            }
            LatLng latLng = new LatLng(
                    item.getLatLonPoint().getLatitude(),
                    item.getLatLonPoint().getLongitude());
            hideSummary();
            showAddPoiSheet(latLng, item);
        });
        showSummary();
    }

    private void showSummary() {
        summarySheet.requestLayout();
        summarySheet.post(() -> {
            summaryBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            summarySheet.announceForAccessibility(
                    summaryTitleText.getText() + "，" + summaryTypeText.getText());
        });
    }

    private void hideSummary() {
        if (summaryBehavior != null
                && summaryBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            summaryBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void onMapClick(LatLng latLng) {
        if (filterOptionsExpanded) {
            collapseFilterOptions();
            return;
        }
        if (summaryBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            hideSummary();
            return;
        }
        clearCandidateMarker();
        showAddPoiSheet(latLng, null);
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

        configureScrollableDialog(dialog, 0.62f);
        dialog.show();
    }

    private void configureScrollableDialog(BottomSheetDialog dialog, float heightRatio) {
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) {
                return;
            }
            ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
            params.height = Math.round(
                    getResources().getDisplayMetrics().heightPixels * heightRatio);
            bottomSheet.setLayoutParams(params);

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setFitToContents(true);
            behavior.setHideable(true);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
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
        String name = nameInput.getText() == null
                ? ""
                : nameInput.getText().toString().trim();
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
        poi.setAddress(addressInput.getText() == null
                ? ""
                : addressInput.getText().toString().trim());
        poi.setDescription(descriptionInput.getText() == null
                ? ""
                : descriptionInput.getText().toString().trim());
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
                    clearCandidateMarker();
                    loadPoiMarkers(false);
                } else {
                    Toast.makeText(MapActivity.this,
                            "保存失败：" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyInitialCameraIfReady() {
        if (initialCameraApplied || !markerDataReady || !locationResolutionComplete) {
            return;
        }
        initialCameraApplied = true;
        if (!hasValidPoiCoordinates(visiblePois)) {
            framePois(visiblePois);
        } else if (currentLocation != null && !userMovedMap) {
            LatLng location = new LatLng(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16.5f));
        } else {
            framePois(visiblePois);
        }
    }

    private boolean hasValidPoiCoordinates(List<CampusPoi> pois) {
        for (CampusPoi poi : pois) {
            if (poi.getLatitude() != null && poi.getLongitude() != null) {
                return true;
            }
        }
        return false;
    }

    private void framePois(List<CampusPoi> pois) {
        List<LatLng> points = new ArrayList<>();
        for (CampusPoi poi : pois) {
            if (poi.getLatitude() != null && poi.getLongitude() != null) {
                points.add(new LatLng(poi.getLatitude(), poi.getLongitude()));
            }
        }
        mapView.post(() -> {
            if (points.isEmpty()) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(BJTU_MAIN_CAMPUS, 16f));
                return;
            }
            if (points.size() == 1) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 17f));
                return;
            }
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            int horizontalPadding = dpToPx(48);
            int topPadding = dpToPx(180);
            int bottomPadding = dpToPx(120);
            aMap.animateCamera(CameraUpdateFactory.newLatLngBoundsRect(
                    builder.build(),
                    horizontalPadding,
                    horizontalPadding,
                    topPadding,
                    bottomPadding));
        });
    }

    private BitmapDescriptor markerIcon(String type, boolean selected) {
        String key = (type == null ? AppConstants.TYPE_OTHER : type)
                + (selected ? "_selected" : "_normal");
        BitmapDescriptor cached = markerIconCache.get(key);
        if (cached != null) {
            return cached;
        }

        Drawable categoryIcon = ContextCompat.getDrawable(this, markerCategoryIcon(type));
        if (categoryIcon == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }

        int diameter = getResources().getDimensionPixelSize(
                selected ? R.dimen.map_marker_selected_size : R.dimen.map_marker_size);
        int padding = dpToPx(selected ? 4 : 3);
        int size = diameter + padding * 2;
        float center = size / 2f;
        float radius = diameter / 2f;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(ContextCompat.getColor(this, markerColor(type)));
        circlePaint.setShadowLayer(
                dpToPx(selected ? 3 : 2),
                0,
                dpToPx(1),
                0x55000000);
        canvas.drawCircle(center, center, radius, circlePaint);

        if (selected) {
            circlePaint.clearShadowLayer();
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(dpToPx(2));
            circlePaint.setColor(ContextCompat.getColor(this, R.color.white));
            canvas.drawCircle(center, center, radius - dpToPx(1), circlePaint);
        }

        categoryIcon = DrawableCompat.wrap(categoryIcon.mutate());
        DrawableCompat.setTint(categoryIcon, ContextCompat.getColor(this, R.color.white));
        int iconSize = dpToPx(selected ? 22 : 16);
        int iconLeft = Math.round(center - iconSize / 2f);
        int iconTop = Math.round(center - iconSize / 2f);
        categoryIcon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        categoryIcon.draw(canvas);

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        markerIconCache.put(key, descriptor);
        return descriptor;
    }

    private int markerCategoryIcon(String type) {
        if ("candidate".equals(type)) return R.drawable.ic_search_24;
        if (AppConstants.TYPE_BUILDING.equals(type)) return R.drawable.ic_marker_building_24;
        if (AppConstants.TYPE_CANTEEN.equals(type)) return R.drawable.ic_marker_canteen_24;
        if (AppConstants.TYPE_RESTAURANT.equals(type)) return R.drawable.ic_marker_restaurant_24;
        if (AppConstants.TYPE_STUDY_ROOM.equals(type)) return R.drawable.ic_marker_study_24;
        if (AppConstants.TYPE_DORM.equals(type)) return R.drawable.ic_marker_dorm_24;
        if (AppConstants.TYPE_SPORTS.equals(type)) return R.drawable.ic_marker_sports_24;
        return R.drawable.ic_marker_other_24;
    }

    @ColorRes
    private int markerColor(String type) {
        if ("candidate".equals(type)) return R.color.campus_map_marker_candidate;
        if (AppConstants.TYPE_BUILDING.equals(type)) return R.color.campus_map_marker_building;
        if (AppConstants.TYPE_CANTEEN.equals(type)) return R.color.campus_map_marker_canteen;
        if (AppConstants.TYPE_RESTAURANT.equals(type)) return R.color.campus_map_marker_restaurant;
        if (AppConstants.TYPE_STUDY_ROOM.equals(type)) return R.color.campus_map_marker_study;
        if (AppConstants.TYPE_DORM.equals(type)) return R.color.campus_map_marker_dorm;
        if (AppConstants.TYPE_SPORTS.equals(type)) return R.color.campus_map_marker_sports;
        return R.color.campus_map_marker_other;
    }

    private void restoreSelectedMarker() {
        if (selectedMarker == null) {
            return;
        }
        Object object = selectedMarker.getObject();
        if (!selectedMarker.isRemoved() && object instanceof CampusPoi) {
            CampusPoi poi = (CampusPoi) object;
            selectedMarker.setIcon(markerIcon(poi.getType(), false));
            selectedMarker.setZIndex(10f);
        }
        selectedMarker = null;
    }

    private void clearPoiMarkers() {
        restoreSelectedMarker();
        for (Marker marker : poiMarkers) {
            marker.remove();
        }
        poiMarkers.clear();
    }

    private void clearCandidateMarker() {
        if (candidateMarker != null) {
            candidateMarker.remove();
            candidateMarker = null;
        }
        candidatePoi = null;
        if (showingCandidateSummary) {
            hideSummary();
        }
    }

    private void beginLoading() {
        activeLoadingOperations++;
        progressBar.setVisibility(View.VISIBLE);
    }

    private void endLoading() {
        activeLoadingOperations = Math.max(0, activeLoadingOperations - 1);
        progressBar.setVisibility(
                activeLoadingOperations == 0 ? View.GONE : View.VISIBLE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
