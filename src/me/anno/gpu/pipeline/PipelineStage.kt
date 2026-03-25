package me.anno.gpu.pipeline

enum class PipelineStage(val id: Int) {
    /**
     * for cutout or non-transparent (fully opaque) geometry
     * */
    OPAQUE(0),

    /**
     * order-independent transparency, for glass PBR materials
     * */
    GLASS(1),

    /**
     * for drawing on top of existing surfaces,
     * can change color, normals, emissive, etc
     * */
    DECAL(2),

    /**
     * for normal alpha-transparency, order-dependent
     * */
    TRANSPARENT(3),

    // feel free to suggest other names to me,
    // and I'll consider to make them official
    STAGE4(4),
    STAGE5(5),
    STAGE6(6),
    STAGE7(7),
    STAGE8(8),
    STAGE9(9),
}