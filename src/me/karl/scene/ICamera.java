package me.karl.scene;

import org.joml.Matrix4f;

public interface ICamera {
	
	Matrix4f getViewMatrix();
	Matrix4f getProjectionMatrix();
	Matrix4f getProjectionViewMatrix();
	void move();

}
