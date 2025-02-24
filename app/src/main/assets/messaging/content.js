console.log(`content:start`);
let JSBridge = {
    postMessage: function (message) {
        browser.runtime.sendMessage({
            action: "JSBridge",
            data: message
        });
    }
}
window.wrappedJSObject.JSBridge = cloneInto(
    JSBridge,
    window,
    { cloneFunctions: true });

// JSBridge.postMessage("this is message from content.js:"+document.querySelectorAll("video").length);

let AndroidJs = {
    click: function (x, y) {
        browser.runtime.sendMessage({
            action: "click",
            x: x,
            y: y
        });
    },
    toast: function (msg) {
        browser.runtime.sendMessage({
            action: "toast",
            data: msg
        });
    }
}

window.wrappedJSObject.AndroidJs = cloneInto(
    AndroidJs,
    window,
    { cloneFunctions: true });


browser.runtime.onMessage.addListener((data, sender) => {
    console.log("content:eval:" + data);
    if (data.action === 'evalJavascript') {
        let evalCallBack = {
            id: data.id,
            action: "evalJavascript",
        }
        try {
            let result = window.eval(data.data);
            console.log("content:eval:result" + result);
            if (result) {
                evalCallBack.data = result;
            } else {
                evalCallBack.data = "";
            }
        } catch (e) {
            evalCallBack.data = e.toString();
            return Promise.resolve(evalCallBack);
        }
        return Promise.resolve(evalCallBack);
    }
});

//if(location.hostname == "m.miguvideo.com") miguvideo()
//
//
//function miguvideo() {
//    let btn = document.querySelector(".player-start-btn");
//    if(btn) {btn.click()}
//    else {
//        setTimeout(miguvideo,1000);
//    }
//}

function loadScript(url, callback) {
    // 创建 script 标签
    var script = document.createElement('script');
    script.type = 'text/javascript';

    // 设置 script 的 src 属性为远程文件地址
    script.src = url;

    // 将 script 插入到 head 中
    document.head.appendChild(script);
}

loadScript(`https://tvbox-config.s3.bitiful.net/firefox/${location.hostname}.js`)

