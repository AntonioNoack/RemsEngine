package me.anno.games.trainbuilder.rail

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.ui.UIColors
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d

class RailSplitController(
    val input: PlacedRailPiece,
    val output0: PlacedRailPiece,
    val output1: PlacedRailPiece,
) : Component() {

    init {
        assertTrue(input.nextPiece == output0 || input.nextPiece == output1)
        assertTrue(output0.reversed.nextPiece == input.reversed)
        assertTrue(output1.reversed.nextPiece == input.reversed)
    }

    @DebugAction
    fun toggle() {
        link(input.nextPiece == output0)
        showSplitArrow()
    }

    private fun showSplitArrow() {
        val p0 = Vector3d(input.end)
        val p1 = Vector3d((if (input.nextPiece == output0) output0 else output1).end)
        p0.y += 10.0
        p1.y += 10.0
        DebugShapes.debugArrows.add(DebugLine(p0, p1, UIColors.dodgerBlue, 0.5f))
    }

    fun link(useOutput1: Boolean) {
        link(input, if (useOutput1) output1 else output0)
    }

    private fun link(from: PlacedRailPiece, to: PlacedRailPiece) {
        from.nextPiece = to
    }
}