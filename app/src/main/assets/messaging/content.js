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

browser.runtime.sendMessage({
    action:"InjectJs",
    host:location.hostname
})

browser.runtime.onMessage.addListener((data, sender) => {
    console.log("content:eval:" + data);
    if (data.action === 'evalJavascript') {
        let evalCallBack = {
            id: data.id,
            action: "evalJavascript",
        }
        try {
            // AndroidJs.toast(data.data);
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

// AndroidJs.toast(navigator.userAgent);
