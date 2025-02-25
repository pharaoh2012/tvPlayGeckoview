package com.pharaoh.tvplay;

import android.app.Activity;
import android.os.Bundle;

public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_home);
        Http.init(this.getApplicationContext());
        PlayControl.Instance.Init(this);
    }
    @Override
    protected void onPause() {
        super.onPause(); // 调用父类方法
        finish(); // 销毁当前Activity
    }
}