package dev.aurora.tvpatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared TV panel styling, matching the graphic EQ palette and remote focus. */
public final class PanelUi {
    public static final int BLUE=0xff659efa, MUTED=0xffaebac4, BACKGROUND=0xff0c1117;
    private PanelUi() {}
    public static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static GradientDrawable shape(Context c,int fill,int stroke,int radius){
        GradientDrawable result=new GradientDrawable();result.setColor(fill);result.setCornerRadius(dp(c,radius));result.setStroke(dp(c,1),stroke);return result;
    }
    public static void decorate(View view){
        view.setBackground(shape(view.getContext(),BACKGROUND,0xff2c3b4b,20));view.setClipToOutline(true);view.setElevation(dp(view.getContext(),12));
    }
    public static LinearLayout stack(Context c){LinearLayout result=new LinearLayout(c);result.setOrientation(LinearLayout.VERTICAL);result.setBackgroundColor(Color.TRANSPARENT);return result;}
    public static LinearLayout panel(Context c){LinearLayout result=stack(c);result.setPadding(dp(c,24),dp(c,18),dp(c,24),dp(c,18));decorate(result);return result;}
    public static TextView text(Context c,String value,int size,int color){TextView result=new TextView(c);result.setText(value);result.setTextSize(size);result.setTextColor(color);result.setBackgroundColor(Color.TRANSPARENT);result.setPadding(0,dp(c,6),0,dp(c,6));return result;}
    public static Button button(Context c,String label,Runnable action){
        Button result=new Button(c);result.setText(label);result.setTextSize(15);result.setAllCaps(false);result.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff6e7c8b,Color.WHITE}));result.setMinHeight(dp(c,44));result.setPadding(dp(c,18),dp(c,8),dp(c,18),dp(c,8));
        StateListDrawable states=new StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},shape(c,0xff101923,0xff263442,12));states.addState(new int[]{android.R.attr.state_focused},shape(c,0xff244466,BLUE,12));states.addState(new int[]{android.R.attr.state_pressed},shape(c,0xff244466,BLUE,12));states.addState(new int[]{},shape(c,0xff172330,0xff34485b,12));result.setBackground(states);result.setOnClickListener(v->action.run());return result;
    }
    public static AlertDialog show(Activity a,View body,float width,float height){
        decorate(body);AlertDialog dialog=new AlertDialog.Builder(a).setView(body).create();dialog.show();Window window=dialog.getWindow();
        if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setDimAmount(.72f);window.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*width),height>0?(int)(a.getResources().getDisplayMetrics().heightPixels*height):ViewGroup.LayoutParams.WRAP_CONTENT);}
        return dialog;
    }
    public static AlertDialog dialog(Activity a,String title,View content,boolean tall){
        LinearLayout body=panel(a),toolbar=new LinearLayout(a);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setBackgroundColor(Color.TRANSPARENT);
        TextView heading=text(a,title,22,Color.WHITE);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);toolbar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        final AlertDialog[] owner=new AlertDialog[1];Button close=button(a,"Close",()->{if(owner[0]!=null)owner[0].dismiss();});close.setId(View.generateViewId());toolbar.addView(close);body.addView(toolbar);
        View line=new View(a);line.setBackgroundColor(0xff263748);LinearLayout.LayoutParams rule=new LinearLayout.LayoutParams(-1,dp(a,1));rule.setMargins(0,dp(a,12),0,dp(a,12));body.addView(line,rule);
        if(content instanceof android.widget.ScrollView){if(content.getId()==View.NO_ID)content.setId(View.generateViewId());close.setNextFocusDownId(content.getId());content.setNextFocusUpId(close.getId());}
        body.addView(content,new LinearLayout.LayoutParams(-1,tall?0:-2,tall?1:0));owner[0]=show(a,body,.86f,tall?.88f:0);close.requestFocus();return owner[0];
    }
}
