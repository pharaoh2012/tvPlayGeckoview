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
import android.widget.TextView;
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


    GeckoSession session;
    GeckoView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gecko_view);
        PlayControl.Instance.setActivity(this);
        // toast("Firefox!!!");


        view = findViewById(R.id.geckoview);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return PlayControl.Instance.DoTouch(event);
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


        PlayInfo pinfo = PlayControl.Instance.playInfo;
        TextView txtChannel = (TextView)findViewById(R.id.txtChannel);
        txtChannel.setText(""+pinfo.current);
        // txtChannel.setTextColor();
        if(pinfo.desktop) {
            // toast("firefox set desktop!!!");
            session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
            session.getSettings().setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
        }

        if (PlayControl.sRuntime == null) {
            // GeckoRuntime can only be initialized once per process
            PlayControl.sRuntime = GeckoRuntime.create(this);
            PlayControl.sRuntime.getSettings().setRemoteDebuggingEnabled(true);

            installExtension();

        }

        view.setSession(session);

        session.open(PlayControl.sRuntime);

        session.loadUri(pinfo.url);


        //session.getSettings().setUserAgentOverride("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0");
        // LoadConfig();
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
            return PlayControl.Instance.DoKey(keyCode);
        }
        return super.dispatchKeyEvent(event);
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
        // toast("❌ close geckview");
        if (session != null) {
            session.close();
            session = null;
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
                                    Http.Get(PlayControl.Instance.getHostJs(host), new HttpCallback() {
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
        PlayControl.sRuntime.getWebExtensionController()
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