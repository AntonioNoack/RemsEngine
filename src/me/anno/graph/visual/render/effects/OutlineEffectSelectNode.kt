package me.anno.graph.visual.render.effects

import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.inspector.Inspectable
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderView
import me.anno.graph.visual.render.scene.RenderViewNode
import java.util.WeakHashMap

class OutlineEffectSelectNode : RenderViewNode(
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
        var numTotal = 0
        renderView.getWorld()?.forAll {
            if (it is MeshComponentBase) {
                numTotal++
                it.groupId = 0
            }
        }
        var numSelected = 0
        for (thing in selection) {
            (thing as? PrefabSaveable)?.forAll {
                if (it is MeshComponentBase) {
                    numSelected++
                    it.groupId = 1
                }
            }
        }
    }
}