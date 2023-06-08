package com.vindroid.skipads;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkipAdsService extends AccessibilityService {
    private final String TAG = "SkipAds.SkipAdsService";

    private final long DELAY_MILLIS = 200;
    private final int MAX_TRY_TIMES = 15; // 200ms * 15 = 3s

    private RuleBroadcastReceiver mRuleReceiver;
    private Map<String, Integer> mAppSkipAdsTimes;
    private Map<String, Integer> mTriggeredApps;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
        String packageName = accessibilityEvent.getPackageName().toString().toLowerCase();
        String className = accessibilityEvent.getClassName().toString().toLowerCase();
        int eventType = accessibilityEvent.getEventType();
        int windowId = accessibilityEvent.getWindowId();
        String key = packageName + "/" + className;
        Log.d(TAG, "[onAccessibilityEvent] app: " + key + ", event: " + eventType + ", windowId: " + windowId);

        if (mTriggeredApps.getOrDefault(key, -1) == windowId) {
            Log.d(TAG, "[onAccessibilityEvent] same window id, ignore");
            return;
        }
        Rule rule = RuleSettings.getInstance(this).getRule(packageName);
        if (rule == null) {
            Log.d(TAG, "[onAccessibilityEvent] not found rule");
            return;
        }
        List<String> ruleClasses = rule.getClasses();
        if (!ruleClasses.isEmpty() && !ruleClasses.contains(Rule.DEFAULT_CLASS) && !ruleClasses.contains(className)) {
            Log.d(TAG, "[onAccessibilityEvent] no matching class, ignore");
            return;
        }

        Log.d(TAG, "[onAccessibilityEvent] rule: " + rule);
        mTriggeredApps.put(key, accessibilityEvent.getWindowId());
        if (skipAds(nodeInfo, className, rule)) {
            mAppSkipAdsTimes.remove(packageName);
        } else {
            mAppSkipAdsTimes.put(packageName, 1);
            skipAdsLater(nodeInfo, packageName, className, rule);
        }
    }

    private void skipAdsLater(final AccessibilityNodeInfo nodeInfo, final String packageName, final String className, final Rule rule) {
        new Handler().postDelayed(() -> {
            if (skipAds(nodeInfo, className, rule)) {
                mAppSkipAdsTimes.remove(packageName);
                return;
            }
            int times = mAppSkipAdsTimes.getOrDefault(packageName, 1);
            mAppSkipAdsTimes.put(packageName, ++times);

            if (times < MAX_TRY_TIMES) {
                skipAdsLater(nodeInfo, packageName, className, rule);
            } else {
                mAppSkipAdsTimes.remove(packageName);
            }
        }, DELAY_MILLIS);
    }

    private boolean skipAds(final AccessibilityNodeInfo nodeInfo, final String className, final Rule rule) {
        if (Rule.TYPE_DEFAULT.equals(rule.getType())) {
            return performAction(nodeInfo, Rule.DEFAULT_KEYWORD, Rule.ACTION_CLICK);
        } else if (Rule.TYPE_CUSTOM.equals(rule.getType())) {
            boolean result = false;
            String keyword, clzName, action;
            Map<String, CustomRule> customRules = rule.getCustomRules();
            for (Map.Entry<String, CustomRule> entry : customRules.entrySet()) {
                keyword = entry.getValue().getKeyword();
                clzName = entry.getValue().getClassName();
                action = entry.getValue().getAction();
                if (TextUtils.isEmpty(clzName) || className.equalsIgnoreCase(clzName)) {
                    result = performAction(nodeInfo, keyword, action);
                }
            }
            return result;
        } else {
            Log.w(TAG, "[skipAds] unknown rule type: " + rule.getType());
            return true;
        }
    }

    private boolean performAction(final AccessibilityNodeInfo nodeInfo, final String keyword, final String action) {
        List<AccessibilityNodeInfo> nodeInfoList = nodeInfo.findAccessibilityNodeInfosByText(keyword);
        for (AccessibilityNodeInfo info : nodeInfoList) {
            if (info.getText() == null || !info.getText().toString().contains(keyword)) {
                continue;
            }
            Log.d(TAG, "[performAction] " + nodeInfo.getPackageName() + ", action: " + action);
            if (Rule.ACTION_CLICK.equals(action)) {
                if (!info.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Rect rect = new Rect();
                    info.getBoundsInScreen(rect);
                    Path path = new Path();
                    path.moveTo((rect.left + rect.right) / 2.0f, (rect.top + rect.bottom) / 2.0f);
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
                    dispatchGesture(builder.build(), null, null);
                }
            } else if (Rule.ACTION_BACK.equals(action)) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            } else {
                Log.w(TAG, "[performAction] unknown action");
            }
            return true;
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "[onInterrupt]");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "[onServiceConnected]");
        mAppSkipAdsTimes = new HashMap<>();
        mTriggeredApps = new HashMap<>();

        mRuleReceiver = new RuleBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_RULE_UPDATE);
        registerReceiver(mRuleReceiver, filter);

        updateServiceInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "[onDestroy]");
        if (mRuleReceiver != null) {
            unregisterReceiver(mRuleReceiver);
        }
    }

    private void updateServiceInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        String[] packageNames = RuleSettings.getInstance(SkipAdsService.this).getManagedApps();
        for (String pkg : packageNames) {
            stringBuilder.append(pkg).append(" | ");
        }
        Log.d(TAG, "[updateServiceInfo] skip ads apps: " + stringBuilder);

        AccessibilityServiceInfo info = getServiceInfo();
        info.packageNames = packageNames;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        setServiceInfo(info);
    }

    public class RuleBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_RULE_UPDATE.equals(intent.getAction())) {
                Log.d(TAG, "[RuleBroadcastReceiver] received: " + intent.getAction());
                updateServiceInfo();
            }
        }
    }
}
