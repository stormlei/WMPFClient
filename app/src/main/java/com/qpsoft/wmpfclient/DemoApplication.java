package com.qpsoft.wmpfclient;

import android.content.Context;
import android.support.multidex.MultiDex;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.qpsoft.wmpfclient.utils.InvokeTokenHelper;
import com.tencent.mmkv.MMKV;
import com.tencent.wmpf.app.WMPFApplication;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

public class DemoApplication extends WMPFApplication {

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install((Context)this);

        InvokeTokenHelper.INSTANCE.initInvokeToken(this);

        String rootDir = MMKV.initialize(this);
        System.out.println("mmkv root: " + rootDir);

        initOkGo();
    }

    private void initOkGo() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor("OkGo");
        httpLoggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        httpLoggingInterceptor.setColorLevel(Level.INFO);
        builder.addInterceptor((Interceptor)httpLoggingInterceptor);
        builder.readTimeout(60000L, TimeUnit.MILLISECONDS);
        builder.writeTimeout(60000L, TimeUnit.MILLISECONDS);
        builder.connectTimeout(60000L, TimeUnit.MILLISECONDS);
        OkGo.getInstance().init(this).setOkHttpClient(builder.build());
    }

    public void onTerminate() {
        super.onTerminate();
        MMKV.onExit();
    }
}
