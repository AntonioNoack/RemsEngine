package me.karl.main;

import me.karl._anno.Display;
import me.karl._anno.Mouse;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import me.karl.scene.ICamera;
import me.karl.utils.DisplayManager;
import me.karl.utils.SmoothFloat;

/**
 * Represents the in-game camera. This class is in charge of keeping the
 * projection-view-matrix updated. It allows the user to alter the pitch and yaw
 * with the left mouse button.
 * 
 * @author Karl
 *
 */
public class Camera implements ICamera {

	private static final float PITCH_SENSITIVITY = 0.3f;
	private static final float YAW_SENSITIVITY = 0.3f;
	private static final float MAX_PITCH = 90;

	private static final float FOV = 70;
	private static final float NEAR_PLANE = 0.2f;
	private static final float FAR_PLANE = 400;

	private static final float Y_OFFSET = 5;

	private final Matrix4f projectionMatrix;
	private final Matrix4f viewMatrix = new Matrix4f();

	private Vector3f position = new Vector3f(0, 0, 0);

	private float yaw = 0;
	private SmoothFloat pitch = new SmoothFloat(10, 10);
	private SmoothFloat angleAroundPlayer = new SmoothFloat(0, 10);
	private SmoothFloat distanceFromPlayer = new SmoothFloat(10, 5);

	public Camera() {
		this.projectionMatrix = createProjectionMatrix();
	}

	@Override
	public void move() {
		calculatePitch();
		calculateAngleAroundPlayer();
		float horizontalDistance = calculateHorizontalDistance();
		float verticalDistance = calculateVerticalDistance();
		calculateCameraPosition(horizontalDistance, verticalDistance);
		this.yaw = 360 - angleAroundPlayer.get();
		yaw %= 360;
		updateViewMatrix();
	}

	@Override
	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	@Override
	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	@Override
	public Matrix4f getProjectionViewMatrix() {
		return new Matrix4f(projectionMatrix).mul(viewMatrix);
	}

	private void updateViewMatrix() {
		viewMatrix.identity();
		viewMatrix.rotate((float) Math.toRadians(pitch.get()), new Vector3f(1, 0, 0));
		viewMatrix.rotate((float) Math.toRadians(yaw), new Vector3f(0, 1, 0));
		Vector3f negativeCameraPos = new Vector3f(-position.x, -position.y, -position.z);
		viewMatrix.translate(negativeCameraPos);
		// Matrix4f.translate(negativeCameraPos, viewMatrix, viewMatrix);
	}

	public void updateTransformMatrix(Matrix4f newViewMatrix){
		viewMatrix.set(new Matrix4f());
		projectionMatrix.set(newViewMatrix);
	}

	private static Matrix4f createProjectionMatrix() {
		float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();
		float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV / 2f))));
		float x_scale = y_scale / aspectRatio;
		float frustum_length = FAR_PLANE - NEAR_PLANE;
		return new Matrix4f(
				x_scale, 0f, 0f, 0f,
				0f, y_scale, 0f, 0f,
				0f, 0f, -((FAR_PLANE + NEAR_PLANE) / frustum_length), -1f,
				0f, 0f, -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length), 0f
		);
	}

	private void calculateCameraPosition(float horizDistance, float verticDistance) {
		float theta = angleAroundPlayer.get();
		position.x = (float) (horizDistance * Math.sin(Math.toRadians(theta)));
		position.y = verticDistance + Y_OFFSET;
		position.z = (float) (horizDistance * Math.cos(Math.toRadians(theta)));
	}

	/**
	 * @return The horizontal distance of the camera from the origin.
	 */
	private float calculateHorizontalDistance() {
		return (float) (distanceFromPlayer.get() * Math.cos(Math.toRadians(pitch.get())));
	}

	/**
	 * @return The height of the camera from the aim point.
	 */
	private float calculateVerticalDistance() {
		return (float) (distanceFromPlayer.get() * Math.sin(Math.toRadians(pitch.get())));
	}

	/**
	 * Calculate the pitch and change the pitch if the user is moving the mouse
	 * up or down with the LMB pressed.
	 */
	private void calculatePitch() {
		if (Mouse.isButtonDown(0)) {
			float pitchChange = Mouse.getDY() * PITCH_SENSITIVITY;
			pitch.increaseTarget(-pitchChange);
			clampPitch();
		}
		pitch.update(DisplayManager.getFrameTime());
	}

	/**
	 * Calculate the angle of the camera around the player (when looking down at
	 * the camera from above). Basically the yaw. Changes the yaw when the user
	 * moves the mouse horizontally with the LMB down.
	 */
	private void calculateAngleAroundPlayer() {
		if (Mouse.isButtonDown(0)) {
			float angleChange = Mouse.getDX() * YAW_SENSITIVITY;
			angleAroundPlayer.increaseTarget(-angleChange);
		}
		angleAroundPlayer.update(DisplayManager.getFrameTime());
	}

	/**
	 * Ensures the camera's pitch isn't too high or too low.
	 */
	private void clampPitch() {
		if (pitch.getTarget() < 0) {
			pitch.setTarget(0);
		} else if (pitch.getTarget() > MAX_PITCH) {
			pitch.setTarget(MAX_PITCH);
		}
	}

}
