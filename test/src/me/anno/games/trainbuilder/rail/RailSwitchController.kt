package me.anno.games.trainbuilder.rail

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction

/**
 * A switch, which is just a double split
 * */
class RailSwitchController(
    input0: PlacedRailPiece,
    output0: PlacedRailPiece,
    input1: PlacedRailPiece,
    output1: PlacedRailPiece,
    switchLane: PlacedRailPiece
) : Component() {

    val split0 = RailSplitController(input0, output0, switchLane)
    val split1 = RailSplitController(output1.reversed, input1.reversed, switchLane.reversed)

    @DebugAction
    fun toggle0() {
        split0.toggle()
    }

    @DebugAction
    fun toggle1() {
        split1.toggle()
    }

    @DebugAction
    fun showSplitArrow0() {
        split0.showSplitArrow()
    }

    @DebugAction
    fun showSplitArrow1() {
        split1.showSplitArrow()
    }
}