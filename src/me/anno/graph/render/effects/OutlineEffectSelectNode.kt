package me.anno.graph.render.effects

import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderView
import me.anno.graph.render.scene.RenderSceneNode0
import me.anno.studio.Inspectable
import java.util.*

class OutlineEffectSelectNode : RenderSceneNode0(
    "OutlineEffectSelect", listOf(
        "Boolean", "Force Update"
    ), emptyList()
) {

    companion object {
        private val lastSelection = WeakHashMap<RenderView, List<Inspectable>>()
    }

    override fun executeAction() {
        val forceUpdate = getInput(1) == true
        val renderView = renderView
        val last = lastSelection[renderView]
        val selection = EditorState.selection
        if (!forceUpdate && last == selection) return // done :)
        lastSelection[renderView] = selection
        renderView.getWorld()?.forAll {
            if (it is MeshComponentBase) it.groupId = 0
        }
        for (thing in selection) {
            (thing as? PrefabSaveable)?.forAll {
                if (it is MeshComponentBase) it.groupId = 1
            }
        }
    }
}