package me.anno.gpu.pipeline

import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray

class InstancedStackV2 {
    val size get() = posSizeRot.size ushr 3
    val posSizeRot = ExpandingFloatArray(256)
    val clickIds = ExpandingIntArray(16)
    fun clear() {
        posSizeRot.clear()
        clickIds.clear()
    }
}