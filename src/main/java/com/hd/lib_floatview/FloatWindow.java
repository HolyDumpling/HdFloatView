package com.hd.lib_floatview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

/*
* 问题：系统浮窗的视图切换时不流畅，一卡一卡的
* 当浮窗内容视图中核心控制View不贴近屏幕边缘时，睡眠和唤醒肯定会导致偏移，待解决
* */

public abstract class FloatWindow extends RelativeLayout implements FloatWindowListener {
    protected int sssss;
    //长按时间判定
    private static final int longClickEffectiveTime = 300;
    //左侧吸附
    public static final int ADSORPTION_TYPE_LEFT = 1;
    //右侧吸附
    public static final int ADSORPTION_TYPE_RIGHT = 2;
    //左右吸附
    public static final int ADSORPTION_TYPE_LR = 0;
    //吸附方式
    private int adsorptionType = ADSORPTION_TYPE_LR;

    protected Context mContext;
    protected View mView;
    //浮动窗参数
    protected FloatWindowParams params;
    //系统浮动窗参数
    protected WindowManager.LayoutParams wmParams;
    //系统浮动窗管理器
    protected WindowManager windowManager;


    private boolean enableVibrator = true;//震动使能开关

    private boolean enableEdit = true;//编辑使能开关
    private boolean isEditing = false;//正在编辑状态

    private boolean enableMove = true;//移动使能开关
    private boolean isMoving = false;//正在移动中

    private boolean enableSleep = true;//睡眠使能开关
    private boolean isSleeping = false;//正在睡眠中

    //UI绘制Handler
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public FloatWindow(@NonNull Context context, @NonNull FloatWindowParams floatWindowParams) {
        super(context);
        this.sssss = 50;
        mContext= context;
        mView = this;
        this.params = floatWindowParams;
        this.params.screenWidth = FloatWindowUtils.getScreenWidth(mContext);
        this.params.screenHeight = FloatWindowUtils.getScreenHeight(mContext);
        this.params.width = setDefineWidthDp();
        this.params.height = setDefineHeightDp();
        this.params.y = setDefineTopMargin();
        initViews();
    }

    public FloatWindow(@NonNull Context context, @NonNull FloatWindowParams floatWindowParams, @Nullable WindowManager windowManager, @Nullable WindowManager.LayoutParams wmParams) {
        super(context);
        this.sssss = 170;
        mContext= context;
        mView = this;
        this.wmParams = wmParams;
        this.windowManager = windowManager;
        this.params = floatWindowParams;
        this.params.screenWidth = FloatWindowUtils.getScreenWidth(mContext);
        this.params.screenHeight = FloatWindowUtils.getScreenHeight(mContext);
        this.params.width = setDefineWidthDp();
        this.params.height = setDefineHeightDp();
        this.params.y = setDefineTopMargin();
        if(wmParams!=null){
            this.wmParams.width = setDefineWidthDp();
            this.wmParams.height = setDefineHeightDp();
            this.wmParams.y = setDefineTopMargin();
        }
        initViews();
    }

    //设置吸附方式
    public void setAdsorptionType(int adsorptionType) {
        if(adsorptionType<ADSORPTION_TYPE_LR||adsorptionType>ADSORPTION_TYPE_RIGHT)
            this.adsorptionType = ADSORPTION_TYPE_LR;
        else
            this.adsorptionType = adsorptionType;
    }

    void printf(String msg){
        if(wmParams==null){
            FloatWindowUtils.i("内部浮窗："+msg);
        } else {
            FloatWindowUtils.i("系统浮窗："+msg);
        }
    }

    public void initViews(){
        FrameLayout contentView = new FrameLayout(getContext());
        initContentView(contentView);
        addView(contentView);

        if(!isSleeping)
            postRunableDelayed(inSleepRun,2000);
    }

    void postRunable(Runnable runnable){
        uiHandler.removeCallbacks(runnable);
        uiHandler.post(runnable);
    }
    void postRunableDelayed(Runnable runnable,long delayMillis){
        uiHandler.removeCallbacks(runnable);
        uiHandler.postDelayed(runnable,delayMillis);
    }

    //进入睡眠模式
    private Runnable inSleepRun = new Runnable() {
        @Override
        public void run() {
            isSleeping = true;
            isEditing = false;
            onSleep();
        }
    };
    //设置定位
    private Runnable layoutRun = new Runnable() {
        @Override
        public void run() {
            if (wmParams!=null) {
                wmParams.width = (int) params.width;
                wmParams.height = (int) params.height;
                wmParams.x = (int) params.x;
                wmParams.y = (int) params.y;
                windowManager.updateViewLayout(mView,wmParams);
            } else {
                layout((int) params.getLeft(), (int) params.getTop(), (int) params.getRight(), (int) params.getBottom());
            }
        }
    };
    //退出编辑模式
    private Runnable closeEditRun = new Runnable() {
        @Override
        public void run() {
            isEditing = false;
            onCloseEdit();
        }
    };




    int newWidth,newHeight;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(params==null)
            return;
        if(wmParams==null){
            newWidth = MeasureSpec.getSize(widthMeasureSpec);
            newHeight = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            newWidth = FloatWindowUtils.getScreenWidth(mContext);
            newHeight = FloatWindowUtils.getScreenHeight(mContext,false) - FloatWindowUtils.getStatusBarHeight(mContext);
        }
        if(newWidth!=params.screenWidth || newHeight!=params.screenHeight){
            adsorption();
            params.screenWidth = newWidth;
            params.screenHeight = newHeight;
            if(isSleeping)
                postRunable(inSleepRun);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //当视图布局改变时，视图会刷新宽高填充屏幕（不清楚原因），此时要修正回布局改变前的定位，但是屏幕会卡一下
        if((right - left)>params.width||(bottom - top)>params.height){
            layout((int)params.getLeft(), (int)params.getTop(), (int)params.getRight(), (int)params.getBottom());
        }else
            super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //当此处为设置true或为控件设置点击事件后，dispatchTouchEvent和onTouch才会持续触发，否则只触发一次（不被捕获，没有处理）
        return true;
    }

    long swipeStartTime;//手势落下时间
    float swipeStartX,swipeStartY;//手势落下坐标
    @Override
    //根视图手势判断（左滑右滑）
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://手指按下
                swipeStartTime = System.currentTimeMillis();
                swipeStartX = event.getRawX();
                swipeStartY = event.getRawY();
                Rect rect = new Rect();
                coreView.getGlobalVisibleRect(rect);
                //不在移动的view内，不处理
                if (isSleeping&&!rect.contains((int) swipeStartX, (int) swipeStartY)) {
                    FloatWindowUtils.i("不在核心View中，不做处理");
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP://手指松开
                long differenceTime = System.currentTimeMillis() - swipeStartTime;
                float differenceX = event.getRawX()-swipeStartX;
                float diffAbsX = Math.abs(differenceX);
                float diffAbsY = Math.abs(event.getRawY()-swipeStartY);
                double angle = Math.atan2(diffAbsY,diffAbsX) * 180 / Math.PI;
                if(!isMoving&&differenceTime<500 && angle<45 && diffAbsX>10 && diffAbsX<500){
                    if(differenceX <0)
                        onSwipeLeft();
                    else
                        onSwipeRight();
                    onLongClick();
                    return true;
                }
                break;
        }
        if(enableSleep && !isSleeping){
            postRunableDelayed(inSleepRun,3000);
        }
        return super.dispatchTouchEvent(event);
    }

    long downActionTime,moveActionTime,upActionTime;
    //触摸坐标（屏幕）
    float x_Screen,y_Screen;
    //点击屏幕坐标（屏幕）
    float x_ClickScreen,y_ClickScreen;
    //触摸移动坐标（屏幕）
    float x_MoveScreen, y_MoveScreen;

    //手指按下
    public void coreViewTouchDown(MotionEvent event) {
        downActionTime = System.currentTimeMillis();
        x_ClickScreen = x_Screen = event.getRawX();
        y_ClickScreen = y_Screen = event.getRawY();
        //LogUtils.i("手势，手指按下：X："+event.getRawX()+"，Y:"+event.getRawY());
        //清除延时触发器
        uiHandler.removeCallbacks(closeEditRun);
        uiHandler.removeCallbacks(inSleepRun);
    }

    //手指移动
    public void coreViewTouchMove(MotionEvent event) {
        moveActionTime = System.currentTimeMillis();
        x_MoveScreen = event.getRawX();
        y_MoveScreen = event.getRawY();
        //LogUtils.i("手势，手指移动：X："+event.getRawX()+"，Y:"+event.getRawY());
        //如果允许编辑，且不在编辑状态，长按持续时间大于longClickEffectiveTime，距离初始点击位移距离小于30，则进入编辑模式
        //LogUtils.i("长按距离："+(Math.abs(x_MoveScreen-x_ClickScreen))+","+(Math.abs(y_MoveScreen-y_ClickScreen)));
        if(enableEdit&&!isEditing
                &&moveActionTime - downActionTime>longClickEffectiveTime
                &&Math.abs(x_MoveScreen-x_ClickScreen)<30
                &&Math.abs(y_MoveScreen-y_ClickScreen)<30){
            isEditing = true;
            onInputEdit();
            startVibrator();
        }
        //如果在编辑状态，并且允许移动
        if(isEditing && enableMove) {
            isMoving = true;
            moveViewPosition();
            hideBtn();
            if(params.height!=coreView.getHeight()){
                params.height = coreView.getHeight();
                coreViewY = coreView.getY();
                params.y += coreViewY;
            }
            onMoveing();
        }
    }
    //手指松开
    public void coreViewTouchUp(MotionEvent event) {
        upActionTime = System.currentTimeMillis();
        x_Screen = event.getRawX();
        y_Screen = event.getRawY();
        //LogUtils.i("手势，手指松开：X："+event.getRawX()+"，Y:"+event.getRawY());
        //退出移动模式
        if(isMoving){
            isMoving = false;
            adsorption();
        }
        //2000毫秒后退出编辑模式
        if(isEditing){
            uiHandler.removeCallbacks(closeEditRun);
            uiHandler.postDelayed(closeEditRun,2000);
        }

        if(enableSleep && !isSleeping){
            uiHandler.removeCallbacks(inSleepRun);
            uiHandler.postDelayed(inSleepRun,3000);
        }
    }

    //更新视图位置的函数
    private synchronized void moveViewPosition(){
        float differenceX = x_MoveScreen - x_Screen;
        float differenceY = y_MoveScreen - y_Screen;
        float differenceWidth = params.width - coreView.getWidth();

        params.width = coreView.getWidth();

        if(isLeft()!=params.isLeft){
            params.x = params.x + differenceX ;
            if(params.isLeft) {
                onRightFloat();
            } else {
                onLeftFloat();
            }
            params.isLeft = !params.isLeft;
        } else {
            float newX;
            if(params.isLeft){
                newX = params.x + differenceX ;
            } else {
                newX = params.x + differenceX + differenceWidth;
            }
            float newRight = newX + coreView.getWidth();
            if(newX>=0 && newRight<=params.screenWidth)
                params.x = newX;
        }
        float newY = params.y + differenceY ;
        float newBottom = newY + getHeight() ;
        if(newY>=0 && newBottom<=(params.screenHeight)){
            params.y = newY;
        }

        postRunable(layoutRun);

        x_Screen = x_MoveScreen;
        y_Screen = y_MoveScreen;
    }


    //吸附到边框
    private synchronized void adsorption() {
        boolean isLeft = isLeft();
        if(params.screenWidth==0)
            isLeft = setDefineIsLeft();
        params.screenWidth = newWidth;
        params.screenHeight = newHeight;
        params.width = setDefineWidthDp();
        if(adsorptionType==ADSORPTION_TYPE_LEFT)
            isLeft = true;
        else if(adsorptionType==ADSORPTION_TYPE_RIGHT)
            isLeft = false;

        //吸附到边框
        if(isLeft) {
            onLeftFloat();
            params.x = 0;
            params.isLeft = true;
        } else {
            onRightFloat();
            params.x = params.screenWidth - params.width;
            params.isLeft = false;
        }

        if(params.screenHeight>0 && params.getBottom()>params.screenHeight)
            params.y = params.screenHeight - params.height;

        onWake();
        onMoved();
    }

    //震动发生函数
    private void startVibrator(){
        if(enableVibrator && getContext()!=null){
            Vibrator vibrator = (Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if(vibrator!=null)
                vibrator.vibrate(50);
        }
    }









    private boolean coreViewTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://手指按下
                coreViewTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE://手指移动
                coreViewTouchMove(event);
                break;
            case MotionEvent.ACTION_UP://手指松开
                coreViewTouchUp(event);
                return true;
        }
        return true;
    }


    protected View coreView;

    //指定浮窗控制区
    public void setCoreView(View coreView){
        if(coreView!=null) {
            this.coreView = coreView;
            coreView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return coreViewTouchEvent(motionEvent);
                }
            });
            printf("设置核心View，宽高："+coreView.getWidth()+"，"+coreView.getHeight());
        }
    }

    //判断控件在左侧还是右侧
    public boolean isLeft(){
        if(params==null)
            return true;
        int contentX = (int)params.x + (int)coreView.getX() + coreView.getWidth()/2;
        return contentX < params.screenWidth / 2.0;
    }

    float coreViewY;

    @Override
    public void onSleep(){
        hideBtn();
        int subW = coreView.getWidth()/2;
        printf("睡了，核心宽高："+coreView.getWidth()+"，"+coreView.getHeight());
        params.width = coreView.getWidth();
        FloatWindowUtils.i("睡了，核心视图的X"+coreView.getX()+",Y"+coreView.getY());
        params.height = coreView.getHeight();
        coreViewY = coreView.getY();
        params.y += coreViewY;
        if(isLeft())
            params.x = -subW;
        else
            params.x = params.screenWidth - subW;
        postRunable(layoutRun);
    }

    @Override
    public void onWake(){
        printf("进入唤醒模式");
        isSleeping = false;
        showBtn();
        params.width = setDefineWidthDp();
        params.height = setDefineHeightDp();
        params.y -= coreViewY;
        printf("唤醒,核心视图的上次记录X："+",本次x："+params.x+"，屏幕宽度："+params.screenWidth+"，视图宽度："+params.width);
        if(isLeft()) {
            params.x = 0;
            params.isLeft = true;
        }else {
            params.x = params.screenWidth - params.width;
            params.isLeft = false;
        }
        postRunable(layoutRun);
        //睡眠使能打开，且不在睡眠状态，则进入睡眠
        if(enableSleep){
            postRunableDelayed(inSleepRun,3000);
        }



        /**
         * 整理思路
         *
         *
         * */
    }


    /**
     * 设置悬浮窗默认宽度（dp单位）
     * */
    public abstract void initContentView(ViewGroup rootView);
    /**
     * 设置悬浮窗默认宽度（dp单位）
     * */
    protected abstract int setDefineWidthDp();
    /**
     * 设置悬浮窗默认高度（dp单位）
     * */
    protected abstract int setDefineHeightDp();
    /**
     * 设置悬浮窗默认位置（dp单位）
     * */
    protected abstract int setDefineTopMargin();
    /**
     * 设置悬浮窗默认方向（左侧：true，右侧：fe）
     * */
    protected abstract boolean setDefineIsLeft();
    /**
     * 需要隐藏的功能按钮
     * */
    public abstract void hideBtn();
    /**
     * 需要显示的功能按钮
     * */
    public abstract void showBtn();
    /**
     * 左侧浮动布局
     * */
    public abstract void onLeftFloat();
    /**
     * 右侧浮动布局
     * */
    public abstract void onRightFloat();


    @Override
    public void onClick() {

    }

    @Override
    public void onSwipeLeft() {
        FloatWindowUtils.i("父控件左滑");
        if(enableSleep) {
            if(isLeft()) {
                isSleeping = true;
                isEditing = false;
                onSleep();
            } else {
                isSleeping = false;
                onWake();
            }
        }
    }

    @Override
    public void onSwipeRight() {
        FloatWindowUtils.i("父控件右滑");
        if(enableSleep){
            if(isLeft()) {
                isSleeping = false;
                onWake();
            }else {
                isSleeping = true;
                isEditing = false;
                onSleep();
            }
        }
    }

    @Override
    public void onMoveing() {

    }

    @Override
    public void onMoved() {

    }
    @Override
    public void onDoubleClick() {

    }

    @Override
    public void onLongClick() {

    }

    @Override
    public void onInputEdit() {
    }

    @Override
    public void onCloseEdit() {
    }
    @Override
    public void onZooming() {

    }

    @Override
    public void onZoomed() {

    }

    @Override
    public void onTopFloat() {

    }

    @Override
    public void onBottomFloat() {

    }

    @Override
    public void onClose() {

    }

}
