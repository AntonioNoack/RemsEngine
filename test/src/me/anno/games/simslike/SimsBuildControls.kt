package me.anno.games.simslike

import me.anno.engine.ui.render.RenderView
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment

class SimsBuildControls(controls: SimsControls, rv: RenderView) : SimsControlBase(controls, rv) {

    init {
        add(TextButton(NameDesc("Play"), style)
            .addLeftClickListener {
                // switch to play mode
                sceneView.editControls = controls.playControls
                controls.playControls.rotationTargetDegrees.set(rotationTargetDegrees)
            }
            .apply {
                alignmentX = AxisAlignment.MAX
                alignmentY = AxisAlignment.MAX
            })
        // todo inventory-like build selection menu
        //  -> take a look at WorldBuilder
    }

    // different modes:
    // todo implement these modes:
    //  - place tiles
    //  - place walls
    //  - edit terrain
    //  - place furniture
    //  - place items
    enum class BuildMode {
        TILES_PLACING,
        WALLS_PLACING,
        TERRAIN_EDIT,
        DECORATING,
        DELETING
    }
}