package com.example.campuscomments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.example.campuscomments.adapter.PoiAdapter;
import com.example.campuscomments.adapter.ReviewAdapter;
import com.example.campuscomments.model.CampusPoi;
import com.example.campuscomments.model.Favorite;
import com.example.campuscomments.model.Review;
import com.example.campuscomments.util.ReviewDeleteUtils;
import com.example.campuscomments.util.WindowInsetUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

public class MainActivity extends AppCompatActivity {
    private static final double NEARBY_RADIUS_KM = 3.0;

    private CampusUser currentUser;
    private AMapLocationClient locationClient;
    private Double userLatitude;
    private Double userLongitude;
    private boolean locationRequested;
    private boolean showedLocationFallback;

    private View homeSection;
    private View listSection;
    private View favoriteSection;
    private View mySection;
    private View myReviewsSection;
    private ImageButton homeTab;
    private ImageButton listTab;
    private ImageButton addTab;
    private ImageButton favoriteTab;
    private ImageButton myTab;

    private TextInputEditText homeSearchInput;
    private TextInputEditText listSearchInput;
    private Spinner campusSpinner;
    private TextView locationHintText;
    private TextView randomEmptyText;
    private TextView listEmptyText;
    private TextView favoriteEmptyText;
    private TextView myReviewEmptyText;
    private TextView userAvatarText;
    private TextView userNameText;
    private TextView userAccountText;
    private TextView favoriteCountText;
    private TextView reviewCountText;
    private ProgressBar listProgressBar;

    private ReviewAdapter randomReviewAdapter;
    private PoiAdapter listAdapter;
    private PoiAdapter favoriteAdapter;
    private ReviewAdapter myReviewAdapter;
    private final List<CampusPoi> nearbyPois = new ArrayList<>();

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    requestSingleLocation();
                } else {
                    showAllPoisWithoutLocation();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        super.onCreate(savedInstanceState);

        currentUser = BmobUser.getCurrentUser(CampusUser.class);
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        WindowInsetUtils.applySystemBars(this);
        bindViews();
        setupTabs();
        setupHome();
        setupLists();
        showTab(homeSection, homeTab);
        loadRandomReviews();
        ensureLocationForNearbyList();
        loadFavorites();
        loadMyReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadRandomReviews();
            loadNearbyPois();
            loadFavorites();
            loadMyReviews();
        }
    }

    @Override
    protected void onDestroy() {
        if (locationClient != null) {
            locationClient.onDestroy();
        }
        super.onDestroy();
    }

    private void bindViews() {
        homeSection = findViewById(R.id.homeSection);
        listSection = findViewById(R.id.listSection);
        favoriteSection = findViewById(R.id.favoriteSection);
        mySection = findViewById(R.id.mySection);
        myReviewsSection = findViewById(R.id.myReviewsSection);
        homeTab = findViewById(R.id.homeTab);
        listTab = findViewById(R.id.listTab);
        addTab = findViewById(R.id.addTab);
        favoriteTab = findViewById(R.id.favoriteTab);
        myTab = findViewById(R.id.myTab);
        homeSearchInput = findViewById(R.id.homeSearchInput);
        listSearchInput = findViewById(R.id.listSearchInput);
        campusSpinner = findViewById(R.id.campusSpinner);
        locationHintText = findViewById(R.id.locationHintText);
        randomEmptyText = findViewById(R.id.randomEmptyText);
        listEmptyText = findViewById(R.id.listEmptyText);
        favoriteEmptyText = findViewById(R.id.favoriteEmptyText);
        myReviewEmptyText = findViewById(R.id.myReviewEmptyText);
        userAvatarText = findViewById(R.id.userAvatarText);
        userNameText = findViewById(R.id.userNameText);
        userAccountText = findViewById(R.id.userAccountText);
        favoriteCountText = findViewById(R.id.favoriteCountText);
        reviewCountText = findViewById(R.id.reviewCountText);
        listProgressBar = findViewById(R.id.listProgressBar);
    }

    private void setupTabs() {
        homeTab.setOnClickListener(v -> showTab(homeSection, homeTab));
        listTab.setOnClickListener(v -> showTab(listSection, listTab));
        addTab.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        favoriteTab.setOnClickListener(v -> {
            showTab(favoriteSection, favoriteTab);
            loadFavorites();
        });
        myTab.setOnClickListener(v -> {
            showTab(mySection, myTab);
            loadMyReviews();
        });
    }

    private void setupHome() {
        TextView welcomeText = findViewById(R.id.welcomeText);
        String displayName = firstNonEmpty(currentUser.getNickname(), currentUser.getUsername(), "同学");
        welcomeText.setText("Hello, " + displayName);
        campusSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"北京交通大学主校区", "北京交通大学东校区"}));
        homeSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                listSearchInput.setText(homeSearchInput.getText());
                showTab(listSection, listTab);
                applyListFilter();
                return true;
            }
            return false;
        });
    }

    private void setupLists() {
        randomReviewAdapter = new ReviewAdapter();
        listAdapter = new PoiAdapter(this::openPoiDetail);
        favoriteAdapter = new PoiAdapter(this::openPoiDetail);
        myReviewAdapter = new ReviewAdapter(currentUser.getObjectId(), this::confirmDeleteReview);

        setupRecycler(R.id.randomReviewRecyclerView, randomReviewAdapter);
        setupRecycler(R.id.nearbyPoiRecyclerView, listAdapter);
        setupRecycler(R.id.favoriteRecyclerView, favoriteAdapter);
        setupRecycler(R.id.myReviewRecyclerView, myReviewAdapter);

        listSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyListFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            BmobUser.logOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        String displayName = firstNonEmpty(currentUser.getNickname(), currentUser.getUsername(), "同学");
        String school = firstNonEmpty(currentUser.getSchool(), "北京交通大学");
        userAvatarText.setText(displayName.substring(0, 1).toUpperCase(Locale.CHINA));
        userNameText.setText(displayName);
        userAccountText.setText("@" + firstNonEmpty(currentUser.getUsername(), "未设置") + " · " + school);

        findViewById(R.id.favoriteSummaryCard).setOnClickListener(v -> {
            showTab(favoriteSection, favoriteTab);
            loadFavorites();
        });
        findViewById(R.id.reviewSummaryCard).setOnClickListener(v -> showMyReviews());
        findViewById(R.id.myReviewsBackButton).setOnClickListener(v -> showTab(mySection, myTab));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (myReviewsSection.getVisibility() == View.VISIBLE) {
                    showTab(mySection, myTab);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupRecycler(int recyclerId, RecyclerView.Adapter<?> adapter) {
        RecyclerView recyclerView = findViewById(recyclerId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void showTab(View section, ImageButton selectedTab) {
        homeSection.setVisibility(section == homeSection ? View.VISIBLE : View.GONE);
        listSection.setVisibility(section == listSection ? View.VISIBLE : View.GONE);
        favoriteSection.setVisibility(section == favoriteSection ? View.VISIBLE : View.GONE);
        mySection.setVisibility(section == mySection ? View.VISIBLE : View.GONE);
        myReviewsSection.setVisibility(View.GONE);

        setTabSelected(homeTab, selectedTab == homeTab);
        setTabSelected(listTab, selectedTab == listTab);
        setTabSelected(favoriteTab, selectedTab == favoriteTab);
        setTabSelected(myTab, selectedTab == myTab);
        setTabSelected(addTab, false);
    }

    private void showMyReviews() {
        homeSection.setVisibility(View.GONE);
        listSection.setVisibility(View.GONE);
        favoriteSection.setVisibility(View.GONE);
        mySection.setVisibility(View.GONE);
        myReviewsSection.setVisibility(View.VISIBLE);
        setTabSelected(homeTab, false);
        setTabSelected(listTab, false);
        setTabSelected(favoriteTab, false);
        setTabSelected(myTab, true);
        setTabSelected(addTab, false);
        loadMyReviews();
    }

    private void setTabSelected(ImageButton tab, boolean selected) {
        tab.setSelected(selected);
        tab.setBackgroundResource(selected ? R.drawable.bg_nav_selected : android.R.color.transparent);
        tab.setColorFilter(ContextCompat.getColor(
                this,
                selected ? R.color.campus_nav_selected_icon : R.color.campus_nav_icon));
    }

    private void ensureLocationForNearbyList() {
        if (locationRequested) return;
        locationRequested = true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestSingleLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void requestSingleLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setOnceLocation(true);
            option.setNeedAddress(true);
            locationClient.setLocationOption(option);
            locationClient.setLocationListener(this::onLocationResult);
            locationClient.startLocation();
        } catch (Exception e) {
            showAllPoisWithoutLocation();
        }
    }

    private void onLocationResult(AMapLocation location) {
        if (location != null && location.getErrorCode() == 0) {
            userLatitude = location.getLatitude();
            userLongitude = location.getLongitude();
            locationHintText.setText("已按当前位置筛选 3 公里内兴趣点");
            loadNearbyPois();
        } else {
            showAllPoisWithoutLocation();
        }
        if (locationClient != null) {
            locationClient.stopLocation();
        }
    }

    private void showAllPoisWithoutLocation() {
        userLatitude = null;
        userLongitude = null;
        locationHintText.setText("未开启定位，已显示全部地点");
        if (!showedLocationFallback) {
            showedLocationFallback = true;
            Toast.makeText(this, "未开启定位，已显示全部地点", Toast.LENGTH_SHORT).show();
        }
        loadNearbyPois();
    }

    private void loadRandomReviews() {
        BmobQuery<Review> query = new BmobQuery<>();
        query.include("poi,author");
        query.order("-createdAt");
        query.setLimit(20);
        query.findObjects(new FindListener<Review>() {
            @Override
            public void done(List<Review> reviews, BmobException e) {
                if (e == null) {
                    if (reviews == null) {
                        reviews = new ArrayList<>();
                    }
                    Collections.shuffle(reviews);
                    List<Review> display = reviews.size() > 5 ? reviews.subList(0, 5) : reviews;
                    randomReviewAdapter.submitList(display);
                    randomEmptyText.setVisibility(display.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    randomEmptyText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadNearbyPois() {
        if (listProgressBar != null) listProgressBar.setVisibility(View.VISIBLE);
        BmobQuery<CampusPoi> query = new BmobQuery<>();
        query.order("-createdAt");
        query.findObjects(new FindListener<CampusPoi>() {
            @Override
            public void done(List<CampusPoi> pois, BmobException e) {
                if (listProgressBar != null) listProgressBar.setVisibility(View.GONE);
                if (e == null) {
                    if (pois == null) {
                        pois = new ArrayList<>();
                    }
                    nearbyPois.clear();
                    nearbyPois.addAll(filterAndSortNearby(pois));
                    applyListFilter();
                } else {
                    listEmptyText.setText("加载兴趣点失败");
                    listEmptyText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private List<CampusPoi> filterAndSortNearby(@NonNull List<CampusPoi> pois) {
        List<CampusPoi> result = new ArrayList<>();
        for (CampusPoi poi : pois) {
            if (userLatitude == null || userLongitude == null) {
                result.add(poi);
            } else if (poi.getLatitude() != null
                    && poi.getLongitude() != null
                    && distanceKm(userLatitude, userLongitude, poi.getLatitude(), poi.getLongitude()) <= NEARBY_RADIUS_KM) {
                result.add(poi);
            }
        }
        if (userLatitude != null && userLongitude != null) {
            result.sort(Comparator.comparingDouble(this::distanceFromUserOrMax));
        }
        return result;
    }

    private double distanceFromUserOrMax(CampusPoi poi) {
        if (userLatitude == null || userLongitude == null || poi.getLatitude() == null || poi.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return distanceKm(userLatitude, userLongitude, poi.getLatitude(), poi.getLongitude());
    }

    private void applyListFilter() {
        String keyword = listSearchInput == null || listSearchInput.getText() == null
                ? "" : listSearchInput.getText().toString().trim().toLowerCase(Locale.CHINA);
        List<CampusPoi> filtered = new ArrayList<>();
        for (CampusPoi poi : nearbyPois) {
            if (TextUtils.isEmpty(keyword)
                    || contains(poi.getName(), keyword)
                    || contains(poi.getAddress(), keyword)
                    || contains(AppConstants.displayType(poi.getType()), keyword)) {
                filtered.add(poi);
            }
        }
        listAdapter.submitList(filtered);
        listEmptyText.setText(TextUtils.isEmpty(keyword) ? "附近暂无兴趣点" : "没有匹配的兴趣点");
        listEmptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.CHINA).contains(keyword);
    }

    private void loadFavorites() {
        BmobQuery<Favorite> query = new BmobQuery<>();
        query.addWhereEqualTo("user", currentUser);
        query.include("poi");
        query.order("-createdAt");
        query.findObjects(new FindListener<Favorite>() {
            @Override
            public void done(List<Favorite> favorites, BmobException e) {
                if (e == null) {
                    if (favorites == null) {
                        favorites = new ArrayList<>();
                    }
                    List<CampusPoi> pois = new ArrayList<>();
                    for (Favorite favorite : favorites) {
                        if (favorite.getPoi() != null) {
                            pois.add(favorite.getPoi());
                        }
                    }
                    favoriteAdapter.submitList(pois);
                    favoriteCountText.setText(String.format(Locale.CHINA, "%d 收藏", pois.size()));
                    favoriteEmptyText.setVisibility(pois.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    favoriteCountText.setText("0 收藏");
                    favoriteEmptyText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadMyReviews() {
        BmobQuery<Review> query = new BmobQuery<>();
        query.addWhereEqualTo("author", currentUser);
        query.include("author,poi");
        query.order("-createdAt");
        query.findObjects(new FindListener<Review>() {
            @Override
            public void done(List<Review> reviews, BmobException e) {
                if (e == null) {
                    if (reviews == null) {
                        reviews = new ArrayList<>();
                    }
                    myReviewAdapter.submitList(reviews);
                    reviewCountText.setText(String.format(Locale.CHINA, "%d 测评", reviews.size()));
                    myReviewEmptyText.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    reviewCountText.setText("0 测评");
                    myReviewEmptyText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void confirmDeleteReview(Review review) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除测评")
                .setMessage("删除后无法恢复，确定删除这条测评吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) ->
                        ReviewDeleteUtils.deleteOwnedReview(review, (deleted, errorMessage) -> {
                            if (deleted) {
                                Toast.makeText(this, "测评已删除", Toast.LENGTH_SHORT).show();
                                loadMyReviews();
                                loadRandomReviews();
                                loadNearbyPois();
                            }
                            if (errorMessage != null) {
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    private void openPoiDetail(CampusPoi poi) {
        Intent intent = new Intent(this, PoiDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_POI_OBJECT_ID, poi.getObjectId());
        startActivity(intent);
    }

    private double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
