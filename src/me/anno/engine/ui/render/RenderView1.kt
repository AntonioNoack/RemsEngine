package me.anno.engine.ui.render

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Style

class RenderView1(playMode: PlayMode, val staticWorld: PrefabSaveable, style: Style) : RenderView(playMode, style) {
    override fun getWorld() = staticWorld
}