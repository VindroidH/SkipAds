package com.vindroid.skipads;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "SkipAds.MainActivity";

    TextView mConfigPathTextView;
    TextView mConfigDataTextView;
    Button mPermissionBtn;
    Button mConfigBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConfigPathTextView = findViewById(R.id.text_config_path);
        mConfigDataTextView = findViewById(R.id.text_config_data);
        mPermissionBtn = findViewById(R.id.btn_permission);
        mConfigBtn = findViewById(R.id.btn_config);

        mPermissionBtn.setOnClickListener(this);
        mConfigBtn.setOnClickListener(this);

        mConfigDataTextView.setText(RuleSettings.getInstance(this).getRuleData());
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btn_permission:
                try {
                    intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }
                break;
            case R.id.btn_config:
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, 12301);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 12301) {
            if (data != null) {
                Uri uri = data.getData();
                Log.d(TAG, "[onActivityResult] uri: " + uri);
                mConfigPathTextView.setText("Rule file: " + uri);
                boolean result = RuleSettings.getInstance(this).load(uri);
                if (result) {
                    mConfigDataTextView.setText(RuleSettings.getInstance(this).getRuleData());
                    Intent intent = new Intent(Constants.ACTION_RULE_UPDATE);
                    sendBroadcast(intent);
                } else {
                    mConfigDataTextView.setText("The rule file has errors, cannot be loaded");
                }
            } else {
                Log.w(TAG, "[onActivityResult] uri is empty");
                mConfigPathTextView.setText("Rule file: WRONG PATH!");
                mConfigDataTextView.setText("");
            }
        }
    }
}