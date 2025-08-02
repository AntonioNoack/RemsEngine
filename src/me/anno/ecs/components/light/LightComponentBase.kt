package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.components.FillSpace
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.DitherMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.posMod

// a light component, of which there can be multiple per object
abstract class LightComponentBase : Component(), Renderable, OnUpdate, FillSpace {

    @SerializedProperty
    var ditherMode = DitherMode.DITHER2X2

    var needsUpdate1 = true

    /**
     * update shadows every N frames, or never if <= 0
     * */
    var autoUpdate = 30

    // @DebugProperty
    @NotSerializedProperty
    var lastDrawn = 0L

    override fun fill(pipeline: Pipeline, transform: Transform) {
        lastDrawn = Time.gameTimeN
    }

    fun needsAutoUpdate(cascade: Int, numCascades: Int): Boolean {
        val autoUpdate = autoUpdate
        if (autoUpdate <= 0 || numCascades <= 0) return false
        // clickId for pseudo-randomness
        // lodIndex*autoUpdate/numLODs for distributing updates over multiple frames
        val frameIndex = Time.frameIndex + clickId + (cascade * autoUpdate) / numCascades
        return posMod(frameIndex, autoUpdate) == 0
    }

    override fun onUpdate() {
        if (lastDrawn >= Time.lastGameTimeN) {
            onVisibleUpdate()
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is LightComponentBase) return
        dst.ditherMode = ditherMode
        dst.autoUpdate = autoUpdate
        dst.needsUpdate1 = needsUpdate1
    }

    open fun onVisibleUpdate() {}
}