package me.anno.games.flatworld.buildings

import me.anno.ecs.components.mesh.Mesh
import me.anno.tests.game.flatworld.humans.Human
import kotlin.math.max

class FactoryBuilding(
    mesh: Mesh,
    val slots: List<ResourceSlot>,
    val typeToSlot: Map<me.anno.games.flatworld.goods.Resource, Int>
) : Building(mesh) {

    val workers = ArrayList<Human>()
    var maxWorkers = 10

    val actualStock = FloatArray(slots.size)
    val plannedStock = FloatArray(slots.size)

    fun getMaxInput(type: me.anno.games.flatworld.goods.Resource): Float {
        val slot = typeToSlot[type] ?: return 0f
        return getMaxInput(slot)
    }

    fun getMaxInput(slotIndex: Int): Float {
        val slot = slots[slotIndex]
        val worstCaseStock = max(actualStock[slotIndex], plannedStock[slotIndex])
        return slot.capacity - worstCaseStock
    }

    // todo calculate whether a truck should run
    // todo calculate whether a thing can be produced

}