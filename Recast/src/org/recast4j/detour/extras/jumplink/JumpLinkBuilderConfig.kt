package org.recast4j.detour.extras.jumplink

class JumpLinkBuilderConfig(
    val cellSize: Float,
    val cellHeight: Float,
    val agentRadius: Float,
    val agentHeight: Float,
    val agentClimb: Float,
    val groundTolerance: Float,
    val startDistance: Float,
    val endDistance: Float,
    val minHeight: Float,
    maxHeight: Float,
    jumpHeight: Float
) {
    val jumpHeight: Float
    val heightRange: Float

    init {
        heightRange = maxHeight - minHeight
        this.jumpHeight = jumpHeight
    }
}