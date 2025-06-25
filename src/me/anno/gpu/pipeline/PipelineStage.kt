package me.anno.gpu.pipeline

enum class PipelineStage(val id: Int) {
    OPAQUE(0),
    GLASS(1),
    DECAL(2),

    // todo add a simple transparency stage
    //  non reflective, just boring transparency
    // todo implement a special pass for them, incl. order-independent transparency
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