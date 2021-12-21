package me.anno.ui.base.components

import me.anno.ecs.prefab.PrefabSaveable

open class LTRB(var left: Int, var top: Int, var right: Int, var bottom: Int) : PrefabSaveable() {

    val width: Int get() = left + right
    val height: Int get() = top + bottom

    override fun clone(): LTRB {
        return LTRB(left, top, right, bottom)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as LTRB
        clone.left = left
        clone.top = top
        clone.right = right
        clone.bottom = bottom
    }

    override val className: String = "LTRB"

}