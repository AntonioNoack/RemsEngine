package me.anno.objects.meshes

import me.anno.animation.skeletal.SkeletalAnimation
import me.anno.gpu.buffer.StaticBuffer
import me.anno.mesh.fbx.model.FBXGeometry
import me.anno.mesh.obj.Material

class FBXData(val geometry: FBXGeometry, val objData: Map<Material, StaticBuffer>) {

    val skeleton = geometry.generateSkeleton()
    val animation = SkeletalAnimation(skeleton, false)

}