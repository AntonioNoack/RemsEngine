package me.anno.sdf

import me.anno.maths.Maths
import me.anno.sdf.SDFCombiningFunctions.hgFunctions
import me.anno.sdf.SDFCombiningFunctions.sMaxCubic
import me.anno.sdf.SDFCombiningFunctions.sMinCubic
import me.anno.sdf.SDFCombiningFunctions.sdDiff
import me.anno.sdf.SDFCombiningFunctions.sdDiff1
import me.anno.sdf.SDFCombiningFunctions.sdInt
import me.anno.sdf.SDFCombiningFunctions.sdMax
import me.anno.sdf.SDFCombiningFunctions.sdMin
import me.anno.sdf.SDFCombiningFunctions.smoothMinCubic
import kotlin.math.abs

enum class CombinationMode(
    val id: Int,
    val funcName: String,
    val glslCode: List<String>,
    val isStyleable: Boolean
) {
    /**
     * A or B -> both
     * */
    UNION(0, "sdMin", listOf(smoothMinCubic, sdMin), true) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return sMinCubic(d0, d1, k)
        }
    },

    /**
     * A and B -> only where both are
     * */
    INTERSECTION(
        1, "sdMax", listOf(
            smoothMinCubic,
            sdMax
        ), true
    ) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return sMaxCubic(d0, d1, k)
        }
    },

    /**
     * A \ B, a, but without b
     * */
    DIFFERENCE1(
        2, "sdMax", listOf(
            smoothMinCubic,
            sdMax
        ), true
    ) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return sMaxCubic(+d0, -d1, k)
        }
    },

    /**
     * B \ A, b, but without a
     * */
    DIFFERENCE2(
        3, "sdMax", listOf(
            smoothMinCubic,
            sdMax
        ), true
    ) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return sMaxCubic(-d0, +d1, k)
        }
    },

    /**
     * A xor B, a or b, but not where both are
     * */
    DIFFERENCE_SYM(
        4, "sdDiff3", listOf(
            smoothMinCubic,
            sdMin,
            sdMax,
            sdDiff1,
            sdDiff
        ), false
    ) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return sMaxCubic(
                +sMinCubic(d0, d1, k),
                -sMaxCubic(d0, d1, k),
                k
            )
        }
    },
    INTERPOLATION(5, "sdInt", listOf(sdInt), false) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return 0f // shouldn't be called on this value!!! (but I want to avoid throwing exceptions)
        }
    },
    PIPE(10, "sdPipe", listOf(hgFunctions), false) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return Maths.length(d0, d1) - group.smoothness
        }
    },
    ENGRAVE(11, "sdEngrave", listOf(hgFunctions), false) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            return Maths.max(d0, (d0 + group.smoothness - abs(d1)) * Maths.SQRT1_2f)
        }
    },
    GROOVE(12, "sdGroove", listOf(hgFunctions), false) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            val g = group.groove
            return Maths.max(d0, Maths.min(d0 + g.x, g.y - abs(d1)))
        }
    },
    TONGUE(13, "sdTongue", listOf(hgFunctions), false) {
        override fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float {
            val g = group.groove
            return Maths.min(d0, Maths.max(d0 - g.x, abs(d1) - g.y))
        }
    };

    abstract fun combine(d0: Float, d1: Float, k: Float, group: SDFGroup): Float
}