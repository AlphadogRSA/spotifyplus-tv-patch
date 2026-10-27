package dev.aurora.tvpatch;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/** Fixed settings actions only; the bridge never accepts code, shell commands or URLs. */
public final class NativeSettings {
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
    public static void attach(WebView view,Activity activity) {
        if(!trusted(view))return;
        if(attached.get()!=view) {
            attached=new WeakReference<>(view);signature="";
            WeakReference<Activity> owner=new WeakReference<>(activity);
            view.addJavascriptInterface(new Object() {
                @JavascriptInterface public void action(String action) {
                    if(action==null || action.length()>24)return;
                    Activity a=owner.get();if(a==null)return;
                    a.runOnUiThread(() -> { if(!a.isFinishing() && trusted(view))Hooks.settingsAction(a,action); });
                }
            },"SpotifyPlusSettings");
        }
        // addJavascriptInterface becomes visible on the next page load. page() also calls attach;
        // attachment at Activity creation registers the bridge before the signed-in page loads.
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
    public static void open(WebView view) {
        if(trusted(view))view.evaluateJavascript("window.__sptvSettings ? window.__sptvSettings.open() : false",null);
    }
}
