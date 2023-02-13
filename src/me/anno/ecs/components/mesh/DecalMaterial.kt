package me.anno.ecs.components.mesh

import me.anno.cache.CacheSection
import me.anno.ecs.Entity
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.maths.Maths.hasFlag
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.types.Booleans.toInt

// todo decal pass:
//  Input: pos, normal (we could pass in color theoretically, but idk)
//  Output: new color, new normal, new emissive
// todo different blend modes: additive, subtractive, default, ...
class DecalMaterial : Material() {

    companion object {
        val timeout = 10_000L
        val shaderLib = CacheSection("DecalLib")

        @JvmStatic
        fun main(args: Array<String>) {
            val scene = Entity()
            val decal = Entity()
            val decalMesh = MeshComponent(flatCube.back)
            scene.add(decal)
            decal.add(decalMesh)
            val decalMat = DecalMaterial()
            decalMat.diffuseBase.set(1f, 0f, 0f, 1f)
            decalMesh.materials = listOf(decalMat.ref)
            val baseMesh = Entity()
            baseMesh.add(MeshComponent(flatCube.front))
            scene.add(baseMesh)
            testSceneWithUI(scene)
        }
    }

    // can we support this in forward rendering?
    // yes, but it will be a bit more expensive
    // a) list of all decals for all pixels -> bad
    // b) render normal + position extra; and apply lighting twice

    // could be implemented using compute or gfx shader

    var writeColor = true
    var writeNormal = false
    var writeEmissive = false
    var writeRoughness = false
    var writeMetallic = false

    // todo this could be a compute shader, or a regular shader working on a copy of data :)

    fun getShader() {
        val id = writeColor.toInt() + writeNormal.toInt(2) +
                writeEmissive.toInt(4) + writeRoughness.toInt(8) + writeMetallic.toInt(16)
        shaderLib.getEntry(id, timeout, false) {
            // todo create shader
            val color = it.hasFlag(1)
            val normal = it.hasFlag(2)
            val emissive = it.hasFlag(4)
            val roughness = it.hasFlag(8)
            val metallic = it.hasFlag(16)
            object : ECSMeshShader("Decal") {

                // depth can be skipped, depth is not written

                // todo read position for effect modifier

                override fun createForwardShader(
                    postProcessing: ShaderStage?,
                    isInstanced: Boolean,
                    isAnimated: Boolean,
                    motionVectors: Boolean,
                    limitedTransform: Boolean
                ): Shader {
                    // todo data needs to be supplied as input
                    val base = createBase(isInstanced, isAnimated, !motionVectors, motionVectors, limitedTransform)
                    base.addFragment(postProcessing)
                    val shader = base.create()
                    finish(shader)
                    return shader
                }

                override fun createDeferredShader(
                    deferred: DeferredSettingsV2,
                    isInstanced: Boolean,
                    isAnimated: Boolean,
                    motionVectors: Boolean,
                    limitedTransform: Boolean
                ): Shader {
                    // todo data needs to be supplied as input
                    val base = createBase(isInstanced, isAnimated, !motionVectors, motionVectors, limitedTransform)
                    base.outputs = deferred

                    // build & finish
                    val shader = base.create()
                    finish(shader)
                    return shader
                }
            }
        }
    }

    // theoretically, this also could support applying clear-coat

    override val className get() = "DecalMaterial"

}