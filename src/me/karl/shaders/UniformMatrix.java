package me.karl.shaders;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

public class UniformMatrix extends Uniform{
	
	private static FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

	public UniformMatrix(String name) {
		super(name);
	}
	
	public void loadMatrix(Matrix4f matrix){
		if(matrix == null){
			// no data available...
			for(int i=0;i<16;i++){
				matrixBuffer.put(i % 5 == 0 ? 1f : 0f);
			}
		} else {
			for(int i=0;i<16;i++){
				matrixBuffer.put(matrix.get(i/4, i&3));
			}
		}
		matrixBuffer.flip();
		GL20.glUniformMatrix4fv(super.getLocation(), false, matrixBuffer);
	}

}
