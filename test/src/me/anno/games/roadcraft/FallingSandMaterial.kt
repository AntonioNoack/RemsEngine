package me.anno.games.roadcraft

import me.anno.Time
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.Material
import me.anno.games.roadcraft.FallingSandShader.optY
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.GPUShader
import me.anno.maths.Maths
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.sq
import org.joml.Vector3f
import org.joml.Vector3i

class FallingSandMaterial : Material() {

    val size = Vector3i(70, (70 / optY).toInt(), 70)

    @Type("Color3")
    var brightColor = Vector3f(0.950f, 0.913f, 0.860f)

    @Type("Color3")
    var darkColor = Vector3f(0.668f, 0.593f, 0.500f)

    var flowSpeed = 200f

    init {
        shader = FallingSandShader
        pipelineStage = PipelineStage.GLASS
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        shader.v3i("bounds", size)
        // max amount of blocks that can be traversed
        val maxSteps = Maths.max(1, size.x + size.y + size.z)
        shader.v1i("maxSteps", maxSteps shr 1) // half is always enough
        shader.v1f("time", fract(Time.gameTime, 1000.0).toFloat() * flowSpeed / optY)
        shader.v3f("brightColorSq", sq(brightColor.x), sq(brightColor.y), sq(brightColor.z))
        shader.v3f("darkColorSq", sq(darkColor.x), sq(darkColor.y), sq(darkColor.z))
    }
}