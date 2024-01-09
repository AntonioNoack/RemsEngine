package me.anno.mesh.blender.impl

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.map
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.utils.search.BinarySearch
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_anim_types.h#L580
 * */
@Suppress("SpellCheckingInspection")
class FCurve(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<FCurve>(file, type, buffer, position) {

    // *next: FCurve, *prev: FCurve, *grp: bActionGroup, *driver: ChannelDriver, modifiers: ListBase,
    // *bezt: BezTriple, // user editable data
    // *fpt: FPoint, totvert: int, // baked/imported data; is this always available? -> no, not even in an imported file
    // active_keyframe_index: int, curval: float, flag: short, extend: short,
    // auto_smoothing: char, array_index: int, *rna_path: char, color_mode: int,
    // color[3]: float, prev_norm_factor: float, prev_offset: float

    val userKeyframes = getInstantList<BezTriple>("*bezt") ?: BInstantList.emptyList()
    val group get() = getPointer("*grp") as? BActionGroup
    val path = charPointer("*rna_path") ?: ""
    val arrayIndex = int("array_index")
    // val keyFrames = getStructArray("*fpt")

    val lastKeyframeIndex get() = userKeyframes.last().controlKfIndex

    fun getValueAt(time: Float): Float {
        val kf = userKeyframes
        if (kf.size == 0) return 0f
        if (kf.size == 1) return kf.first().controlKfValue
        // find correct index for interpolation
        var idx = BinarySearch.binarySearch(kf.size) {
            kf[it].controlKfIndex.compareTo(time)
        }
        if (idx < 0) idx = -idx - 1
        idx = clamp(idx, 0, kf.size - 2)
        val kf0 = userKeyframes[idx]
        val kf0x = kf0.controlKfIndex
        val kf0y = kf0.controlKfValue
        val kf1 = userKeyframes[idx + 1]
        val kf1x = kf1.controlKfIndex
        val kf1y = kf1.controlKfValue
        return map(kf0x, kf1x, kf0y, kf1y, time)
    }

    override fun toString(): String {
        return "FCurve { '${group?.name}', '$path'[$arrayIndex], ${userKeyframes.map { "${it.controlKfIndex}: ${it.controlKfValue}" }} }"
    }
}