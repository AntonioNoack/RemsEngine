package me.karl.renderer;

import me.anno.image.ImageGPUCache;
import me.anno.gpu.GFX;
import me.anno.gpu.TextureLib;
import me.anno.gpu.texture.Clamping;
import me.anno.gpu.texture.GPUFiltering;
import me.anno.gpu.texture.Texture2D;
import me.anno.io.files.FileReference;
import me.anno.video.MissingFrameException;
import me.karl.animatedModel.AnimatedModel;
import me.karl.scene.ICamera;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * This class deals with rendering an animated entity. Nothing particularly new
 * here. The only exciting part is that the joint transforms get loaded up to
 * the shader in a uniform array.
 *
 * @author Karl
 */
public class AnimatedModelRenderer {

    private AnimatedModelShader shader;

    /**
     * Initializes the shader program used for rendering animated models.
     */
    public AnimatedModelRenderer() {
        this.shader = new AnimatedModelShader();
    }

    /**
     * Renders an animated entity. The main thing to note here is that all the
     * joint transforms are loaded up to the shader to a uniform array. Also 5
     * attributes of the VAO are enabled before rendering, to include joint
     * indices and weights.
     *
     * @param entity   - the animated entity to be rendered.
     * @param camera   - the camera used to render the entity.
     * @param lightDir - the direction of the light in the scene.
     */
    public void render(AnimatedModel entity, ICamera camera, Vector3f lightDir) {
        prepare(camera, lightDir);
        List<FileReference> tex = entity.getTextures();
        Texture2D texture = null;
        FileReference file0;
        if (!tex.isEmpty() && (file0 = tex.get(0)) != null) {
            texture = ImageGPUCache.INSTANCE.getImage(file0, 1000L, true);
            if (texture == null && GFX.INSTANCE.isFinalRendering() && file0.hasValidName()) {
                throw new MissingFrameException(file0);
            }
        }
        if (texture == null) {
            texture = TextureLib.INSTANCE.getWhiteTexture();
        }
        texture.bind(0, GPUFiltering.LINEAR, Clamping.REPEAT);
        entity.getModel().bind(0, 1, 2, 3, 4);
        shader.jointTransforms.loadMatrixArray(entity.getJointTransforms());
        GL11.glDrawElements(GL11.GL_TRIANGLES, entity.getModel().getIndexCount(), GL11.GL_UNSIGNED_INT, 0);
        entity.getModel().unbind(0, 1, 2, 3, 4);
        finish();
    }

    /**
     * Deletes the shader program when the game closes.
     */
    public void cleanUp() {
        shader.cleanUp();
    }

    /**
     * Starts the shader program and loads up the projection view matrix, as
     * well as the light direction. Enables and disables a few settings which
     * should be pretty self-explanatory.
     *
     * @param camera   - the camera being used.
     * @param lightDir - the direction of the light in the scene.
     */
    private void prepare(ICamera camera, Vector3f lightDir) {
        shader.start();
        shader.projectionViewMatrix.loadMatrix(camera.getProjectionViewMatrix());
        shader.lightDirection.loadVec3(lightDir);
        // OpenGlUtils.antialias(true);
        // OpenGlUtils.disableBlending();
        // OpenGlUtils.enableDepthTesting(true);
    }

    /**
     * Stops the shader program after rendering the entity.
     */
    private void finish() {
        shader.stop();
    }

}
