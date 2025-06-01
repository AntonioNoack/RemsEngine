package me.anno.gpu

/**
 * Dithering based on alpha value.
 * Ideally the dither pattern has a quickly changing offset, so high fps can blur the pixels visually, without extra post-processing step.
 *
 * For glass or plastic surfaces, DRAW_EVERYTHING and the special glass-blending / alpha blending is preferred.
 * */
@Suppress("unused")
enum class DitherMode(val id: Int, val glslSnipped: String) {
    /**
     * Alpha below threshold 1/255 will be discarded
     * */
    ALPHA_THRESHOLD_INV255(0, "if(finalAlpha<${1f / 255f}) { discard; }\n"),

    /**
     * Alpha below threshold 0.5 will be discarded
     * */
    ALPHA_THRESHOLD_HALF(1, "if(finalAlpha<0.5) { discard; }\n"),

    /**
     * Alpha below threshold 1.0 will be discarded
     * */
    ALPHA_THRESHOLD_ONE(2, "if(finalAlpha<1.0) { discard; }\n"),

    /**
     * Everything will be kept, e.g., handled by alpha blending
     * */
    DRAW_EVERYTHING(3, "// draw everything\n"),

    /**
     * Use dither2x2() do discard/keep pixels.
     * This is currently used for directional lights and for click-picking.
     *
     * I tested whether we could use + frameId to visually blur the result,
     * but that only works well with screen-space pixels. Shadow pixels are too large, and the flickering is visible when moving.
     * */
    DITHER2X2(4, "if(dither2x2(finalAlpha,gl_FragCoord.xy,gl_SampleID)) { discard; }\n")
}