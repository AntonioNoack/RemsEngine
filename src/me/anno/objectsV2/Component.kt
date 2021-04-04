package me.anno.objectsV2

import me.anno.objects.Transform

abstract class Component : Transform() {

    override fun getApproxSize(): Int = 1000
    override fun isDefaultValue(): Boolean = false

}