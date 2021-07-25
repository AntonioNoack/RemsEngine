package me.karl.utils;

public class DisplayManager {

	private static long lastFrameTime;
	private static float delta;

	public static void createDisplay() {
		lastFrameTime = getCurrentTime();
	}

	public static void update() {
		long currentFrameTime = getCurrentTime();
		delta = (currentFrameTime - lastFrameTime) / 1000f;
		lastFrameTime = currentFrameTime;
	}

	public static float getFrameTime() {
		return delta;
	}

	public static void closeDisplay() {
		// Display.destroy();
	}

	private static long getCurrentTime() {
		return System.currentTimeMillis();
	}

}
