package me.anno.graph.render.effects

import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.graph.render.scene.RenderSceneNode0

class OutlineEffectSelectNode : RenderSceneNode0("OutlineEffectSelect", emptyList(), emptyList()) {
    override fun executeAction() {
        val rv = renderView
        rv.getWorld()?.forAll {
            if (it is MeshComponentBase) it.groupId = 0
        }
        for (thing in EditorState.selection) {
            (thing as? PrefabSaveable)?.forAll {
                if (it is MeshComponentBase) it.groupId = 1
            }
        }
    }
}