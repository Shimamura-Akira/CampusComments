package com.example.campuscomments;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuscomments.util.WindowInsetUtils;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText nicknameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        WindowInsetUtils.applySystemBars(this);
        WindowInsetUtils.bindBackButton(this);

        usernameInput = findViewById(R.id.usernameInput);
        nicknameInput = findViewById(R.id.nicknameInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> register());
    }

    private void register() {
        String username = usernameInput.getText().toString().trim();
        String nickname = nicknameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "账号和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        registerButton.setEnabled(false);
        CampusUser user = new CampusUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(TextUtils.isEmpty(nickname) ? username : nickname);
        user.signUp(new SaveListener<CampusUser>() {
            @Override
            public void done(CampusUser campusUser, BmobException e) {
                registerButton.setEnabled(true);
                if (e == null) {
                    Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "注册失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
