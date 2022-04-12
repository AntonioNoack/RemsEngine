package me.anno.ecs.components.mesh.sdf.shapes

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.abs

// todo implement terrain
// there are multiple ways that we could implement...
// https://www.shadertoy.com/view/4ttSWf
// https://iquilezles.org/www/articles/fbm/fbm.htm
// https://iquilezles.org/www/articles/fbmsdf/fbmsdf.htm
class SDFTerrain : PositionMapper() {

    var amplitude = 10f

    var seed = Vector2f()
        set(value) {
            field.set(value)
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        TODO("Not yet implemented")
    }

    override fun calcTransform(pos: Vector4f) {
        TODO("Not yet implemented")
    }

    override fun applyTransform(bounds: AABBf) {
        val delta = abs(amplitude) * 0.5f
        bounds.minY -= delta
        bounds.maxY += delta
    }

    override fun clone(): PrefabSaveable {
        val clone = SDFTerrain()
        copy(clone)
        return clone
    }

    override val className = "SDFTerrain"


}