package me.karl.shaders;

import java.io.BufferedReader;

import me.anno.gpu.shader.Shader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import me.karl.utils.URI;

public class ShaderProgram {

	private final int programID;

	public ShaderProgram(String vertexFile, String fragmentFile, String... inVariables) {
		int vertexShaderID = loadShader(vertexFile, null, GL20.GL_VERTEX_SHADER);
		int fragmentShaderID = loadShader(fragmentFile, null, GL20.GL_FRAGMENT_SHADER);
		programID = GL20.glCreateProgram();
		init(vertexShaderID, fragmentShaderID, inVariables);
	}

	public ShaderProgram(URI vertexFile, URI fragmentFile, String... inVariables) {
		int vertexShaderID = loadShader(vertexFile, GL20.GL_VERTEX_SHADER);
		int fragmentShaderID = loadShader(fragmentFile, GL20.GL_FRAGMENT_SHADER);
		programID = GL20.glCreateProgram();
		init(vertexShaderID, fragmentShaderID, inVariables);
	}

	private void init(int vertexShaderID, int fragmentShaderID, String... inVariables){
		GL20.glAttachShader(programID, vertexShaderID);
		GL20.glAttachShader(programID, fragmentShaderID);
		bindAttributes(inVariables);
		GL20.glLinkProgram(programID);
		GL20.glDetachShader(programID, vertexShaderID);
		GL20.glDetachShader(programID, fragmentShaderID);
		GL20.glDeleteShader(vertexShaderID);
		GL20.glDeleteShader(fragmentShaderID);
	}
	
	protected void storeAllUniformLocations(Uniform... uniforms){
		for(Uniform uniform : uniforms){
			uniform.storeUniformLocation(programID);
		}
		GL20.glValidateProgram(programID);
	}

	public void start() {
		Shader.Companion.setLastProgram(programID);
		GL20.glUseProgram(programID);
	}

	public void stop() {
		Shader.Companion.setLastProgram(0);
		GL20.glUseProgram(0);
	}

	public void cleanUp() {
		stop();
		GL20.glDeleteProgram(programID);
	}

	private void bindAttributes(String[] inVariables){
		for(int i=0;i<inVariables.length;i++){
			GL20.glBindAttribLocation(programID, i, inVariables[i]);
		}
	}

	private int loadShader(CharSequence shaderSource, URI file, int type) {
		int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, shaderSource);
		GL20.glCompileShader(shaderID);
		if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println(GL20.glGetShaderInfoLog(shaderID, 500));
			System.err.println(file == null ?
					"Could not compile shader\n"+shaderSource :
					"Could not compile shader "+ file);
			System.exit(-1);
		}
		return shaderID;
	}
	
	private int loadShader(URI file, int type) {
		StringBuilder shaderSource = new StringBuilder();
		try {
			BufferedReader reader = file.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("//\n");
			}
			reader.close();
		} catch (Exception e) {
			System.err.println("Could not read file.");
			e.printStackTrace();
			System.exit(-1);
		}
		return loadShader(shaderSource, file, type);
	}


}
