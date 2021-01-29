package me.anno.objects.effects.types

import me.anno.utils.io.ResourceHelper

object GLSLLib {

    operator fun get(path: String) = String(ResourceHelper.loadResource(path).readBytes())
        .trim().run {
            replace("\r", "")
        }.run {
            substring(indexOf('\n')+1, lastIndexOf('\n'))
        }

    fun case(case: Int, path: String) = String(ResourceHelper.loadResource(path).readBytes())
        .trim().run {
            replace("\r", "")
        }.run {
            "case $case:\n" + substring(indexOf('\n')+1, lastIndexOf('\n')) + "\nbreak;\n"
        }

}