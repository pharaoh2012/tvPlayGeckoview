package com.pharaoh.tvplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class GeckoViewActivity extends Activity {

    private static GeckoRuntime sRuntime;
    GeckoSession session;
    GeckoView view;

    Config config;

    long lastBackTime = 0;
    //private GestureDetector mGestureDetector;
    String[] PlayUrls = null;

    private float[] lastTouchDownXY = new float[2];

    private  String getUrl() {
        int length = PlayUrls.length;
        int currentCCTV = config.getCurrentCCTV();
        if(currentCCTV>length) currentCCTV=1;
        if(currentCCTV < 1) currentCCTV = length;
        config.setCurrentCCTV(currentCCTV);
        toast("播放"+ currentCCTV +"频道",Toast.LENGTH_LONG);
        return PlayUrls[currentCCTV-1].trim();
    }

    private void Play() {
        GeckoViewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String url = getUrl();
                String u1 = url;
                String useragent = null;
                if(url.contains("##")) {
                    int index = url.indexOf("##"); // 获取第一个 # 的索引位置
                    u1 = url.substring(0, index); // 第一部分：# 前的字符串
                    useragent = url.substring(index + 2); // 第二部分：第一个 # 后的所有内容
                }
                session.getSettings().setUserAgentOverride(useragent);
                view.setSession(session);
                session.loadUri(u1);
            }
        });
    }

    private void LoadConfig() {
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
                            GeckoViewActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //mWebView.loadData("Null","text/plain","utf8");
                                    session.loadUri("about:blank");
                                }
                            });

                        }
                    }
                });

            }
        }.start();
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
        session.loadUri(getUrl());
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gecko_view);

        Http.init(this);
        config = new Config(this);


        view = findViewById(R.id.geckoview);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

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
        });
//        view.setOnLongClickListener(new View.OnLongClickListener() {
//
//            @Override
//            public boolean onLongClick(View v) {
//                //PlayNext();
//                toast("onLongClick");
//                int h = view.getHeight()/2;
//                if(lastTouchDownXY[1]<h) {
//                    showConfigDialog();
//                }
//                else {
//                    int w = view.getWidth()/2;
//                    if(lastTouchDownXY[0]<w) {
//                        PlayPre();
//                    } else {
//                        PlayNext();
//                    }
//                }
//                //showConfigDialog();
//                return true;
//            }
//        });


        session = new GeckoSession();

// Workaround for Bug 1758212
        session.setContentDelegate(new GeckoSession.ContentDelegate() {});

        // session.PermissionDelegate = new

        // session.getSettings().setJavaScriptEnabled(true);
        // 设置页面加载监听器
        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {

            @Override
            public void onPageStart(GeckoSession session, String url) {
                //super.onPageStart(session,url);
                String jsCode = "alert('Hello from onPageStart!');";
                session.loadUri("javascript:" + jsCode);
            }

            @Override
            public void onPageStop(GeckoSession session, boolean success) {
                // super.onPageLoad(session, success);
                Toast.makeText(GeckoViewActivity.this, "on page stop", Toast.LENGTH_SHORT).show();
                // 页面加载完成后执行 JavaScript
                String jsCode = "alert('Hello from onPageStop!');";
                session.loadUri("javascript:" + jsCode);
            }
        });

        if (sRuntime == null) {
            // GeckoRuntime can only be initialized once per process
            sRuntime = GeckoRuntime.create(this);
        }


        session.open(sRuntime);


        //session.getSettings().setUserAgentOverride("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0");
        LoadConfig();
        // session.loadUri("https://m.miguvideo.com/m/liveDetail/608807420?channelId=10010001005"); // Or any other URL...
        //session.loadUri("https://www.yangshipin.cn/tv/home?pid=600001800"); // Or any other URL...
    }

    public void toast(final String msg, final int time) {
        GeckoViewActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GeckoViewActivity.this, msg, time).show();
            }
        });
    }

    public void toast(final String msg) {
        toast(msg,Toast.LENGTH_SHORT);
    }

    public void setClipText(String txt) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, txt));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            return DoKey(keyCode);
        }
        return super.dispatchKeyEvent(event);
    }
    public boolean DoKey(int keyCode) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - lastBackTime > 2000) {
                lastBackTime = System.currentTimeMillis();
                toast("再按一次返回键关闭");
            }
            else {
                this.finish();
            }
            return true;
        }
        Toast.makeText(this,"KeyCode:"+keyCode,Toast.LENGTH_SHORT).show();
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
        final EditText editText = new EditText(this);
        editText.setSingleLine();
        editText.setHint("设置地址");
        editText.requestFocus();
        editText.setFocusable(true);
        editText.setText(config.JS_HOST);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this)
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

    public void simulateClickAtCoordinate(float x, float y, int sleepTime) {
        long downTime = System.currentTimeMillis();
        long eventTime = System.currentTimeMillis();
        toast("click:"+x+","+y+" delay="+sleepTime);
        // 创建按下事件 (ACTION_DOWN)
        MotionEvent downEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0
        );

        // 创建抬起事件 (ACTION_UP)
        MotionEvent upEvent = MotionEvent.obtain(
                downTime + sleepTime,
                eventTime + sleepTime, // 稍微延迟以模拟真实点击
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        );

        // 分发事件到 WebView
        view.dispatchTouchEvent(downEvent);
        view.dispatchTouchEvent(upEvent);

        // 回收事件对象
        downEvent.recycle();
        upEvent.recycle();
    }

    @Override
    protected void onPause() {
        super.onPause(); // 调用父类方法
        closeGeckoView();
        finish(); // 销毁当前Activity
    }

    private void closeGeckoView() {
        if (session != null) {
            session.close();
            session = null;
        }

        if (sRuntime != null) {
            sRuntime.shutdown();
            sRuntime = null;
        }
    }

}