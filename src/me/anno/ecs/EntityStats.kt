package me.anno.ecs

import me.anno.utils.structures.Recursion

// (how) can we call them from Lua? -> the Java way, because Lua is using Java reflection iirc
object EntityStats {

    val Entity.sizeOfHierarchy
        get(): Int = calcSizeOfHierarchy(this)

    private fun calcSizeOfHierarchy(entity: Entity): Int {
        val children = entity.children
        var sum = 1 + entity.components.size // self plus components
        for (i in children.indices) {
            sum += calcSizeOfHierarchy(children[i])
        }
        return sum
    }

    val Entity.totalNumEntities
        get(): Int = calcTotalNumEntities(this)

    private fun calcTotalNumEntities(entity: Entity): Int {
        var count = 0
        Recursion.processRecursive(entity) { entityI, remaining ->
            remaining.addAll(entityI.children)
            count++
        }
        return count
    }

    val Entity.totalNumComponents
        get(): Int = calcTotalNumComponents(this)

    private fun calcTotalNumComponents(entity: Entity): Int {
        var count = 0
        Recursion.processRecursive(entity) { entityI, remaining ->
            remaining.addAll(entityI.children)
            count += entityI.components.size
        }
        return count
    }
}