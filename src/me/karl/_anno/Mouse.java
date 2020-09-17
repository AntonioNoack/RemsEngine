package me.karl._anno;

public class Mouse {
    static int downKeys = 0;
    static float dx, dy;
    static int lastX, lastY;
    public static boolean isButtonDown(int button){
        int flag = 1 << button;
        return (downKeys & flag) == flag;
    }
    public static float getDX(){
        return dx*10f/Display.width;
    }
    public static float getDY(){
        return dy*10f/Display.width;
    }
}
