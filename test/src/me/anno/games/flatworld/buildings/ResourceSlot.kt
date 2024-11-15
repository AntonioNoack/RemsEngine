package me.anno.games.flatworld.buildings

class ResourceSlot(val type: me.anno.games.flatworld.goods.Resource, val relativeQuantity: Int, val capacity: Float) {
    val isInput get() = relativeQuantity > 0
}