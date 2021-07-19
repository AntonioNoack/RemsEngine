package me.anno.ecs.prefab

import me.anno.io.text.TextReader
import me.anno.studio.history.History2

class ChangeHistory : History2<String>() {

    override fun getTitle(v: String): String {
        return "X${v.length}"
    }

    override fun apply(v: String) {
        val changes = TextReader.read(v).filterIsInstance<Change>()
        TODO("apply these changes")
    }

    override fun filter(v: Any?): String? = v as? String

    override val className: String = "ChangeHistory"

}