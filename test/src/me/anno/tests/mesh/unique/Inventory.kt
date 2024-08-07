package me.anno.tests.mesh.unique

import me.anno.utils.structures.lists.Lists.createArrayList

class Inventory(slots: Int) {
    val slots: List<ItemSlot> = createArrayList(slots) {
        ItemSlot(0, 0)
    }
}