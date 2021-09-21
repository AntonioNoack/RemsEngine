package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight

object DefaultSun {

    val defaultSun = DirectionalLight()
    val defaultSunEntity = Entity()

    init {
        // more is currently not required for light rendering
        defaultSunEntity.add(defaultSun)
        defaultSun.color.set(5f)
        defaultSun.invWorldMatrix
            .identity()
            .rotateY(-0.5f)
            .rotateX(+1.0f)
    }

}