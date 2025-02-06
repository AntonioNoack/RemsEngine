package org.recast4j.detour

import org.joml.Vector3f

class StraightPathItem private constructor(val pos: Vector3f, var flags: Int, var ref: Long) {
    private constructor() : this(Vector3f(), 0, 0L)

    companion object {
        private val cache = ArrayList<StraightPathItem>()

        fun create(pos: Vector3f, flags: Int, ref: Long): StraightPathItem {
            val item = synchronized(cache) {
                cache.removeLastOrNull() ?: StraightPathItem()
            }
            item.pos.set(pos)
            item.flags = flags
            item.ref = ref
            return item
        }

        fun clear(list: MutableList<StraightPathItem>) {
            synchronized(cache) { cache.addAll(list) }
            list.clear()
        }

        fun clear(item: StraightPathItem) {
            synchronized(cache) { cache.add(item) }
        }
    }

}