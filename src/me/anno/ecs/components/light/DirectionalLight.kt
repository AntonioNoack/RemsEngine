package me.anno.ecs.components.light

import me.anno.ecs.components.light.PointLight.Companion.cubeMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.gui.LineShapes.drawArrowZ
import me.anno.engine.gui.LineShapes.drawBox
import me.anno.gpu.pipeline.Pipeline
import org.joml.*

class DirectionalLight : LightComponent(LightType.DIRECTIONAL) {

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        cameraMatrix: Matrix4f,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int,
        position: Vector3d,
        rotation: Quaterniond
    ) {
        cameraMatrix.set(Matrix4d(drawTransform).invert())
        cameraMatrix.setTranslation(0f, 0f, 0f)
        val sx = (1.0 / (cascadeScale * worldScale)).toFloat()
        val sz = (1.0 / (worldScale)).toFloat()
        // z must be mapped from [-1,1] to [0,1]
        // additionally it must be scaled to match the world size
        cameraMatrix.scaleLocal(sx, sx, sz * 0.5f)
        cameraMatrix.m32(0.5f)
        pipeline.frustum.defineOrthographic(drawTransform, resolution, position, rotation)
    }

    override fun getLightPrimitive(): Mesh = cubeMesh

    override fun drawShape() {
        drawBox(entity)
        drawArrowZ(entity, +1.0, -1.0)
    }

    override fun clone(): DirectionalLight {
        val clone = DirectionalLight()
        copy(clone)
        return clone
    }

    override val className: String = "DirectionalLight"

}