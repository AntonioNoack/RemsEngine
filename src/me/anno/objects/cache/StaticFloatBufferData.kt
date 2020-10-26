package me.anno.objects.cache

import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGMesh

class StaticFloatBufferData(val buffer: StaticBuffer): CacheData {

    var minX = 0.0
    var maxX = 0.0
    var minY = 0.0
    var maxY = 0.0

    fun setBounds(svg: SVGMesh){
        minX = svg.minX
        maxX = svg.maxX
        minY = svg.minY
        maxY = svg.maxY
    }

    override fun destroy() {
        buffer.destroy()
    }
}