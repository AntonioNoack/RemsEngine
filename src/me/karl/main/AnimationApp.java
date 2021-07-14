package me.karl.main;

import org.lwjgl.glfw.GLFW;
import me.karl.renderEngine.RenderEngine;
import me.karl.scene.DAEScene;

public class AnimationApp {

	public static long window;

	/**
	 * Initialises the engine and loads the scene. For every frame it updates the
	 * camera, updates the animated entity (which updates the animation),
	 * renders the scene to the screen, and then updates the display. When the
	 * display is close the engine gets cleaned up.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		RenderEngine engine = RenderEngine.init();

		DAEScene scene = SceneLoader.loadScene(GeneralSettings.RES_FOLDER);

		while (!GLFW.glfwWindowShouldClose(window)) {
			scene.getCamera().move();
			scene.getAnimatedModel().update();
			engine.renderScene(scene);
			engine.update();
		}

		engine.close();

	}

}
