package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.DitherMode
import me.anno.gpu.pipeline.Pipeline

// a light component, of which there can be multiple per object
abstract class LightComponentBase : Component(), Renderable {

    @SerializedProperty
    var ditherMode = DitherMode.DITHER2X2

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        lastDrawn = Time.gameTimeN
        return clickId // not itself clickable
    }

    override fun onUpdate(): Int {
        super.onUpdate()
        if (lastDrawn >= Time.lastGameTime)
            onVisibleUpdate()
        return 1
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as LightComponentBase
        dst.ditherMode = ditherMode
    }

    open fun onVisibleUpdate(): Boolean = false
}