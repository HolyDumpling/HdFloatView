package com.hd.lib_floatview;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class FloatWindowManager {
    //根Activity
    private Activity activity;
    //浮动控件实例
    private FloatWindow floatWindow1;
    //浮动控件实例
    private FloatWindow floatWindow2;
    //浮动控件类名
    private String floatWindowName;
    //浮动控件参数
    private static FloatWindowParams floatViewParams1;
    //浮动控件参数
    private static FloatWindowParams floatViewParams2;
    //内容视图
    private FrameLayout contentView;

    //系统窗口管理器
    private WindowManager windowManager;

    private FloatWindowManager(){ }
    public static FloatWindowManager getInstance(){ return FloatWindowManagerHolder.sInstance; }
    private static class FloatWindowManagerHolder{
        private static final FloatWindowManager sInstance = new FloatWindowManager();
    }

    /**
     * 显示悬浮窗口
     * @param baseActivity 上下文
     */
    public synchronized FloatWindow showFloatWindow(Activity baseActivity, String className) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (baseActivity == null)
            return null;
        if(floatViewParams1 == null)
            floatViewParams1 = new FloatWindowParams();

        Context mContext = baseActivity.getApplicationContext();//得到App上下文
        dismissFloatWindow();
        if(floatWindow1==null || !floatWindowName.equals(className)){
            floatWindowName = className;
            Class clazz = Class.forName(className);
            Constructor con = clazz.getDeclaredConstructor(Context.class,FloatWindowParams.class);
            floatWindow1 = (FloatWindow) con.newInstance(mContext,floatViewParams1);
        }

        activity = baseActivity;
        View rootView = activity.getWindow().getDecorView().getRootView();
        contentView = rootView.findViewById(android.R.id.content);
        contentView.addView(floatWindow1);
        return floatWindow1;
    }

    public synchronized void dismissFloatWindow(){
        if(activity!=null && floatWindow1!=null)
            contentView.removeView(floatWindow1);
    }


    public void showSysFloatWindow(Context mContext, String className) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException{
        if(floatViewParams2 == null)
            floatViewParams2 = new FloatWindowParams();

        Context appContext = mContext.getApplicationContext();
        windowManager = FloatWindowUtils.getWindowManager(appContext);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.packageName = mContext.getPackageName();


        /**
         * FLAG_NOT_TOUCH_MODAL：屏幕上弹窗之外的地方能够点击、弹窗上的EditText也可以输入、键盘能够弹出来。
         * FLAG_KEEP_SCREEN_ON：保持屏幕常亮
         * FLAG_NOT_FOCUSABLE：Window不需要获取焦点,也不需要接收任何输入事件
         * FLAG_SCALED：弹出窗口特殊模式，布局参数用于指示显示比例。
         * FLAG_ALT_FOCUSABLE_IM：窗口不能与输入法交互，覆盖输入法窗口。（同时设置FLAG_NOT_FOCUSABLE，窗口将能够与输入法交互，输入法窗口覆盖）
         * FLAG_LAYOUT_INSET_DECOR：确保窗口内容不会被装饰条（状态栏）盖住。
         * FLAG_LAYOUT_IN_SCREEN：弹出窗口占满整个屏幕，忽略周围的装饰边框（例如状态栏）。
         * FLAG_LAYOUT_NO_LIMITS：允许弹出窗口扩展到屏幕之外。
         * FLAG_COMPATIBLE_WINDOW ：以原始尺寸显示窗口。
         * FLAG_HARDWARE_ACCELERATED：开启硬件加速
         * */

        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SCALED
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;


        //悬浮窗权限
        if (Build.VERSION.SDK_INT >= 26) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        wmParams.gravity = Gravity.START | Gravity.TOP;
        wmParams.format = PixelFormat.RGBA_8888;


        if(floatWindow2==null || !floatWindowName.equals(className)){
            floatWindowName = className;
            Class clazz = Class.forName(className);
            Constructor con = clazz.getDeclaredConstructor(Context.class,FloatWindowParams.class,WindowManager.class,WindowManager.LayoutParams.class);
            floatWindow2 = (FloatWindow) con.newInstance(mContext,floatViewParams2,windowManager,wmParams);
        }

        try {
            windowManager.addView(floatWindow2, wmParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
