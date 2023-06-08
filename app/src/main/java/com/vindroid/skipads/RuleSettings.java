package com.vindroid.skipads;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class RuleSettings {
    private final String TAG = "SkipAds.Settings";

    public static final String KEY_PACKAGE = "package";

    private final Context mContext;
    private final SharedPreferences mSp;
    private final Map<String, Rule> mRules;

    private static RuleSettings sInstance = null;

    public static synchronized RuleSettings getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RuleSettings(context);
        }
        return sInstance;
    }

    private RuleSettings(Context context) {
        mContext = context.getApplicationContext();
        mSp = mContext.getSharedPreferences(Constants.SP_APP_CONFIG, Context.MODE_PRIVATE);
        mRules = new HashMap<>();

        String jsonStr = mSp.getString(Constants.KEY_CONFIG_DATA, "");
        if (!TextUtils.isEmpty(jsonStr)) {
            try {
                loadRules(new JSONArray(jsonStr));
            } catch (JSONException e) {
                Log.e(TAG, "has exception.", e);
            }
        }
    }

    public boolean load(Uri uri) {
        Log.d(TAG, "[loadConfig] file uri: " + uri);
        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            inputStream.close();

            String json = stringBuilder.toString();
            Log.d(TAG, "[loadConfig] data: " + json);

            mSp.edit().putString(Constants.KEY_CONFIG_DATA, json).apply();
            loadRules(new JSONArray(json));

            return true;
        } catch (JSONException | IOException e) {
            Log.e(TAG, "[loadConfig] has exception.", e);
        }
        return false;
    }

    public String[] getManagedApps() {
        return mRules.keySet().toArray(new String[0]);
    }

    public String getRuleData() {
        return mSp.getString(Constants.KEY_CONFIG_DATA, "");
    }

    public Rule getRule(String packageName) {
        if (TextUtils.isEmpty(packageName) || mRules.size() == 0) {
            return null;
        }
        return mRules.get(packageName);
    }

    private void loadRules(JSONArray json) {
        if (json == null) {
            return;
        }
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject ruleJson = json.getJSONObject(i);
                mRules.put(ruleJson.getString(KEY_PACKAGE), new Rule(ruleJson));
            } catch (JSONException e) {
                Log.e(TAG, "[rules] has exception.", e);
            }
        }
    }
}
