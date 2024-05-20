package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection")
class BRenderData(ptr: ConstructorData) : BlendData(ptr) {

    val frsSec = i16("frs_sec") // 30.0 -> fps :)
    val frsSecBase = f32("frs_sec_base") // 1.0, or 1.001, so for 29.97 XD
    val frameLen = f32("framelen") // 1.0?

    override fun toString(): String {
        return "RenderData { $frsSec, $frsSecBase, $frameLen }"
    }
}