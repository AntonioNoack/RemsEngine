package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.Shader
import me.anno.io.files.InvalidRef
import me.anno.utils.pooling.JomlPools
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector3d

open class PlanarMaterialBase : Material() {

    var worldPosCenter = Vector3d()
    var scale = Vector2d(1.0)
    var tilingDir = Quaternionf()

    private fun tiling(shader: Shader) {

        // world scale correction
        val worldScale = RenderState.worldScale
        val pos = RenderState.cameraPosition
        shader.v3f(
            "tileOffset",
            ((worldPosCenter.x - pos.x) * worldScale).toFloat(),
            ((worldPosCenter.y - pos.y) * worldScale).toFloat(),
            ((worldPosCenter.z - pos.z) * worldScale).toFloat()
        )

        val texture = diffuseMap.nullIfUndefined()
            ?: emissiveMap.nullIfUndefined()
            ?: normalMap.nullIfUndefined()
            ?: InvalidRef

        // calculate final scale + aspect ratio correction
        val tex3 = getTex(texture)
        val scaleX = (if (tex3 != null) tex3.h.toDouble() / tex3.w else 1.0) / (scale.x * worldScale)
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

    override fun bind(shader: Shader) {
        super.bind(shader)
        tiling(shader)

        val cameraPosition = RenderState.cameraPosition
        shader.v3f("cameraPosition", cameraPosition.x.toFloat(), cameraPosition.y.toFloat(), cameraPosition.z.toFloat())
        shader.v1f("worldScale", RenderState.worldScale)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PlanarMaterialBase
        dst.worldPosCenter.set(worldPosCenter)
        dst.scale.set(scale)
        dst.tilingDir.set(tilingDir)
    }

}