package dev.aurora.tvpatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Native graphic control: TV remote, touch, and accessibility all change the same stored curve. */
public final class GraphicEqView extends View {
    private static final int BLUE = 0xff659efa, MUTED = 0xffaebac4;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences prefs;
    private final float density;
    private int[] gains;
    private int selected;
    private boolean enabled;
    private float left, right, top, bottom, gainY;

    public GraphicEqView(Context c, SharedPreferences prefs) {
        super(c); this.prefs = prefs; density = getResources().getDisplayMetrics().density;
        gains = GraphicEqSettings.read(prefs); enabled = prefs.getBoolean("dspEnabled", false);
        setFocusable(true); setFocusableInTouchMode(true); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        describe();
    }
    private float dp(float value) { return value * density; }
    public void enabled(boolean value) { enabled = value; invalidate(); describe(); }
    public void reset() {
        gains = new int[GraphicEq.BANDS];
        SharedPreferences.Editor edit = prefs.edit().putInt("boostMb", 0).putBoolean("graphicEqMigrated", true);
        for (int i = 0; i < gains.length; i++) edit.putInt("graphicEq" + i, 0);
        for (int i = 0; i < 5; i++) edit.putInt("eq" + i, 0);
        edit.apply(); AudioDsp.refresh(); invalidate(); describe();
    }
    private float x(int band) { return left + (right-left) * band / (GraphicEq.BANDS-1); }
    private float y(int gain) { return top + (bottom-top) * (GraphicEq.LIMIT-gain) / (GraphicEq.LIMIT*2f); }
    private void color(int color, float stroke) {
        paint.setShader(null); paint.setPathEffect(null); paint.setStyle(Paint.Style.FILL);
        paint.setColor(color); paint.setStrokeWidth(dp(stroke));
    }
    private void label(Canvas c, String text, float x, float y, int color, float size, Paint.Align align) {
        color(color, 1); paint.setTextSize(dp(size)); paint.setTextAlign(align); c.drawText(text, x, y, paint);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        left = dp(54); right = w-dp(32); top = dp(42); bottom = Math.max(top+dp(70), h-dp(115)); gainY = h-dp(29);
        color(0xff252f36, 1); c.drawRoundRect(new RectF(0,0,w,h), dp(24), dp(24), paint);
        for (int value = -600; value <= 600; value += 300) {
            color(0xff3a4650, 1); paint.setPathEffect(new DashPathEffect(new float[]{dp(4),dp(5)},0));
            c.drawLine(left, y(value), right, y(value), paint);
            label(c, Integer.toString(value/100), left-dp(15), y(value)+dp(4), MUTED, 12, Paint.Align.RIGHT);
        }
        for (int i = 0; i < gains.length; i++) {
            color(0xff4b5660, 4); paint.setStrokeCap(Paint.Cap.ROUND); c.drawLine(x(i), top, x(i), bottom, paint);
        }
        Path curve = new Path(); curve.moveTo(left, y(gains[0]));
        for (int i=1;i<gains.length;i++) {
            float mid = (x(i-1)+x(i))/2;
            curve.cubicTo(mid,y(gains[i-1]),mid,y(gains[i]),x(i),y(gains[i]));
        }
        Path fill = new Path(curve); fill.lineTo(right,bottom); fill.lineTo(left,bottom); fill.close();
        color(BLUE,1); paint.setShader(new LinearGradient(0,top,0,bottom,0x70659efa,0x05659efa,Shader.TileMode.CLAMP));
        paint.setAlpha(enabled?255:100); c.drawPath(fill,paint);
        color(enabled?BLUE:0xff778591,3); paint.setStyle(Paint.Style.STROKE); c.drawPath(curve,paint);
        for (int i = 0; i < gains.length; i++) {
            float point = y(gains[i]);
            color(enabled?BLUE:0xff778591,4); c.drawLine(x(i),point,x(i),bottom,paint);
            c.drawCircle(x(i),point,dp(8),paint);
            if (hasFocus() && selected==i) {
                color(Color.WHITE,2); paint.setStyle(Paint.Style.STROKE); c.drawCircle(x(i),point,dp(13),paint);
            }
            label(c,String.format(Locale.ROOT,"%.1f",gains[i]/100f),x(i),top-dp(17),Color.WHITE,13,Paint.Align.CENTER);
            int hz=GraphicEq.frequency(i);
            label(c,hz>=1000?(hz/1000)+"K":Integer.toString(hz),x(i),bottom+dp(25),MUTED,13,Paint.Align.CENTER);
        }
        label(c,"Post-gain",left,bottom+dp(65),Color.WHITE,16,Paint.Align.LEFT);
        int boost=Math.max(0,Math.min(600,prefs.getInt("boostMb",0)));
        label(c,String.format(Locale.ROOT,"+%.1f dB",boost/100f),right,bottom+dp(65),MUTED,14,Paint.Align.RIGHT);
        color(0xff4b5660,4); c.drawLine(left,gainY,right,gainY,paint);
        float thumb=left+(right-left)*boost/600f;
        color(enabled?BLUE:0xff778591,4); c.drawLine(left,gainY,thumb,gainY,paint); c.drawCircle(thumb,gainY,dp(9),paint);
        if (hasFocus() && selected==GraphicEq.BANDS) {
            color(Color.WHITE,2); paint.setStyle(Paint.Style.STROKE); c.drawCircle(thumb,gainY,dp(14),paint);
        }
    }
    private void adjust(int direction) {
        if (selected==GraphicEq.BANDS) {
            int value=Math.max(0,Math.min(600,prefs.getInt("boostMb",0)+direction*50));
            prefs.edit().putInt("boostMb",value).apply();
        } else {
            gains[selected]=GraphicEq.adjust(gains[selected],direction);
            prefs.edit().putInt("graphicEq"+selected,gains[selected]).apply();
        }
        AudioDsp.refresh(); invalidate(); describe();
    }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT) {
            selected=Math.max(0,Math.min(GraphicEq.BANDS,selected+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)));
            invalidate(); describe(); return true;
        }
        if (key==KeyEvent.KEYCODE_DPAD_UP || key==KeyEvent.KEYCODE_DPAD_DOWN) {
            adjust(key==KeyEvent.KEYCODE_DPAD_UP?1:-1); return true;
        }
        if (key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER) {
            if (selected==GraphicEq.BANDS) {
                View toolbar=focusSearch(View.FOCUS_UP);
                if (toolbar!=null && toolbar!=this) toolbar.requestFocus();
            } else selected=GraphicEq.BANDS;
            invalidate(); describe(); return true;
        }
        return super.onKeyDown(key,event);
    }
    @Override public boolean onKeyUp(int key, KeyEvent event) {
        if (key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT || key==KeyEvent.KEYCODE_DPAD_UP
                || key==KeyEvent.KEYCODE_DPAD_DOWN || key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER) return true;
        return super.onKeyUp(key,event);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (right<=left) return true;
        int action=event.getActionMasked();
        if (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE) {
            requestFocus();
            if (event.getY()>bottom+dp(35)) {
                selected=GraphicEq.BANDS;
                int value=Math.round((event.getX()-left)/(right-left)*12)*50;
                prefs.edit().putInt("boostMb",Math.max(0,Math.min(600,value))).apply();
            } else {
                selected=Math.max(0,Math.min(GraphicEq.BANDS-1,Math.round((event.getX()-left)/(right-left)*(GraphicEq.BANDS-1))));
                gains[selected]=GraphicEq.clamp(Math.round((1-(event.getY()-top)/(bottom-top))*24-12)*50);
                prefs.edit().putInt("graphicEq"+selected,gains[selected]).apply();
            }
            AudioDsp.refresh(); invalidate(); describe(); return true;
        }
        if(action==MotionEvent.ACTION_UP) { performClick(); return true; }
        return action==MotionEvent.ACTION_CANCEL || super.onTouchEvent(event);
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    private void describe() {
        String selectedText=selected==GraphicEq.BANDS?"Post-gain "+prefs.getInt("boostMb",0)/100f+" dB"
                :GraphicEq.frequency(selected)+" hertz "+gains[selected]/100f+" dB";
        setContentDescription(selectedText+". Left and right select. Up and down adjust. OK switches to post-gain, then toolbar. EQ "+(enabled?"on":"off"));
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info); info.setClassName("android.widget.SeekBar");
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }
    @Override public boolean performAccessibilityAction(int action, android.os.Bundle args) {
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            adjust(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1); return true;
        }
        return super.performAccessibilityAction(action,args);
    }
}
