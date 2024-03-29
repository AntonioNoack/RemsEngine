package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Change
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.io.Saveable
import me.anno.io.json.saveable.JsonStringReader
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.EngineBase
import me.anno.engine.history.StringHistory
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.structures.lists.Lists.count2
import org.apache.logging.log4j.LogManager

/**
 * keeps track of when changes occurred, so we can revert and redo them
 * */
class ChangeHistory : StringHistory() {

    @NotSerializedProperty
    var prefab: Prefab? = null

    override fun apply(prev: String, curr: String) {
        if (prev == curr) return
        if ("CAdd" !in Saveable.objectTypeRegistry) {
            ECSRegistry.init()
        }

        val workspace = EngineBase.workspace
        val changes = JsonStringReader.read(curr, workspace, true).filterIsInstance<Change>()
        val prefab = prefab!!
        val prevAdds = prefab.adds
        val currAdds = changes.filterIsInstance<CAdd>()

        val prevSets = prefab.sets
        val currSetsSize = changes.count2 { it is CSet }
        val major = prevAdds != currAdds || // warning: we should sort them
                prevSets.size != currSetsSize // warning: one could have been added and one removed
        if (major) prefab.invalidateInstance()

        if (!major && prefab._sampleInstance != null) {
            // if instance exists, and no major changes, we only need to apply what really has changed
            val prevValues = HashMap<Pair<Path, String>, Any?>(currSetsSize)
            val nothing = Any()
            prevSets.forEach { k1, k2, v ->
                prevValues[Pair(k1, k2)] = v ?: nothing
            }
            for (change in changes) {
                if (change is CSet) {
                    val prevValue = prevValues[Pair(change.path, change.name)]
                    val currValue = change.value ?: nothing
                    if (prevValue != currValue) {
                        prefab[change.path, change.name] = change.value
                        // LOGGER.debug("Changed ${change.path} from $prevValue to $currValue")
                    }
                }
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
        } else {
            prevSets.clear()
            for (change in changes) {
                if (change is CSet) {
                    prevSets[change.path, change.name] = change.value
                }
            }
        }
        if (major) {
            LOGGER.info("Hierarchy changed")
            prefab.adds.clear()
            prefab.readAdds(currAdds)
        }
        PropertyInspector.invalidateUI(major)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ChangeHistory::class)
    }
}