package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection")
class BRenderData(ptr: ConstructorData) : BlendData(ptr) {

    val frsSec = short("frs_sec") // 30.0 -> fps :)
    val frsSecBase = float("frs_sec_base") // 1.0, or 1.001, so for 29.97 XD
    val frameLen = float("framelen") // 1.0?

    override fun toString(): String {
        return "RenderData { $frsSec, $frsSecBase, $frameLen }"
    }
}