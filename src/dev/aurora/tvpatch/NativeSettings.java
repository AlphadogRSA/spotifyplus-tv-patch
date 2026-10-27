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
    private static String focusMode="native";
    private static boolean menuVisible;
    private static final java.util.Set<Integer> routedKeys=new java.util.HashSet<>();
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
        attached=new WeakReference<>(view);signature="";focusMode="native";menuVisible=false;routedKeys.clear();
        view.addJavascriptInterface(new Object() {
            @JavascriptInterface public void focus(String mode,boolean profile) {
                if(!"native".equals(mode) && !"menu".equals(mode) && !"profile".equals(mode) && !"text".equals(mode))return;
                UI.post(() -> {
                    if(attached.get()!=view || !trusted(view))return;
                    boolean entered=profile && !menuVisible;focusMode=mode;menuVisible=profile;
                    if(entered)refresh(view,true);
                });
            }
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
        if(!trusted(view) || (!force && !menuVisible))return;
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
    public static boolean textEntry(WebView view) {return view!=null && attached.get()==view && "text".equals(focusMode) && trusted(view);}
    public static boolean bypassTextKey(WebView view,int code,boolean down) {
        if(!textEntry(view))return false;
        if(down){routedKeys.remove(code);return true;}
        return !routedKeys.contains(code);
    }
    public static boolean navigation(WebView view,int code,boolean down,Runnable fallback) {
        boolean pairedRelease=!down && routedKeys.remove(code);
        if(view==null || !trusted(view) || (!pairedRelease && ("text".equals(focusMode) || ("native".equals(focusMode) && !menuVisible))))return false;
        String key=code==19 ? "ArrowUp" : code==20 ? "ArrowDown" : code==21 ? "ArrowLeft" : code==22 ? "ArrowRight" : (code==23 || code==66) ? "Enter" : null;
        if(key==null)return false;
        if(down)routedKeys.add(code);
        view.evaluateJavascript("!!(window.__sptvSettings && window.__sptvSettings.dispatch(\""+key+"\","+down+"))",result -> {
            if(!"true".equals(result) && attached.get()==view && trusted(view))fallback.run();
        });
        return true;
    }
    public static void open(WebView view) {
        if(trusted(view))view.evaluateJavascript("window.__sptvSettings ? window.__sptvSettings.open() : false",null);
    }
}
