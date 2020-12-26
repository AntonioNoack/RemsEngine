package me.anno.objects.modes

import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01Cube
import me.anno.gpu.buffer.StaticBuffer
import me.anno.objects.utils.CubemapModel.cubemapModel

enum class UVProjection(val id: Int, val doScale: Boolean, val displayName: String, val description: String){
    Planar(0, true, "Planar", "Simple plane, e.g. for 2D video"){
        override fun getBuffer(): StaticBuffer = flat01Cube
    },
    Equirectangular(1, false, "Full Cubemap", "Earth-like projection, equirectangular"){
        override fun getBuffer(): StaticBuffer = cubemapModel
    },
    TiledCubemap(2, false, "Tiled Cubemap", "Cubemap with 6 square sides"){
        override fun getBuffer(): StaticBuffer = cubemapModel
    };
    abstract fun getBuffer(): StaticBuffer
}