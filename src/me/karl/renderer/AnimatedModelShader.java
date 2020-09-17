package me.karl.renderer;

import me.karl.shaders.ShaderProgram;
import me.karl.shaders.UniformMat4Array;
import me.karl.shaders.UniformMatrix;
import me.karl.shaders.UniformSampler;
import me.karl.shaders.UniformVec3;

public class AnimatedModelShader extends ShaderProgram {

	private static final int MAX_JOINTS = 120;// max number of joints in a skeleton
	private static final int DIFFUSE_TEX_UNIT = 0;

	private static final String VERTEX_SHADER = "" +
			"#version 150\n" +

			"const int MAX_JOINTS = "+MAX_JOINTS+";// max joints allowed in a skeleton\n" +
			"const int MAX_WEIGHTS = 3;// max number of joints that can affect a vertex\n" +

			"in vec3 in_position;\n" +
			"in vec2 in_textureCoords;\n" +
			"in vec3 in_normal;\n" +
			"in ivec3 in_jointIndices;\n" +
			"in vec3 in_weights;\n" +

			"out vec2 pass_textureCoords;\n" +
			"out vec3 pass_normal;\n" +

			"uniform mat4 jointTransforms[MAX_JOINTS];\n" +
			"uniform mat4 transform;\n" +

			"void main(void){\n" +

			"	vec4 totalLocalPos = vec4(0.0);\n" +
			"	vec4 totalNormal = vec4(0.0);\n" +

			"	for(int i=0;i<MAX_WEIGHTS;i++){\n" +
			"		mat4 jointTransform = jointTransforms[in_jointIndices[i]];\n" +
			"		vec4 posePosition = jointTransform * vec4(in_position, 1.0);\n" +
			"		totalLocalPos += posePosition * in_weights[i];\n" +
			"		\n" +
			"		vec4 worldNormal = jointTransform * vec4(in_normal, 0.0);\n" +
			"		totalNormal += worldNormal * in_weights[i];\n" +
			"	}\n" +

			"	gl_Position = transform * totalLocalPos;\n" +
			"	pass_normal = totalNormal.xyz;\n" +
			"	pass_textureCoords = in_textureCoords;\n" +

			"}";

	private static final String FRAGMENT_SHADER = "" +
			"#version 150\n" +

			"const vec2 lightBias = vec2(0.7, 0.6);//just indicates the balance between diffuse and ambient lighting\n" +

			"in vec2 pass_textureCoords;\n" +
			"in vec3 pass_normal;\n" +

			"out vec4 out_colour;\n" +

			"uniform sampler2D diffuseMap;\n" +
			"uniform vec3 lightDirection;\n" +

			"void main(void){\n" +
			"	vec4 diffuseColour = texture(diffuseMap, pass_textureCoords);\n" +
			"	vec3 unitNormal = normalize(pass_normal);\n" +
			"	float diffuseLight = max(dot(-lightDirection, unitNormal), 0.0) * lightBias.x + lightBias.y;\n" +
			"	out_colour = diffuseColour * diffuseLight;\n" +
			"}";

	protected UniformMatrix projectionViewMatrix = new UniformMatrix("transform");
	protected UniformVec3 lightDirection = new UniformVec3("lightDirection");
	protected UniformMat4Array jointTransforms = new UniformMat4Array("jointTransforms", MAX_JOINTS);
	private final UniformSampler diffuseMap = new UniformSampler("diffuseMap");

	/**
	 * Creates the shader program for the {@link AnimatedModelRenderer} by
	 * loading up the vertex and fragment shader code files. It also gets the
	 * location of all the specified uniform variables, and also indicates that
	 * the diffuse texture will be sampled from texture unit 0.
	 */
	public AnimatedModelShader() {
		super(VERTEX_SHADER, FRAGMENT_SHADER, "in_position", "in_textureCoords", "in_normal", "in_jointIndices",
				"in_weights");
		super.storeAllUniformLocations(projectionViewMatrix, diffuseMap, lightDirection, jointTransforms);
		connectTextureUnits();
	}

	/**
	 * Indicates which texture unit the diffuse texture should be sampled from.
	 */
	private void connectTextureUnits() {
		super.start();
		diffuseMap.loadTexUnit(DIFFUSE_TEX_UNIT);
		super.stop();
	}

}
