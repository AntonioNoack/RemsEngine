package me.anno.games.visualnovel

import me.anno.games.visualnovel.VisualNovelState.primary
import me.anno.games.visualnovel.VisualNovelState.secondary
import me.anno.graph.visual.actions.ActionNode
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

class CharacterNode : ActionNode(
    "Characters",
    listOf("FileReference", "Primary", "FileReference", "Secondary"), emptyList()
) {
    override fun executeAction() {
        primary = getInput(1) as? FileReference ?: InvalidRef
        secondary = getInput(2) as? FileReference ?: InvalidRef
    }
}