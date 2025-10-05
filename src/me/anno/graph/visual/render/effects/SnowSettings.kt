package me.anno.graph.visual.render.effects

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.systems.GlobalSetting
import me.anno.ecs.systems.OnUpdate
import me.anno.utils.types.Floats.toRadians
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Settings for SnowNode.
 *
 * The instance in your scene with the highest priority will be chosen.
 * */
class SnowSettings : Component(), GlobalSetting, OnUpdate {

    @Range(0.0, 100.0)
    var density = 2f

    @Range(0.0001, 0.5)
    var flakeSize = 0.02f

    var velocity = Vector3d(-0.3f, -1.5f, 0f)
    var position = Vector3d(0f, 0f, 0f)

    // where zoom-in is appearing when the density changes
    var center = Vector3d(0f, 100f, 0f)

    @Type("Color3HDR")
    var color = Vector3f(10f)

    @Range(1.0, 100.0)
    var elongation = 1f

    @Range(1e-38, 1e38)
    var fogDistance = 1000f

    /**
     * How much the sky is affected by the current snow fall.
     * At the moment, just a constant, but could be changed to a gradient in the future.
     *
     * Snow is very bright, so maybe not set this to 1.0 straight away.
     * */
    @Range(0.0, 1.0)
    var skySnowiness = 0.1f

    var worldRotation = Quaternionf()

    override var priority = 0.0

    private var lastDensity = density

    @DebugAction
    fun configureRain() {
        color.set(3f)
        density = 1.3f
        velocity.set(0f, -8f, 0f)
        flakeSize = 0.005f
        elongation = 30f
        // tilt rain a bit
        worldRotation.rotateX((15f).toRadians())
        skySnowiness = 0f
    }

    @DebugAction
    fun configureSnow() {
        color.set(10f)
        density = 2f
        velocity.set(-0.3f, -1.5f, 0f)
        flakeSize = 0.02f
        elongation = 1f
        worldRotation.identity()
        skySnowiness = 0.1f
    }

    override fun onUpdate() {
        val deltaDensity = (lastDensity / density).toDouble()
        lastDensity = density
        if (deltaDensity != 1.0 && deltaDensity in 0.5..2.0) {
            position.add(center)
            position.mul(deltaDensity)
            position.sub(center)
        }
    }
}
