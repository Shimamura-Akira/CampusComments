package com.example.campuscomments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscomments.adapter.PoiAdapter;
import com.example.campuscomments.db.LocalDatabaseHelper;
import com.example.campuscomments.model.CampusPoi;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

public class PoiListActivity extends AppCompatActivity {
    private PoiAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private Spinner typeSpinner;
    private LocalDatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_list);

        databaseHelper = new LocalDatabaseHelper(this);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        typeSpinner = findViewById(R.id.typeSpinner);
        Button refreshButton = findViewById(R.id.refreshButton);
        Button addButton = findViewById(R.id.addButton);
        RecyclerView poiRecyclerView = findViewById(R.id.poiRecyclerView);

        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"全部", "教学楼", "食堂", "餐厅", "自习室", "宿舍", "运动场", "其他"}));
        adapter = new PoiAdapter(poi -> {
            Intent intent = new Intent(this, PoiDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_POI_OBJECT_ID, poi.getObjectId());
            startActivity(intent);
        });
        poiRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        poiRecyclerView.setAdapter(adapter);

        refreshButton.setOnClickListener(v -> loadPois());
        addButton.setOnClickListener(v -> startActivity(new Intent(this, PoiEditActivity.class)));
        loadPois();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPois();
    }

    private void loadPois() {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
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
                    adapter.submitList(pois);
                    databaseHelper.replacePoiCache(pois);
                    emptyText.setVisibility(pois.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    List<CampusPoi> cached = readCachedPois();
                    adapter.submitList(cached);
                    emptyText.setVisibility(cached.isEmpty() ? View.VISIBLE : View.GONE);
                    Toast.makeText(PoiListActivity.this, "云端加载失败，已尝试显示缓存：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private List<CampusPoi> readCachedPois() {
        List<CampusPoi> pois = new ArrayList<>();
        Cursor cursor = databaseHelper.getReadableDatabase().query(
                LocalDatabaseHelper.TABLE_POI_CACHE,
                null,
                null,
                null,
                null,
                null,
                LocalDatabaseHelper.COL_NAME + " ASC");
        try {
            while (cursor.moveToNext()) {
                CampusPoi poi = new CampusPoi();
                poi.setObjectId(cursor.getString(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_OBJECT_ID)));
                poi.setName(cursor.getString(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_NAME)));
                poi.setType(cursor.getString(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_TYPE)));
                poi.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_LATITUDE)));
                poi.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_LONGITUDE)));
                poi.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_ADDRESS)));
                poi.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_DESCRIPTION)));
                poi.setAvgScore(cursor.getDouble(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_AVG_SCORE)));
                poi.setReviewCount(cursor.getInt(cursor.getColumnIndexOrThrow(LocalDatabaseHelper.COL_REVIEW_COUNT)));
                pois.add(poi);
            }
        } finally {
            cursor.close();
        }
        return pois;
    }
}
