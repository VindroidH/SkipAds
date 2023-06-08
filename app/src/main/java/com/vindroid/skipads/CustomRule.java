package com.vindroid.skipads;

import android.text.TextUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CustomRule {
    public static final String KEY_KEYWORD = "keyword";
    public static final String KEY_CLASS = "class";
    public static final String KEY_ACTION = "action";

    private final String mKeyword;
    private final String mClassName;
    private final String mAction;

    public CustomRule(String keyword, String className, String action) {
        mKeyword = TextUtils.isEmpty(keyword) ? Rule.DEFAULT_KEYWORD : keyword;
        mClassName = TextUtils.isEmpty(className) ? Rule.DEFAULT_CLASS : className;
        mAction = TextUtils.isEmpty(action) ? Rule.DEFAULT_ACTION : action;
    }

    public String getId() {
        String id = mKeyword + "|" + mClassName + "|" + mAction;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(id.getBytes());
            BigInteger bigInt = new BigInteger(1, md.digest());
            StringBuilder md5 = new StringBuilder(bigInt.toString(16));
            while (md5.length() < 32) {
                md5.insert(0, "0");
            }
            return md5.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return id;
    }

    public String getKeyword() {
        return mKeyword;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getAction() {
        return mAction;
    }
}
