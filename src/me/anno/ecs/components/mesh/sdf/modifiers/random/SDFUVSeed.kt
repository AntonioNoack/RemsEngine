package me.anno.ecs.components.mesh.sdf.modifiers.random

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.SDFArray
import me.anno.ecs.components.mesh.sdf.shapes.SDFSphere
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXBase
import me.anno.utils.OS.pictures
import org.joml.Vector4f

// notice: to disable color interpolation, disable linear filtering inside the material :)
// todo multiple uv modifiers won't work together correctly (except for now, where everything is just random,
//  and only this one is using textures in sdf materials)
class SDFUVSeed : SDFRandom() {

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seed: String
    ): String? {
        builder.append("uv=nextRandF2(").append(seed).append(");\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seed: Int) {}

    override fun clone(): PrefabSaveable {
        val clone = SDFUVSeed()
        copy(clone)
        return clone
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ECSRegistry.init()
            val entity = Entity()
            val shape = SDFSphere()
            val array = SDFArray()
            array.count.set(100)
            array.cellSize.set(3f)
            shape.addChild(array)
            val material = Material()
            material.diffuseMap = pictures.getChild("normal bricks.png")
            material.linearFiltering = false
            shape.sdfMaterials = listOf(material.ref)
            val random = SDFUVSeed()
            shape.addChild(random)
            entity.add(shape)
            GFXBase.disableRenderDoc()
            testSceneWithUI(entity)
        }
    }

}