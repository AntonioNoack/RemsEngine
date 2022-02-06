package me.anno.gpu.shader

import me.anno.utils.io.ResourceHelper
import me.anno.utils.io.Streams.readText

object GLSLLib {

    fun case(case: Int, path: String) = ResourceHelper.loadResource(path).readText()
        .trim().run {
            replace("\r", "")
        }.run {
            "case $case:\n" + substring(indexOf('\n')+1, lastIndexOf('\n')) + "\nbreak;\n"
        }

}