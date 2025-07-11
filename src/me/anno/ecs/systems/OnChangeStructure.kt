package me.anno.ecs.systems

import me.anno.ecs.Entity

/**
 * extension for Components to be notified about when their entity changes
 * */
interface OnChangeStructure {
    fun onChangeStructure(entity: Entity)
}