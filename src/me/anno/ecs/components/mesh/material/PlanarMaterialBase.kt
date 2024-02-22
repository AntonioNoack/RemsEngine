package me.anno.ecs.components.mesh.material

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.GPUShader
import me.anno.utils.pooling.JomlPools
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector3d

open class PlanarMaterialBase : Material() {

    var worldPosCenter = Vector3d()
    var scale = Vector2d(1.0)
    var tilingDir = Quaternionf()

    private fun tiling(shader: GPUShader) {

        // world scale correction
        val worldScale = RenderState.worldScale
        val pos = RenderState.cameraPosition
        shader.v3f(
            "tileOffset",
            ((worldPosCenter.x - pos.x) * worldScale).toFloat(),
            ((worldPosCenter.y - pos.y) * worldScale).toFloat(),
            ((worldPosCenter.z - pos.z) * worldScale).toFloat()
        )

        val texture = diffuseMap
            .ifUndefined(emissiveMap)
            .ifUndefined(normalMap)

        // calculate final scale + aspect ratio correction
        val tex3 = getTex(texture)
        val scaleX = (if (tex3 != null) tex3.height.toDouble() / tex3.width else 1.0) / (scale.x * worldScale)
        val scaleY = 1.0 / (scale.y * worldScale)

        val dirU = JomlPools.vec3f.create()
        val dirV = JomlPools.vec3f.create()

        tilingDir.transform(dirU.set(1f, 0f, 0f))
        tilingDir.transform(dirV.set(0f, 0f, 1f))

        shader.v3f(
            "tilingU",
            (dirU.x * scaleX).toFloat(),
            (dirU.y * scaleX).toFloat(),
            (dirU.z * scaleX).toFloat()
        )

        shader.v3f(
            "tilingV",
            (dirV.x * scaleY).toFloat(),
            (dirV.y * scaleY).toFloat(),
            (dirV.z * scaleY).toFloat()
        )
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        tiling(shader)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PlanarMaterialBase
        dst.worldPosCenter.set(worldPosCenter)
        dst.scale.set(scale)
        dst.tilingDir.set(tilingDir)
    }
}