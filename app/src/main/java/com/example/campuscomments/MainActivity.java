package com.example.campuscomments;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.campuscomments.model.CampusPoi;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.SaveListener;

public class MainActivity extends AppCompatActivity {

    private CampusUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = BmobUser.getCurrentUser(CampusUser.class);
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView welcomeText = findViewById(R.id.welcomeText);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button mapButton = findViewById(R.id.mapButton);
        Button listButton = findViewById(R.id.listButton);
        Button addButton = findViewById(R.id.addPoiButton);
        Button seedButton = findViewById(R.id.seedPoiButton);
        Button myButton = findViewById(R.id.myButton);
        String displayName = currentUser.getNickname() == null ? currentUser.getUsername() : currentUser.getNickname();
        welcomeText.setText("欢迎来到校园点评，" + displayName);
        mapButton.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        listButton.setOnClickListener(v -> startActivity(new Intent(this, PoiListActivity.class)));
        addButton.setOnClickListener(v -> startActivity(new Intent(this, PoiEditActivity.class)));
        seedButton.setOnClickListener(v -> createSeedPoi(seedButton));
        myButton.setOnClickListener(v -> startActivity(new Intent(this, MyActivity.class)));
        logoutButton.setOnClickListener(v -> {
            BmobUser.logOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void createSeedPoi(Button seedButton) {
        seedButton.setEnabled(false);
        CampusPoi poi = new CampusPoi();
        poi.setName("测试地点 " + System.currentTimeMillis() % 10000);
        poi.setType(AppConstants.TYPE_CANTEEN);
        poi.setLatitude(39.9042);
        poi.setLongitude(116.4074);
        poi.setAddress("校园内测试位置");
        poi.setDescription("这是一条用于调试 Bmob 云端读写的兴趣点。");
        poi.setAvgScore(0.0);
        poi.setReviewCount(0);
        poi.setCreatedBy(currentUser);
        poi.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                seedButton.setEnabled(true);
                if (e == null) {
                    Toast.makeText(MainActivity.this, "测试兴趣点已创建", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "创建失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
