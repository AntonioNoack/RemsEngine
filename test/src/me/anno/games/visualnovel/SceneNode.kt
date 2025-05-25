package me.anno.games.visualnovel

import me.anno.games.visualnovel.VisualNovelState.background
import me.anno.graph.visual.actions.ActionNode
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

class SceneNode : ActionNode("Scene", listOf("FileReference", "Background"), emptyList()) {
    override fun executeAction() {
        background = getInput(1) as? FileReference ?: InvalidRef
    }
}