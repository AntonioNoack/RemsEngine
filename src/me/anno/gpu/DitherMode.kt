package me.anno.gpu

@Suppress("unused")
enum class DitherMode(val id: Int, val glslSnipped: String) {
    /**
     * alpha below threshold 1/255 will be discarded
     * */
    ALPHA_THRESHOLD_INV255(0, "if(finalAlpha<${1f / 255f}) { discard; }\n"),

    /**
     * alpha below threshold 0.5 will be discarded
     * */
    ALPHA_THRESHOLD_HALF(1, "if(finalAlpha<0.5) { discard; }\n"),

    /**
     * alpha below threshold 1.0 will be discarded
     * */
    ALPHA_THRESHOLD_ONE(2, "if(finalAlpha<1.0) { discard; }\n"),

    /**
     * everything will be kept, e.g., handled by blending
     * */
    DRAW_EVERYTHING(3, "// draw everything\n"),

    /**
     * use dither2x2() do discard/keep pixels
     * */
    DITHER2X2(4, "if(dither2x2(finalAlpha)) { discard; }\n")
}