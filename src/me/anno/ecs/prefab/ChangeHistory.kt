package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.Change
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.studio.history.StringHistory

class ChangeHistory : StringHistory() {

    override fun apply(v: String) {
        // todo change/change0
        val changes = TextReader.read(v, true).filterIsInstance<Change>()
        TODO("apply these changes")
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

            val str = TextWriter.toText(hist)
            println(str)

            val hist2 = TextReader.readFirstOrNull<ChangeHistory>(str)!!

            val str2 = TextWriter.toText(hist2)

            if (str != str2) {
                println(str2)
                throw RuntimeException()
            }

        }
    }

}