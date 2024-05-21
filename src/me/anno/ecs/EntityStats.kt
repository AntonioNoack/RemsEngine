package me.anno.ecs

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
        var sum = 1 // self
        val children = entity.children
        for (i in children.indices) {
            sum += calcTotalNumEntities(children[i])
        }
        return sum
    }

    val Entity.totalNumComponents
        get(): Int = calcTotalNumComponents(this)

    private fun calcTotalNumComponents(entity: Entity): Int {
        var sum = entity.components.size
        val children = entity.children
        for (i in children.indices) {
            sum += calcTotalNumComponents(children[i])
        }
        return sum
    }
}