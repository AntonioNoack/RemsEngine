package me.anno.engine.ui.render

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.EditorState
import me.anno.ui.Style

class RenderView0(playMode: PlayMode, style: Style) : RenderView(playMode, style) {
    override fun getWorld(): PrefabSaveable? {
        return EditorState.prefab.value?.sample as? PrefabSaveable ?: flatCube
    }
}