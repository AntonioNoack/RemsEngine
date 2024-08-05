package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.ecs.prefab.change.PrefabChanges
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
import me.anno.engine.history.StringHistory
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.io.json.saveable.JsonStringReader
import me.anno.ui.editor.PropertyInspector
import org.apache.logging.log4j.LogManager

/**
 * keeps track of when changes occurred, so we can revert and redo them
 * */
class ChangeHistory : StringHistory() {

    @NotSerializedProperty
    var prefab: Prefab? = null

    override fun apply(prev: String, curr: String) {
        if (prev == curr) return
        if ("CAdd" !in objectTypeRegistry) {
            ECSRegistry.init()
        }

        val workspace = EngineBase.workspace
        val changes = JsonStringReader.readFirstOrNull(curr, workspace, PrefabChanges::class) ?: PrefabChanges()
        val prefab = prefab ?: return
        val prevAdds = prefab.adds
        val currAdds = changes.adds

        val prevSets = prefab.sets
        val currSets = changes.sets
        val major = prevAdds != currAdds || // warning: we should sort them
                prevSets.size != currSets.size // warning: one could have been added and one removed
        if (major) prefab.invalidateInstance()

        if (!major && prefab._sampleInstance != null) {
            // if instance exists, and no major changes, we only need to apply what really has changed
            currSets.forEach { path, name, currValue ->
                prefab[path, name] = currValue
            }
            // update transforms
            val si = prefab._sampleInstance
            if (si is Entity) {
                si.forAll {
                    if (it is Entity) {
                        it.transform.teleportUpdate()
                    }
                }
            }
        }
        prevSets.clear()
        currSets.forEach { path, name, currValue ->
            prevSets[path, name] = currValue
        }
        if (major) {
            LOGGER.info("Hierarchy changed")
            prefab.adds.clear()
            prefab.adds.putAll(currAdds)
        }
        PropertyInspector.invalidateUI(major)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ChangeHistory::class)
    }
}