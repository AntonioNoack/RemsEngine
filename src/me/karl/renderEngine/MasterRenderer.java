package me.karl.renderEngine;

import me.karl.renderer.AnimatedModelRenderer;
import me.karl.scene.DAEScene;
import me.karl.skybox.SkyboxRenderer;
import org.lwjgl.opengl.GL11;

/**
 * This class is in charge of rendering everything in the scene to the screen.
 *
 * @author Karl
 */
public class MasterRenderer {

    private final SkyboxRenderer skyRenderer;
    private final AnimatedModelRenderer entityRenderer;

    protected MasterRenderer(AnimatedModelRenderer renderer, SkyboxRenderer skyRenderer) {
        this.skyRenderer = skyRenderer;
        this.entityRenderer = renderer;
    }

    /**
     * Renders the scene to the screen.
     */
    protected void renderScene(DAEScene scene) {
        prepare();
        skyRenderer.render(scene.getCamera());
        entityRenderer.render(scene.getAnimatedModel(), scene.getCamera(), scene.getLightDirection());
    }

    /**
     * Clean up when the game is closed.
     */
    protected void cleanUp() {
        skyRenderer.cleanUp();
        entityRenderer.cleanUp();
    }

    /**
     * Prepare to render the current frame by clearing the framebuffer.
     */
    private void prepare() {
        GL11.glClearColor(1, 1, 1, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }


}
