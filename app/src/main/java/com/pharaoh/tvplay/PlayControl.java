package com.pharaoh.tvplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PlayControl {
    public static PlayControl Instance = new PlayControl();
    long lastBackTime = 0;
    Config config;
    String[] PlayUrls = null;
    Activity activity;
    private float[] lastTouchDownXY = new float[2];

    public void setActivity(Activity a) {
        activity = a;
    }

    private  String getUrl() {
        int length = PlayUrls.length;
        int currentCCTV = config.getCurrentCCTV();
        if(currentCCTV>length) currentCCTV=1;
        if(currentCCTV < 1) currentCCTV = length;
        config.setCurrentCCTV(currentCCTV);
        toast("播放"+ currentCCTV +"频道", Toast.LENGTH_LONG);
        return PlayUrls[currentCCTV-1].trim();
    }

    public void Play() {
        String url = getUrl();
        String u1 = url;
        String useragent = null;
        Boolean firefox=false;
        Boolean desktop = false;
        if(url.contains("##")) {
            int index = url.indexOf("##"); // 获取第一个 # 的索引位置
            u1 = url.substring(0, index); // 第一部分：# 前的字符串
            useragent = url.substring(index + 2); // 第二部分：第一个 # 后的所有内容
            firefox = useragent.contains("webview=firefox");
            desktop = useragent.contains("desktop=1");
        }
        Intent intent;
        if(firefox) intent = new Intent(activity,GeckoViewActivity.class);
        else intent = new Intent(activity,MainActivity.class);
        intent.putExtra("desktop",desktop);

        activity.startActivity(intent);
        activity.finish();

    }

    private void PlayNext() {
        config.setCurrentCCTV(config.getCurrentCCTV()+1);
        Play();
    }
    private void PlayPre() {
        config.setCurrentCCTV(config.getCurrentCCTV()-1);
        Play();
    }

    private void Play(int i) {
        config.setCurrentCCTV(i);
        //mWebView.loadUrl(getUrl());
        Play();
    }

    public void toast(final String msg, final int time) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, msg, time).show();
            }
        });
    }

    public void toast(final String msg) {
        toast(msg,Toast.LENGTH_SHORT);
    }

    public boolean DoKey(int keyCode) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - lastBackTime > 2000) {
                lastBackTime = System.currentTimeMillis();
                toast("再按一次返回键关闭");
            }
            else {
                activity.finish();
            }
            return true;
        }
        // Toast.makeText(this,"KeyCode:"+keyCode,Toast.LENGTH_SHORT).show();
        switch (keyCode) {
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                PlayPre();
                return true;
            case KeyEvent.KEYCODE_CHANNEL_UP:
                PlayNext();
                return true;
            case KeyEvent.KEYCODE_MENU:
                showConfigDialog();
                return true;
            default:
                //Toast.makeText(this,"keyCode:"+keyCode,Toast.LENGTH_LONG).show();
                if(keyCode > KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                    Play(keyCode-KeyEvent.KEYCODE_0);
                    return true;
                } else if(keyCode > KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
                    Play(keyCode-KeyEvent.KEYCODE_NUMPAD_0);
                    return true;
                }
                break;
        }

        return false;
    }

    private void showConfigDialog() {
        // 获取EditText
        final EditText editText = new EditText(activity);
        editText.setSingleLine();
        editText.setHint("设置地址");
        editText.requestFocus();
        editText.setFocusable(true);
        editText.setText(config.JS_HOST);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(activity)
                .setTitle("请输入设置地址：")
                .setView(editText).setPositiveButton("确定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String content = editText.getText().toString().trim();
                                if (content.length() == 0) {
                                    toast("网址不能为空!");
                                    return;
                                }
                                config.setJS_HOST(content);
                                LoadConfig();
                            }
                        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        inputDialog.create().show();
    }

    public boolean DoTouch(MotionEvent event) {

        // save the X,Y coordinates
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastTouchDownXY[0] = event.getX();
            lastTouchDownXY[1] = event.getY();
            //toast("onTouch down");
        } else if(event.getActionMasked() == MotionEvent.ACTION_UP) {
            if(lastTouchDownXY[1]>300) return false;
            float dx = event.getX()-lastTouchDownXY[0];
            float dy = event.getY()-lastTouchDownXY[1];
            // toast("onTouch up: dx="+dx+"  dy="+dy);
            if(Math.abs(dy) < 100) {
                if(Math.abs(dx) < 100) return false;
                if(dx<0) {
                    PlayPre();
                    return false;
                } else {
                    PlayNext();
                    return false;
                }
            } else {
                if(Math.abs(dx)>100) return false;
                showConfigDialog();
            }
        }
        return false;
    }

    public void LoadConfig() {
        new Thread() {
            @Override
            public void run() {
                String text = Http.Get(config.JS_HOST + "config.txt", new HttpCallback() {
                    @Override
                    public void result(String text) {
                        if(text !=null && text.length()>0) {
                            PlayUrls = text.split("\\n");
                            Play();
                        }
                        else {
                            toast("获取频道错误!请检查网络是否正常!",Toast.LENGTH_LONG);
                        }
                    }
                });

            }
        }.start();
    }


}
