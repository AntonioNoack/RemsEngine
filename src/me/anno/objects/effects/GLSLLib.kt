package me.anno.objects.effects

import me.anno.utils.io.ResourceHelper
import me.anno.utils.io.Streams.readText

object GLSLLib {

    operator fun get(path: String) = ResourceHelper.loadResource(path).readText()
        .trim().run {
            replace("\r", "")
        }.run {
            substring(indexOf('\n')+1, lastIndexOf('\n'))
        }

    fun case(case: Int, path: String) = ResourceHelper.loadResource(path).readText()
        .trim().run {
            replace("\r", "")
        }.run {
            "case $case:\n" + substring(indexOf('\n')+1, lastIndexOf('\n')) + "\nbreak;\n"
        }

}