package me.karl.skybox;

import me.karl.shaders.ShaderProgram;
import me.karl.shaders.UniformMatrix;

public class SkyboxShader extends ShaderProgram {

	private static final String VERTEX_SHADER = "" +
			"#version 150\n" +

			"in vec3 in_position;\n" +
			"out float pass_height;\n" +

			"uniform mat4 projectionViewMatrix;\n" +

			"void main(void){\n" +
			"	gl_Position = projectionViewMatrix * vec4(in_position, 1.0);\n" +
			"	pass_height = in_position.y;\n" +
			"}";

	private static final String FRAGMENT_SHADER = "" +
			"#version 150\n" +

			"const vec4 colour1 = vec4(0.88, 0.67, 0.219, 1.0);\n" +
			"const vec4 colour2 = vec4(0.2, 0.6, 0.7, 1.0);\n" +

			"in float pass_height;\n" +

			"float smoothlyStep(float edge0, float edge1, float x){\n" +
			"    float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);\n" +
			"    return t * t * (3.0 - 2.0 * t);\n" +
			"}\n" +

			"void main(void){\n" +
			"	float fadeFactor = 1.0 - smoothlyStep(-50.0, 70.0, pass_height);\n" +
			"	vec4 color = mix(colour2, colour1, fadeFactor), tint = vec4(1.0);\n" +
			"	float zDistance = 1.0;\n" +
			"	gl_FragColor = color;\n" +
			"}";

	protected UniformMatrix projectionViewMatrix = new UniformMatrix("projectionViewMatrix");

	public SkyboxShader() {
		super("skybox", VERTEX_SHADER, FRAGMENT_SHADER, "in_position");
		super.storeAllUniformLocations(projectionViewMatrix);
	}
}
