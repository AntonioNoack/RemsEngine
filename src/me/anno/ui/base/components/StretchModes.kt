package me.anno.ui.base.components

import me.anno.image.ImageScale
import me.anno.utils.structures.tuples.IntPair

enum class StretchModes {
    OVERFLOW {
        override fun stretch(srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntPair {
            return ImageScale.scaleMin(srcW, srcH, dstW, dstH)
        }
    },
    PADDING {
        override fun stretch(srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntPair {
            return ImageScale.scaleMax(srcW, srcH, dstW, dstH)
        }
    },
    STRETCH {
        override fun stretch(srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntPair {
            return IntPair(dstW, dstH)
        }
    };

    abstract fun stretch(srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntPair
}
