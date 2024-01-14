package me.anno.gpu

enum class DitherMode {
    /**
     * alpha below threshold 1/255 will be discarded
     * */
    ALPHA_THRESHOLD_INV255,
    /**
     * alpha below threshold 0.5 will be discarded
     * */
    ALPHA_THRESHOLD_HALF,
    /**
     * alpha below threshold 1.0 will be discarded
     * */
    ALPHA_THRESHOLD_ONE,
    /**
     * everything will be kept, e.g., handled by blending
     * */
    DRAW_EVERYTHING,
    /**
     * use dither2x2() do discard/keep pixels
     * */
    DITHER2X2
}