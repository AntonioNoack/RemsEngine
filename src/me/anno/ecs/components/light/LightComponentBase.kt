package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.DitherMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.posMod

// a light component, of which there can be multiple per object
abstract class LightComponentBase : Component(), Renderable, OnUpdate {

    @SerializedProperty
    var ditherMode = DitherMode.DITHER2X2

    var needsUpdate1 = true
    var autoUpdate = 30

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        lastDrawn = Time.gameTimeN
        return clickId // not itself clickable
    }

    fun needsAutoUpdate(): Boolean {
        val autoUpdate = autoUpdate
        return autoUpdate > 0 && posMod(Time.frameIndex + clickId, autoUpdate) == 0
    }

    override fun onUpdate() {
        if (lastDrawn >= Time.lastGameTimeN) {
            onVisibleUpdate()
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as LightComponentBase
        dst.ditherMode = ditherMode
        dst.autoUpdate = autoUpdate
        dst.needsUpdate1 = needsUpdate1
    }

    open fun onVisibleUpdate(): Boolean = false
}