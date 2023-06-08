package com.vindroid.skipads;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class Rule {
    private static final String TAG = "SkipAds.Rule";

    public static final String KEY_TYPE = "rule";
    public static final String KEY_CUSTOM_RULE = "custom_rules";

    public static final String TYPE_DEFAULT = "default";
    public static final String TYPE_CUSTOM = "custom";

    public static final String ACTION_CLICK = "click";
    public static final String ACTION_BACK = "back";

    public static final String DEFAULT_KEYWORD = "跳过";
    public static final String DEFAULT_CLASS = "";
    public static final String DEFAULT_ACTION = ACTION_CLICK;

    private String mType;
    private Map<String, CustomRule> mCustomRules;
    private List<String> mClasses;

    public Rule(JSONObject json) {
        if (json == null) {
            Log.e(TAG, "json is empty");
            return;
        }
        mType = json.optString(KEY_TYPE, TYPE_DEFAULT).toLowerCase();
        mCustomRules = new HashMap<>();
        mClasses = new ArrayList<>();
        if (TYPE_CUSTOM.equals(mType)) {
            try {
                JSONArray customRules = json.getJSONArray(KEY_CUSTOM_RULE);
                JSONObject rule;
                String key, cls, action;
                for (int i = 0; i < customRules.length(); i++) {
                    rule = customRules.getJSONObject(i);
                    key = rule.optString(CustomRule.KEY_KEYWORD, DEFAULT_KEYWORD).toLowerCase();
                    cls = rule.optString(CustomRule.KEY_CLASS, DEFAULT_CLASS).toLowerCase();
                    action = rule.optString(CustomRule.KEY_ACTION, ACTION_CLICK).toLowerCase();
                    CustomRule customRule = new CustomRule(key, cls, action);
                    mCustomRules.put(customRule.getId(), customRule);
                    if (!mClasses.contains(cls)) {
                        mClasses.add(cls);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "missing exception when parsing custom rules.", e);
            }
        }
    }

    public String getType() {
        return mType;
    }

    public Map<String, CustomRule> getCustomRules() {
        return mCustomRules;
    }

    public List<String> getClasses() {
        return mClasses;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("type: ").append(mType);
        if (TYPE_DEFAULT.equals(mType)) {
            builder.append(", keyword: ").append(DEFAULT_KEYWORD);
        }
        if (mCustomRules.size() > 0) {
            builder.append(", custom rule:");
            String keyword, className, action;
            for (Map.Entry<String, CustomRule> entry : mCustomRules.entrySet()) {
                keyword = entry.getValue().getKeyword();
                className = entry.getValue().getClassName();
                action = entry.getValue().getAction();
                builder.append(" [")
                        .append("keyword: ").append(keyword)
                        .append(", className: ").append(className)
                        .append(", action: ").append(action)
                        .append("]");
            }
        }
        return builder.toString();
    }
}
