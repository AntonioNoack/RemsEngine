package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.engine.ECSWorld
import me.anno.ui.editor.treeView.AbstractTreeView
import me.anno.ui.style.Style

// todo runtime and pre-runtime view
// todo unity oriented
// todo easily add stuff
// todo add prefabs
// todo generally: prefabs

// todo easy scripting
// todo support many languages at runtime via scripting
// todo compile all scripting languages for export? <3

// todo add / remove components
// todo reorder them by dragging
// todo just generalize the TreeViewPanel? sounds like a good idea :)


// todo switch between programming language styles easily, throughout the code?... idk whether that's possible...
// maybe on a per-function-basis

class ECSTreeView(val root: ECSWorld, isGaming: Boolean, style: Style) : AbstractTreeView<Entity>(
    if (isGaming) listOf(
        root.globallyShared,
        root.playerPrefab,
        root.locallyShared,
        root.localPlayers,
        root.remotePlayers
    )
    else listOf(root.globallyShared, root.playerPrefab, root.locallyShared),
    {
        // todo open add menu
        // temporary solution:
        it.add(Entity())
    },
    ECSFileImporter,
    false,
    style
) {

    override var selectedElement: Entity? = null

    override fun selectElement(element: Entity?) {
        selectedElement = element
    }

    override fun focusOnElement(element: Entity) {
        selectElement(element)
        // todo focus on the element by inverting the camera transform and such...
    }

    override fun getClassName(): String = "ECSTreeView"

}