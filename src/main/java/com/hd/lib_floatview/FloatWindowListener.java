package com.hd.lib_floatview;

public interface FloatWindowListener {
    //点击事件
    void onClick();
    //左滑事件
    void onSwipeLeft();
    //右滑事件
    void onSwipeRight();
    //双击事件
    void onDoubleClick();
    //长按事件
    void onLongClick();

    //移动事件进行中，非必须实现
    void onMoveing();
    //移动事件完成，非必须实现
    void onMoved();

    //缩放事件进行中，非必须实现
    void onZooming();
    //缩放事件完成，非必须实现
    void onZoomed();


    //左侧浮动
    void onLeftFloat();
    //右侧浮动
    void onRightFloat();
    //顶部浮动
    void onTopFloat();
    //底部浮动
    void onBottomFloat();


    //睡眠事件，非必须实现
    void onSleep();
    //唤醒事件，非必须实现
    void onWake();

    //关闭事件，非必须实现
    void onClose();
    //进入编辑模式
    void onInputEdit();
    //退出编辑模式
    void onCloseEdit();
}
