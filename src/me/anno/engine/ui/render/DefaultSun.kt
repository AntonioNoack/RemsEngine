package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import org.joml.Quaternionf

object DefaultSun {

    val defaultSun = DirectionalLight()
    val defaultSunEntity = Entity()

    init {
        // more is currently not required for light rendering
        defaultSunEntity.add(defaultSun)
        // similar to the thumbnail main light
        defaultSunEntity.rotation = Quaternionf()
            .rotateX(-1f)
            .rotateY(-0.5f)
        defaultSunEntity.validateTransform()
        defaultSun.color.set(20f)
    }
}