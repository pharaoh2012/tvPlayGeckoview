package com.pharaoh.tvplay;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 在这里初始化全局变量或执行其他逻辑
        //System.out.println("MyApplication is created!");
        Log.i("Application","MyApplication is created!");
        // Toast.makeText(this, "Application Load", Toast.LENGTH_SHORT).show();

    }
}
