package me.anno.engine.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ui.base.Panel

// todo controls
// todo show the scene
// todo drag stuff
// todo translate, rotate, scale with gizmos
// todo render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process

// todo blend between multiple cameras, only allow 2? yes :)


// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

class SceneView : Panel(style) {

    // todo a custom state, which stores all related rendering information

    // can exist (game/game mode), but does not need to (editor)
    // todo in the editor it becomes the prefab for a local player -> ui shall always be placed in the local player
    var localPlayer: LocalPlayer? = null

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        // todo go through the rendering pipeline, and render everything

        // todo draw all local players in their respective fields
        // todo use customizable masks for the assignment (plus a button mapping)
        // todo if a color doesn't appear on the mapping, it doesn't need to be drawn
        // todo more important players can have a larger field
        // todo slope of these partial windows can be customized for nicer looks

    }

}