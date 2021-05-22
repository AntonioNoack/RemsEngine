package me.anno.objects.meshes

import me.anno.gpu.buffer.StaticBuffer
import me.anno.mesh.fbx.model.FBXGeometry
import me.anno.mesh.obj.Material

class FBXData(val geometry: FBXGeometry, val objData: Map<Material, StaticBuffer>)