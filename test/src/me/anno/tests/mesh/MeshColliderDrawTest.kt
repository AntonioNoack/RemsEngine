package me.anno.tests.mesh

import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // we tried to optimize it, but it didn't really work out, and I didn't want to invest much more time into it
    //  if (Input.isShiftDown) {
    //            // the old way is really inefficient:
    //            //  draw mesh as lines with specific material
    //            // todo the mesh scale isn't correct :/
    //            val transform = transform ?: sampleEntity.transform
    //            val material = debugMaterial
    //            color.toVecRGBA(material.diffuseBase)
    //            GFXState.drawLines.use(true) {
    //                val stage = pipeline.defaultStage
    //                if (Input.isControlDown) {
    //                    // todo here, the transform is even weirder...
    //                    stage.draw(pipeline, transform, this, material, 0, mesh)
    //                } else {
    //                    val shader = ECSShaderLib.simpleShader.value
    //                    shader.use()
    //                    PipelineStageImpl.bindJitterUniforms(shader)
    //                    PipelineStageImpl.bindCameraUniforms(shader, pipeline.applyToneMapping)
    //                    val bounds = AABBd(mesh.getBounds())
    //                    bounds.transform(transform.globalTransform)
    //                    PipelineStageImpl.bindLightUniforms(pipeline, shader, bounds, false)
    //                    PipelineStageImpl.bindTransformUniforms(shader, transform)
    //                    material.bind(shader)
    //                    for (i in 0 until mesh.numMaterials) {
    //                        mesh.draw(pipeline, shader, i)
    //                    }
    //                }
    //            }
    //        } else {
    val mesh = IcosahedronModel.createIcosphere(2)
    val collider = MeshCollider(mesh.ref)
    testSceneWithUI("MeshCollider DrawTest", collider) {
        EditorState.select(collider)
    }
}