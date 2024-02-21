package me.anno.ecs

import me.anno.utils.types.Floats.f2s

// todo (how) can we call them from Lua?
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

    fun Entity.toStringWithTransforms(depth: Int): StringBuilder {
        val text = StringBuilder()
        for (i in 0 until depth) text.append('\t')
        val p = transform.localPosition
        val r = transform.localRotation
        val s = transform.localScale
        text.append(
            "Entity((${p.x.f2s()},${p.y.f2s()},${p.z.f2s()})," +
                    "(${r.x.f2s()},${r.y.f2s()},${r.z.f2s()},${r.w.f2s()})," +
                    "(${s.x.f2s()},${s.y.f2s()},${s.z.f2s()}),'$name',$sizeOfHierarchy):\n"
        )
        val nextDepth = depth + 1
        for (child in children)
            text.append(child.toStringWithTransforms(nextDepth))
        for (component in components)
            text.append(component.toString(nextDepth))
        return text
    }
}