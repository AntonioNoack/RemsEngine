package me.anno.experiments

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.bvh.BLASBranch
import me.anno.maths.bvh.BLASLeaf
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.OS.downloads

// todo split dragon into meshlets:
//  - 6-dimensional split into x,y,z,nx,ny,nz?
//  - first by x,y,z
// todo meshlet-based culling
// todo meshlet-based LODs somhow...
// todo meshlet-based LOD hierarchy somehow...
fun main() {
    OfficialExtensions.initForTests()
    val mesh = MeshCache[downloads.getChild("3d/dragon.obj")] as Mesh
    val node = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 256)!! // todo run maxNodeSize=64 at 200 fps
    Build.isDebug = false
    val scene = Entity("Scene")
    createScene(scene, node)
    testSceneWithUI("Meshlets", scene)
}

fun createScene(entity: Entity, node: BLASNode) {
    when (node) {
        is BLASLeaf -> {
            val mesh = Mesh()
            val indices = node.geometry.indices
            val i0 = node.start * 3
            val di = node.length * 3
            mesh.positions = extract(node.geometry.positions, indices, i0, di, 3)
            mesh.normals = extract(node.geometry.normals, indices, i0, di, 3)
            mesh.uvs = extract(node.geometry.uvs, indices, i0, di, 2)
            entity.add(MeshComponent(mesh).apply {
                isInstanced = false
            })
        }
        is BLASBranch -> {
            createScene(Entity("Min", entity), node.n0)
            createScene(Entity("Max", entity), node.n1)
        }
    }
}

fun extract(src: FloatArray?, indices: IntArray, start: Int, length: Int, x: Int): FloatArray? {
    src ?: return null
    val dst = FloatArray(length * x)
    var k = 0
    for (i in 0 until length) {
        val idx = indices[start + i] * x
        for (j in 0 until x) {
            dst[k++] = src[idx + j]
        }
    }
    return dst
}

