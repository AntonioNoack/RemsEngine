package me.karl.main;

import me.karl.animatedModel.AnimatedModel;
import me.karl.animation.Animation;
import me.karl.loaders.AnimatedModelLoader;
import me.karl.loaders.AnimationLoader;
import me.karl.scene.ICamera;
import me.karl.scene.Scene;
import me.karl.utils.URI;

public class SceneLoader {

    /**
     * Sets up the scene. Loads the entity, load the animation, tells the entity
     * to do the animation, sets the light direction, creates the camera, etc...
     *
     * @param resFolder - the folder containing all the information about the animated entity
     *                  (mesh, animation, and texture info).
     * @return The entire scene.
     */
    public static Scene loadScene(URI resFolder) {
        return loadScene(
                new URI(resFolder, GeneralSettings.MODEL_FILE),
                new URI(resFolder, GeneralSettings.ANIM_FILE)
        );
    }

    public static Scene loadScene(URI model, URI anim) {
        ICamera camera = new Camera();
        AnimatedModel entity = AnimatedModelLoader.loadEntity(model);
        Animation animation = AnimationLoader.loadAnimation(anim);
        entity.doAnimation(animation);
        Scene scene = new Scene(entity, camera);
        scene.setLightDirection(GeneralSettings.LIGHT_DIR);
        return scene;
    }

}
