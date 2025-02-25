package com.pharaoh.tvplay;

public class PlayInfo {
    public static final String Desktop_USER_AGENT  = "Mozilla/5.0 (X11; Linux Arm; rv:109.0) Chrome/120.0.0.0";
    public String url;

    // 内核类型 0=chrome 1=firefox
    public int type = 0;
    public Boolean desktop = false;
    public Boolean showImage = false;
    public int current;

    public PlayInfo(String u, int cur) {
        current = cur;
        String u1 = u;
        String useragent = null;
        boolean firefox = false;
        if(u.contains("##")) {
            int index = u.indexOf("##"); // 获取第一个 # 的索引位置
            u1 = u.substring(0, index); // 第一部分：# 前的字符串
            useragent = u.substring(index + 2); // 第二部分：第一个 # 后的所有内容
            firefox = useragent.contains("webview=firefox");
            desktop = useragent.contains("desktop=1");
            showImage = useragent.contains("image=1");
        }
        this.url = u1;
        this.type = firefox?1:0;
    }
}
