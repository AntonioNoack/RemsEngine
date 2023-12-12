package me.anno.ecs

import me.anno.utils.types.Floats.f2s

// todo (how) can we call them from Lua?
object EntityStats {

    val Entity.sizeOfHierarchy
        get(): Int {
            val children = children
            var sum = 1 + components.size // self plus components
            for (i in children.indices) {
                sum += children[i].sizeOfHierarchy
            }
            return sum
        }

    val Entity.totalNumEntities
        get(): Int {
            val children = children
            var sum = 1 // self
            for (i in children.indices) {
                sum += children[i].totalNumEntities
            }
            return sum
        }

    val Entity.totalNumComponents
        get(): Int {
            val children = children
            var sum = components.size
            for (i in children.indices) {
                sum += children[i].totalNumComponents
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