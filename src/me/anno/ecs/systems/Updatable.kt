package me.anno.ecs.systems

import me.anno.ecs.Component

interface Updatable {
    /**
     * will be called once per frame, once per instance-class, and shall then iterate over all instances
     * */
    fun update(instances: Collection<Component>)

    /**
     * Lower values run first; sorted on per-class-basis only.
     *
     * If you need sorting within instances, use a custom child-class of Updatable and modify
     * fun update(instances: Collection<Component>)
     * */
    fun priority(): Int = 100 //
}