package me.karl.skybox;

import me.anno.gpu.blending.BlendDepth;
import me.anno.gpu.blending.BlendMode;
import org.lwjgl.opengl.GL11;

import me.karl.openglObjects.Vao;
import me.karl.scene.ICamera;
import me.karl.utils.OpenGlUtils;

public class SkyboxRenderer {

	private static final float SIZE = 200;

	private final SkyboxShader shader;
	private final Vao box;

	public SkyboxRenderer() {
		this.shader = new SkyboxShader();
		this.box = CubeGenerator.generateCube(SIZE);
	}

	/**
	 * Renders the skybox.
	 * 
	 * @param camera
	 *            - the scene's camera.
	 */
	public void render(ICamera camera) {
		prepare(camera);
		box.bind(0);
		GL11.glDrawElements(GL11.GL_TRIANGLES, box.getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
		box.unbind(0);
		shader.stop();
	}

	/**
	 * Delete the shader when the game closes.
	 */
	public void cleanUp() {
		shader.cleanUp();
	}

	/**
	 * Starts the shader, loads the projection-view matrix to the uniform
	 * variable, and sets some OpenGL state which should be mostly
	 * self-explanatory.
	 * 
	 * @param camera
	 *            - the scene's camera.
	 */
	private void prepare(ICamera camera) {
		shader.start();
		shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
		// BlendDepth bd = new BlendDepth(null, true, true);
		// bd.bind();
		// OpenGlUtils.disableBlending();
		// OpenGlUtils.enableDepthTesting(true);
		// OpenGlUtils.cullBackFaces(true);
		// OpenGlUtils.antialias(false);
	}

}
