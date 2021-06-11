package me.anno.objects.meshes

import de.javagl.jgltf.model.GltfAnimations.createModelAnimations
import de.javagl.jgltf.model.GltfModel
import me.anno.mesh.gltf.ExternalCameraImpl
import me.anno.mesh.gltf.GltfViewerLwjgl

class GlTFData(val viewer: GltfViewerLwjgl, val model: GltfModel, val camera: ExternalCameraImpl){

    val animationModels = model.animationModels
    val animationNames = animationModels.map { it.name }
    val animations = createModelAnimations(animationModels)


}