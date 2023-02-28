package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import org.joml.Quaterniond

object DefaultSun {

    val defaultSun = DirectionalLight()
    val defaultSunEntity = Entity()

    init {
        // more is currently not required for light rendering
        defaultSunEntity.add(defaultSun)
        // similar to the thumbnail main light
        defaultSunEntity.rotation = Quaterniond()
            .rotateX(-1.0)
            .rotateY(-0.5)
        defaultSunEntity.validateTransform()
        defaultSun.color.set(20f)
    }

}