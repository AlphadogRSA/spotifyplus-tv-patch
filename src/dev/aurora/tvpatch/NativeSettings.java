package dev.aurora.tvpatch;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/** Fixed settings actions only; the bridge never accepts code, shell commands or URLs. */
public final class NativeSettings {
    private static final Handler UI=new Handler(Looper.getMainLooper());
    private static String script;
    private static WeakReference<WebView> attached=new WeakReference<>(null);
    private static String signature="";
    private NativeSettings() {}
    static boolean trusted(WebView view) {
        String url=view.getUrl();
        if(url==null)return false;
        if(url.startsWith("file:///android_asset/"))return true;
        if(!url.startsWith("https://"))return false;
        String host=android.net.Uri.parse(url).getHost();
        return host!=null && (host.equals("spotify.com")||host.endsWith(".spotify.com")||host.equals("tv.scdn.co"));
    }
    public static void register(WebView view) {
        // Register before loadUrl. A new WebView legitimately has no trusted URL yet.
        if(attached.get()==view)return;
        attached=new WeakReference<>(view);signature="";
        view.addJavascriptInterface(new Object() {
            @JavascriptInterface public void action(String action) {
                if(action==null || action.length()>24)return;
                UI.post(() -> {
                    Activity a=Hooks.activeActivity();
                    if(a!=null && !a.isFinishing() && attached.get()==view && trusted(view))Hooks.settingsAction(a,action);
                });
            }
        },"SpotifyPlusSettings");
    }
    public static void attach(WebView view,Activity activity) {
        register(view);
        refresh(view,true);
    }
    public static void refresh(WebView view,boolean force) {
        if(!trusted(view))return;
        try {
            JSONObject values=Hooks.settingsSnapshot(view.getContext());String json=values.toString();
            if(!force && json.equals(signature))return;signature=json;
            if(script==null)try(InputStream input=view.getContext().getAssets().open("spotifyplus-settings.js");ByteArrayOutputStream output=new ByteArrayOutputStream()) {
                byte[] bytes=new byte[4096];int read;while((read=input.read(bytes))!=-1)output.write(bytes,0,read);
                script=output.toString("UTF-8");
            }
            view.evaluateJavascript("("+script+")("+json+")",null);
        }catch(Exception ignored){}
    }
    public static boolean remote(WebView view, int code, boolean down) {
        if(view==null || !trusted(view))return false;
        String url=view.getUrl();
        if(url==null || !url.matches(".*#/profile/?(?:[?].*)?$"))return false;
        String key=code==19 ? "ArrowUp" : code==20 ? "ArrowDown" : (code==23 || code==66) ? "Enter" : null;
        if(key==null)return false;
        if(down)view.evaluateJavascript("window.__sptvSettings && window.__sptvSettings.remote(\""+key+"\")",null);
        return true;
    }
    public static void open(WebView view) {
        if(trusted(view))view.evaluateJavascript("window.__sptvSettings ? window.__sptvSettings.open() : false",null);
    }
}
