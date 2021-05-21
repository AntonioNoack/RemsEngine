package me.anno.objects

import me.anno.gpu.ShaderLib.colorForceFieldBuffer
import me.anno.gpu.ShaderLib.maxColorForceFields
import me.anno.gpu.ShaderLib.uvForceFieldBuffer
import me.anno.gpu.shader.Shader
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.animation.AnimatedProperty
import me.anno.objects.attractors.EffectColoring
import me.anno.objects.attractors.EffectMorphing
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.types.Floats.put3
import org.joml.Vector3fc
import org.joml.Vector4f
import org.lwjgl.opengl.GL20.glUniform3fv
import org.lwjgl.opengl.GL20.glUniform4fv
import kotlin.math.abs
import kotlin.math.sqrt

abstract class GFXTransform(parent: Transform?) : Transform(parent) {

    init {
        timelineSlot.setDefault(0)
    }

    val attractorBaseColor = AnimatedProperty.color(Vector4f(1f))

    // sure about that??...
    override fun getStartTime(): Double = 0.0

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "attractorBaseColor", attractorBaseColor)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "attractorBaseColor" -> attractorBaseColor.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val fx = getGroup("Effects", "Visual Effects Settings", "effects")
        fx += vi("Coloring: Base Color", "Base color for coloring", attractorBaseColor, style)
    }

    open fun transformLocally(pos: Vector3fc, time: Double): Vector3fc {
        return pos
    }

    fun uploadAttractors(shader: Shader, time: Double) {

        uploadUVAttractors(shader, time)
        uploadColorAttractors(shader, time)

    }

    fun uploadUVAttractors(shader: Shader, time: Double) {

        // has no ability to display them
        if (shader["forceFieldUVCount"] < 0) return

        var attractors = children
            .filterIsInstance<EffectMorphing>()

        attractors.forEach {
            it.lastLocalTime = it.getLocalTime(time)
            it.lastInfluence = it.influence[it.lastLocalTime]
        }

        attractors = attractors.filter {
            it.lastInfluence != 0f
        }

        if (attractors.size > maxColorForceFields)
            attractors = attractors
                .sortedByDescending { it.lastInfluence }
                .subList(0, maxColorForceFields)

        shader.v1("forceFieldUVCount", attractors.size)
        if (attractors.isNotEmpty()) {
            val loc1 = shader["forceFieldUVs"]
            val buffer = uvForceFieldBuffer
            if (loc1 > -1) {
                buffer.position(0)
                for (attractor in attractors) {
                    val localTime = attractor.lastLocalTime
                    val position = transformLocally(attractor.position[localTime], time)
                    buffer.put(position.x() * 0.5f + 0.5f)
                    buffer.put(position.y() * 0.5f + 0.5f)
                    buffer.put(position.z())
                }
                buffer.position(0)
                glUniform3fv(loc1, buffer)
            }
            val loc2 = shader["forceFieldUVSpecs"]
            if (loc2 > -1) {
                buffer.position(0)
                val sx = if (this is Video) 1f / lastW else 1f
                val sy = if (this is Video) 1f / lastH else 1f
                for (attractor in attractors) {
                    val localTime = attractor.lastLocalTime
                    val weight = attractor.lastInfluence
                    val sharpness = attractor.sharpness[localTime]
                    val scale = attractor.scale[localTime]
                    buffer.put(sqrt(sy / sx) * weight * scale.z() / scale.x())
                    buffer.put(sqrt(sx / sy) * weight * scale.z() / scale.y())
                    buffer.put(10f / (scale.z() * weight * weight))
                    buffer.put(sharpness)
                }
                buffer.position(0)
                glUniform4fv(loc2, buffer)
            }
        }

    }

    fun uploadColorAttractors(shader: Shader, time: Double) {

        // has no ability to display them
        if (shader["forceFieldColorCount"] < 0) return

        var attractors = children
            .filterIsInstance<EffectColoring>()

        attractors.forEach {
            it.lastLocalTime = it.getLocalTime(time)
            it.lastInfluence = it.influence[it.lastLocalTime]
        }

        if (attractors.size > maxColorForceFields)
            attractors = attractors
                .sortedByDescending { it.lastInfluence }
                .subList(0, maxColorForceFields)

        shader.v1("forceFieldColorCount", attractors.size)
        if (attractors.isNotEmpty()) {
            shader.v4("forceFieldBaseColor", attractorBaseColor[time])
            val buffer = colorForceFieldBuffer
            buffer.position(0)
            for (attractor in attractors) {
                val localTime = attractor.lastLocalTime
                val color = attractor.color[localTime]
                val colorM = attractor.colorMultiplier[localTime]
                buffer.put(color.x() * colorM)
                buffer.put(color.y() * colorM)
                buffer.put(color.z() * colorM)
                buffer.put(color.w())
            }
            buffer.position(0)
            glUniform4fv(shader["forceFieldColors"], buffer)
            buffer.position(0)
            for (attractor in attractors) {
                val localTime = attractor.lastLocalTime
                val position = transformLocally(attractor.position[localTime], time)
                val weight = attractor.lastInfluence
                buffer.put3(position)
                buffer.put(weight)
            }
            buffer.position(0)
            glUniform4fv(shader["forceFieldPositionsNWeights"], buffer)
            buffer.position(0)
            val sx = if (this is Video) 1f / lastW else 1f
            val sy = if (this is Video) 1f / lastH else 1f
            for (attractor in attractors) {
                val localTime = attractor.lastLocalTime
                val scale = attractor.scale[localTime]
                val power = attractor.sharpness[localTime]
                buffer.put(abs(sy / sx / scale.x()))
                buffer.put(abs(sx / sy / scale.y()))
                buffer.put(abs(1f / scale.z()))
                buffer.put(power)
            }
            buffer.position(0)
            glUniform4fv(shader["forceFieldColorPowerSizes"], buffer)
        }

    }

    companion object {

        fun uploadAttractors(transform: GFXTransform?, shader: Shader, time: Double){
            transform?.uploadAttractors(shader, time) ?: uploadAttractors0(shader)
        }

        fun uploadAttractors0(shader: Shader) {

            // localScale, localOffset not needed
            shader.v1("forceFieldColorCount", 0)
            shader.v1("forceFieldUVCount", 0)

        }
    }

}