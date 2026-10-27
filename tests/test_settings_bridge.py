"""Run the production bridge against a minimal Android lifecycle fake (no preinstalled JS bridge)."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
STUBS={
 'android/app/Activity.java':'package android.app; public class Activity { public boolean isFinishing(){return false;} }',
 'android/os/Looper.java':'package android.os; public class Looper { public static Looper getMainLooper(){return new Looper();} }',
 'android/os/Handler.java':'package android.os; public class Handler {public Handler(Looper l){} public void post(Runnable r){r.run();}}',
 'android/net/Uri.java':'package android.net; public class Uri {java.net.URI u; public static Uri parse(String s){Uri r=new Uri();r.u=java.net.URI.create(s);return r;} public String getHost(){return u.getHost();}}',
 'android/content/Context.java':'package android.content; public class Context {public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}}',
 'android/content/res/AssetManager.java':'package android.content.res; public class AssetManager { public java.io.InputStream open(String s){return new java.io.ByteArrayInputStream(new byte[0]);} }',
 'android/webkit/JavascriptInterface.java':'package android.webkit; public @interface JavascriptInterface {}',
 'android/webkit/WebView.java':'package android.webkit; public class WebView {public String url;public Object bridge; public int registrations; public String getUrl(){return url;} public void addJavascriptInterface(Object b,String name){bridge=b;registrations++;} public android.content.Context getContext(){return new android.content.Context();} public void evaluateJavascript(String code,Object callback){} }',
 'dev/aurora/tvpatch/Hooks.java':'package dev.aurora.tvpatch; public class Hooks {public static int calls;public static android.app.Activity activeActivity(){return new android.app.Activity();} public static void settingsAction(android.app.Activity a,String s){calls++;} public static org.json.JSONObject settingsSnapshot(android.content.Context c){return new org.json.JSONObject();}}',
 'BridgeLifecycle.java':'''import dev.aurora.tvpatch.NativeSettings;import dev.aurora.tvpatch.Hooks;import android.webkit.WebView;
public class BridgeLifecycle {public static void main(String[] a)throws Exception {
 WebView v=new WebView(); NativeSettings.register(v);
 assert v.registrations==1 : "Interface must exist before navigation when URL is null";
 java.lang.reflect.Method action=v.bridge.getClass().getDeclaredMethod("action",String.class);action.setAccessible(true);
 action.invoke(v.bridge,"theme");assert Hooks.calls==0 : "Blank documents cannot invoke settings";
 v.url="https://api-partner.spotify.com/tvapp?platform=androidtv";
 action.invoke(v.bridge,"theme");assert Hooks.calls==1 : "First trusted document can invoke settings without reload";
 NativeSettings.register(v);assert v.registrations==1 : "Repeated attachment must not replace bridge";
 v.url="https://example.com/";action.invoke(v.bridge,"theme");assert Hooks.calls==1 : "Untrusted navigation is rejected";
 System.out.println("Bridge lifecycle: first-load registration and invocation-time trust passed");
}}''',
}
class BridgeLifecycleTest(unittest.TestCase):
 def test_first_document_works_and_untrusted_documents_cannot_invoke(self):
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory);files=[]
   for name,source in STUBS.items():
    path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(source);files.append(str(path))
   files.append(str(ROOT/'src/dev/aurora/tvpatch/NativeSettings.java'))
   subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',str(ROOT/'.tools/json.jar'),'-d',str(root/'classes'),*files],check=True)
   subprocess.run(['java','-ea','-cp',str(root/'classes')+':'+str(ROOT/'.tools/json.jar'),'BridgeLifecycle'],check=True)
if __name__=='__main__':unittest.main()
