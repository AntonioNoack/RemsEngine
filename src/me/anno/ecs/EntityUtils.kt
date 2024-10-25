package me.anno.ecs

object EntityUtils {

    @JvmStatic
    fun Entity.setContains(child: Entity, shallContain: Boolean) {
        if (shallContain) {
            if (child !in children) {
                addChild(child)
            }
        } else if (child in children) {
            remove(child)
        }
    }

    @JvmStatic
    fun Entity.setContains(component: Component, shallContain: Boolean) {
        if (shallContain) {
            if (component !in components) {
                addComponent(component)
            }
        } else if (component in components) {
            remove(component)
        }
    }
}