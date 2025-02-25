package com.pharaoh.tvplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity {
    // LinearLayout mWebContainer;
    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PlayControl.Instance.setActivity(this);
        // toast("Chrome!!!");

        // mWebContainer = (LinearLayout)findViewById(R.id.webViewParent);

        mWebView = (WebView) findViewById(R.id.webview);
        initWebSettings();

        initJsInterface();

        initWebChromeClient();

        initWebViewClient();

        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return PlayControl.Instance.DoTouch(event);
            }
        });

        PlayInfo pinfo = PlayControl.Instance.playInfo;

        TextView txtChannel = (TextView)findViewById(R.id.txtChannel);
        txtChannel.setText(""+pinfo.current);

        if(pinfo.desktop) {
            // toast("set desktop!!!");
            mWebView.getSettings().setUserAgentString(PlayInfo.Desktop_USER_AGENT);
        }
        if(pinfo.showImage) {
            mWebView.getSettings().setLoadsImagesAutomatically(true);
        }
        mWebView.loadUrl(pinfo.url);
        PlayControl.Instance.CloseGecko();
    }

    @Override
    protected void onPause() {
        super.onPause(); // 调用父类方法
        destroyWebView();
        finish(); // 销毁当前Activity
    }

    private void destroyWebView() {
        // toast("❌destroyWebView chrome");
        if(mWebView != null) {

            //mWebContainer.removeView(mWebView);
            mWebView.removeAllViews();
            //mWebView.loadData("Null","text/plain","utf8");
            mWebView.loadUrl("about:blank");
            mWebView.onPause();
            mWebView.removeAllViews();
            mWebView.pauseTimers();
            mWebView.destroy();
            mWebView = null;
        }
    }

    private void initWebSettings() {
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        //webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        WebView.setWebContentsDebuggingEnabled(true);
    }

    private void initWebViewClient() {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view,String url) {
                try {
                    URL u = new URL(url);
                    String  host = u.getHost();
                    String ujs =  PlayControl.Instance.getHostJs(host);
                    //Toast.makeText(MainActivity.this,"JS:"+ujs,Toast.LENGTH_LONG).show();
                    new Thread() {
                        @Override
                        public void run() {
                            String text = Http.Get(ujs, new HttpCallback() {
                                @Override
                                public void result(String text) {
                                    if(text !=null && text.length()>0) {
                                        MainActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mWebView.evaluateJavascript(text,null);
                                            }
                                        });

                                    }
                                }
                            });

                        }
                    }.start();
                } catch (MalformedURLException e) {
                    //throw new RuntimeException(e);
                }
            }
        });
    }

    private void initWebChromeClient() {
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // 此处的 view 就是全屏的视频播放界面，需要把它添加到我们的界面上
                //Toast.makeText(MainActivity.this,"full screen",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onHideCustomView() {
                // 退出全屏播放，我们要把之前添加到界面上的视频播放界面移除
                Toast.makeText(MainActivity.this,"Exit full screen",Toast.LENGTH_LONG).show();
            }

            @Override
            public  boolean onJsPrompt(WebView view, String url, String message,String defaultValue, JsPromptResult result) {
                if(message.equals("httpget")) {
                    new Thread() {
                        @Override
                        public void run() {
                            String text = Http.Get(defaultValue, new HttpCallback() {
                                @Override
                                public void result(String txt) {
                                    result.confirm(txt);
                                }
                            });

                        }
                    }.start();
                }
                return true;
            }
        });
    }

    private void initJsInterface() {
        mWebView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void toast(final String msg) {
                MainActivity.this.toast(msg);
            }

            @JavascriptInterface
            public void close() {
                MainActivity.this.finish();
            }

            @JavascriptInterface
            public void click(final float x,final float y) {
                simulateClickAtCoordinate(mWebView,x,y,1000);
            }

            @JavascriptInterface
            public void setUseragent(final String useragent) {
                mWebView.getSettings().setUserAgentString(useragent);
            }

            @JavascriptInterface
            public void playM3u8(final String u,final String packName,final String className,final String type) {
                Uri uri = Uri.parse(u);
                Intent i = new Intent(Intent.ACTION_VIEW,uri);
                i.setPackage(packName);
                i.setClassName(packName,className);
                i.setDataAndType(uri,type);
                //MainActivity.this.toast(i.toUri(0),Toast.LENGTH_LONG);
                MainActivity.this.setClipText(i.toUri(0));

                MainActivity.this.startActivity(i);
            }

            @JavascriptInterface
            public void playIntent(final String intent) {
                try {
                    Intent i = Intent.parseUri(intent,0);
                    MainActivity.this.startActivity(i);
                } catch (Exception e) {
                    MainActivity.this.toast("Error:"+e.getMessage());
                }
            }


        }, "AndroidJs");
    }
    public void toast(final String msg, final int time) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, time).show();
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
            return PlayControl.Instance.DoKey(keyCode); // DoKey(keyCode);
        }
        return super.dispatchKeyEvent(event);
    }
    public void simulateClickAtCoordinate(WebView webView, float x, float y,int sleepTime) {
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
        webView.dispatchTouchEvent(downEvent);
        webView.dispatchTouchEvent(upEvent);

        // 回收事件对象
        downEvent.recycle();
        upEvent.recycle();
    }

}