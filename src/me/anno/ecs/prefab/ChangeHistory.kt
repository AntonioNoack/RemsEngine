package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Change
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase
import me.anno.studio.history.History
import me.anno.studio.history.StringHistory
import me.anno.ui.editor.PropertyInspector
import org.apache.logging.log4j.LogManager

class ChangeHistory : StringHistory() {

    override fun apply(v: String) {
        // change/change0
        // maybe incorrect...
        val workspace = StudioBase.workspace
        val changes = TextReader.read(v, workspace, true).filterIsInstance<Change>()
        LOGGER.debug(changes)
        // todo this may be the incorrect prefab...
        val prefab = PrefabInspector.currentInspector!!.prefab
        prefab.adds = changes.filterIsInstance<CAdd>()
        prefab.sets.clear()
        for (change in changes) {
            if (change is CSet) {
                prefab.sets[change.path, change.name!!] = change.value
            }
        }
        LOGGER.debug("invalidated instance")
        prefab.invalidateInstance()
        PropertyInspector.invalidateUI(true)
    }

    override val className get() = "ChangeHistory"

    companion object {
        private val LOGGER = LogManager.getLogger(ChangeHistory::class)
    }

}