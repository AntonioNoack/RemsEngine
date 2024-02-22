package me.anno.ecs.systems

import me.anno.ecs.Component

interface Updatable {
    /**
     * will be called once per frame, once per instance-class, and shall then iterate over all instances
     * */
    fun update(instances: Collection<Component>)
}