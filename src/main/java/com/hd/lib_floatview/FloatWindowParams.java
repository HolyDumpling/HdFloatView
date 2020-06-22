package com.hd.lib_floatview;

public class FloatWindowParams {
    public boolean isLeft;//左侧视图
    public int screenWidth;//屏幕宽度
    public int screenHeight;//屏幕高度
    public float x;//窗口的左上角x坐标
    public float y;//窗口的左上角y坐标
    public float width;//窗口的宽
    public float height;//窗口的高

    public float getLeft() { return x; }
    public float getTop() { return y; }
    public float getRight() {
        return x+width;
    }
    public float getBottom() {
        return y+height;
    }
}
