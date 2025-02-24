package com.pharaoh.tvplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.GeckoResult;

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
                toast(url);
                if(url.contains("##")) {
                    int index = url.indexOf("##"); // 获取第一个 # 的索引位置
                    u1 = url.substring(0, index); // 第一部分：# 前的字符串
                    useragent = url.substring(index + 2); // 第二部分：第一个 # 后的所有内容
                    // toast(useragent);
                    session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
                    session.getSettings().setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
                }
                else {
//                    session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
//                    session.getSettings().setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
                }
                //session.getSettings().setUserAgentOverride(useragent);

                view.releaseSession();
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


        session = new GeckoSession();

// Workaround for Bug 1758212
        session.setContentDelegate(new GeckoSession.ContentDelegate() {});

        // 启用自动播放
        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public GeckoResult<Integer> onContentPermissionRequest(GeckoSession session, ContentPermission perm) {
                if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE ||
                        perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE) {
                    return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
                }
                return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
                // return super.onContentPermissionRequest(session, perm);
            }
        });

        // session.PermissionDelegate = new

        // session.getSettings().setJavaScriptEnabled(true);
        // 设置页面加载监听器
        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {

            @Override
            public void onPageStart(GeckoSession session, String url) {
                //super.onPageStart(session,url);
                // evaluateJavascript("window.appMessage('onPageStart')");
//                String jsCode = "alert('Hello from onPageStart!');";
//                session.loadUri("javascript:" + jsCode);
            }

            @Override
            public void onPageStop(GeckoSession session, boolean success) {
                // super.onPageLoad(session, success);
                // evaluateJavascript("window.appMessage('on page stop')");
                // Toast.makeText(GeckoViewActivity.this, "on page stop", Toast.LENGTH_SHORT).show();
//                // 页面加载完成后执行 JavaScript
//                String jsCode = "alert('Hello from onPageStop!');";
//                session.loadUri("javascript:" + jsCode);
            }
        });
        session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);

        if (sRuntime == null) {
            // GeckoRuntime can only be initialized once per process
            sRuntime = GeckoRuntime.create(this);
            sRuntime.getSettings().setRemoteDebuggingEnabled(true);

            installExtension();

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

    // js

    private final WebExtension.MessageDelegate mMessagingDelegate = new WebExtension.MessageDelegate() {

        @Override
        public void onConnect(WebExtension.Port port) {
            Log.e("MessageDelegate", "onConnect");
            mPort = port;
            mPort.setDelegate(mPortDelegate);
        }
    };

    private final WebExtension.PortDelegate mPortDelegate = new WebExtension.PortDelegate() {
        @Override
        public void onPortMessage(final Object message, final WebExtension.Port port) {
            Log.e("MessageDelegate", "Received message from extension: " + message);
            try {
                if (message instanceof JSONObject) {
                    Log.e("MessageDelegate", "Received JSONObject");
                    JSONObject jsonObject = (JSONObject) message;
                    String action = jsonObject.getString("action");
                    // toast("onPortMessage:"+action);
                    switch (action) {
                        case "InjectJs" -> {
                            String host = jsonObject.getString("host");
                            new Thread() {
                                @Override
                                public void run() {
                                    Http.Get(config.JS_HOST + host + ".js", new HttpCallback() {
                                        @Override
                                        public void result(String txt) {
                                            if(txt != null && !txt.isEmpty()) {
                                                // toast("InjectJs:"+host);
                                                evaluateJavascript(txt);
                                            }
                                        }
                                    });
                                }
                            }.start();

                        }
                        case "JSBridge" -> {
                            String data = jsonObject.getString("data");
                            Toast.makeText(GeckoViewActivity.this, data, Toast.LENGTH_LONG).show();
                        }
                        case "toast" -> {
                            String data1 = jsonObject.getString("data");
                            Toast.makeText(GeckoViewActivity.this, data1, Toast.LENGTH_LONG).show();
                        }
                        case "click" -> {
                            float x = (float) jsonObject.getDouble("x");
                            float y = (float) jsonObject.getDouble("y");
                            simulateClickAtCoordinate(x, y, 1000);
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnect(final WebExtension.Port port) {
            Log.e("MessageDelegate", "onDisconnect");
            if (port == mPort) {
                mPort = null;
            }
        }
    };

    private WebExtension.Port mPort;

    public void installExtension() {
        sRuntime.getWebExtensionController()
                .ensureBuiltIn("resource://android/assets/messaging/", "messaging@example.com")
                .accept(
                        extension -> {
                            Log.i("MessageDelegate", "Extension installed: " + extension);
                            runOnUiThread(() -> extension.setMessageDelegate(mMessagingDelegate, "browser"));
                        },
                        e -> {
                            Log.e("MessageDelegate", "Error registering WebExtension", e);
                        }
                );
    }

    public void evaluateJavascript(String javascriptString) {
        try {
            long id = System.currentTimeMillis();
            Log.e("evalJavascript:id:", String.valueOf(id));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("action", "evalJavascript");
            jsonObject.put("data", javascriptString);
            jsonObject.put("id", id);
            Log.e("evalJavascript:", jsonObject.toString());
            mPort.postMessage(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}