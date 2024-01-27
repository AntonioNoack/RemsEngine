package me.anno.gpu.pipeline

enum class PipelineStage(val id: Int) {
    OPAQUE(0),
    TRANSPARENT(1),
    DECAL(2),

    // feel free to suggest other names to me,
    // and I'll consider to make them official
    STAGE3(3),
    STAGE4(4),
    STAGE5(5),
    STAGE6(6),
    STAGE7(7),
    STAGE8(8),
    STAGE9(9),
}