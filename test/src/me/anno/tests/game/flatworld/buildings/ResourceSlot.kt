package me.anno.tests.game.flatworld.buildings

import me.anno.tests.game.flatworld.goods.Resource

class ResourceSlot(val type: Resource, val relativeQuantity: Int, val capacity: Float) {
    val isInput get() = relativeQuantity > 0
}