package me.anno.tests.game.simslike

import me.anno.ecs.Entity
import me.anno.engine.ui.render.RenderView

class SimsControls(val scene: Entity, val household: Household, rv: RenderView) {
    val playControls = SimsPlayControls(this, rv)
    val buildControls = SimsBuildControls(this, rv)

}