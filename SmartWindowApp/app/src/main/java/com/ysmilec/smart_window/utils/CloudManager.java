package com.ysmilec.smart_window.utils;


import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class CloudManager {
    private static CloudManager instance;
    private static final String CLOUD_URL = "https://api.heclouds.com";
    OkHttpClient okHttpClient = new OkHttpClient();
    private CloudManager() {

    }

    public static CloudManager getInstance() {
        if (instance == null) {
            synchronized (CloudManager.class) {
                if (instance == null) {
                    instance = new CloudManager();
                }
            }
        }
        return instance;
    }

    public static String getToken() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String version = "2018-10-31";
        String resourceName = "products/275459";
        String expirationTime = System.currentTimeMillis() / 1000 + 100 * 24 * 60 * 60 + "";
        String signatureMethod = TokenGenerator.SignatureMethod.SHA1.name().toLowerCase();
        String accessKey = "ScgD6dse2CpZxwipfSbFYtiLEh7OxUBuAQK2l7+HzuM=";
        String token = TokenGenerator.assembleToken(version, resourceName, expirationTime, signatureMethod, accessKey);
        return token;
    }


    public void setWindowStatus(boolean isWindowOpen, Callback callback) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String token = getToken();
        String status = null;
        if(isWindowOpen)
            status = "open";
        else
            status = "close";

        String json="{\""+status+"\"}";
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body=RequestBody.create(JSON,json);
        Request request = new Request
                .Builder()
                .post(body)
                .url(CLOUD_URL + "/v1/synccmds?device_id=544541006&timeout=30")
                .header("Authorization",token)
                .build();
        Log.i("response",request.toString());
        okHttpClient.newCall(request).enqueue(callback);
    }

    public void getWindowStatus( Callback callback) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String token = getToken();
        Request request = new Request
                .Builder()
                .get()
                .url(CLOUD_URL + "/devices/544541006/datastreams/window_status")
                .header("Authorization",token)
                .build();
        Log.i("response",request.toString());
        okHttpClient.newCall(request).enqueue(callback);
    }

    public void getWindowIsrain( Callback callback) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String token = getToken();
        Request request = new Request
                .Builder()
                .get()
                .url(CLOUD_URL + "/devices/544541006/datastreams/israin")
                .header("Authorization",token)
                .build();
        Log.i("response",request.toString());
        okHttpClient.newCall(request).enqueue(callback);
    }
}


