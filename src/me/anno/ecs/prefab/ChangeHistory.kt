package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Change
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase
import me.anno.studio.history.StringHistory
import me.anno.ui.editor.PropertyInspector

class ChangeHistory : StringHistory() {

    override fun apply(v: String) {
        // change/change0
        // maybe incorrect...
        val workspace = StudioBase.workspace
        val changes = TextReader.read(v, workspace, true).filterIsInstance<Change>()
        println(changes)
        // todo this may be the incorrect prefab...
        val prefab = PrefabInspector.currentInspector!!.prefab
        prefab.adds = changes.filterIsInstance<CAdd>()
        prefab.sets.clear()
        for (change in changes) {
            if (change is CSet) {
                prefab.sets[change.path, change.name!!] = change.value
            }
        }
        println("invalidated instance")
        prefab.invalidateInstance()
        PropertyInspector.invalidateUI()
    }

    override val className: String = "ChangeHistory"

    companion object {

        /**
         * a test for StringHistories compression capabilities
         * */
        @JvmStatic
        fun main(args: Array<String>) {

            registerCustomClass(ChangeHistory())

            val hist = ChangeHistory()
            hist.put("hallo")
            hist.put("hello")
            hist.put("hello world")
            hist.put("hell")
            hist.put("hello world, you")
            hist.put("kiss the world")
            hist.put("this is the world")
            hist.put("that was le world")

            val str = TextWriter.toText(hist, InvalidRef)
            LOGGER.info(str)

            val hist2 = TextReader.readFirstOrNull<ChangeHistory>(str, InvalidRef)!!

            val str2 = TextWriter.toText(hist2, InvalidRef)

            if (str != str2) {
                LOGGER.info(str2)
                throw RuntimeException()
            }

        }
    }

}