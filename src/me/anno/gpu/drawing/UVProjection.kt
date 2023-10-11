package me.anno.gpu.drawing

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.CubemapModel
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01Cube
import me.anno.language.translation.NameDesc

enum class UVProjection(val id: Int, val doScale: Boolean, val naming: NameDesc) {
    Planar(0, true, NameDesc("Planar", "Simple plane, e.g. for 2D video", "")) {
        override fun getMesh(): Mesh = flat01Cube
    },
    Equirectangular(1, false, NameDesc("Full Cubemap", "Earth-like projection, equirectangular", "")) {
        override fun getMesh(): Mesh = CubemapModel
    },
    TiledCubemap(2, false, NameDesc("Tiled Cubemap", "Cubemap with 6 square sides", "")) {
        override fun getMesh(): Mesh = CubemapModel
    };

    abstract fun getMesh(): Mesh
}