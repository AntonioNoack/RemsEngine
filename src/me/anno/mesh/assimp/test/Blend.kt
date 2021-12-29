package me.anno.mesh.assimp.test

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS
/*import org.blender.dna.ListBase
import org.blender.dna.Mesh
import org.blender.utils.MainLib
import org.cakelab.blender.io.BlenderFile*/

//  todo we need to split the project, or create our own kotlin compiler...

fun main() {

    // todo Assimps model loader is broken:
    //  - compile it ourselves
    //  - create a loader with jBlend

    // val ref = getReference(OS.documents, "Bricks/2x4.blend") // does not work with Assimp, because it has multiple roots :/
    val ref = getReference(OS.documents, "Blender/CustomHair.blend")
    // PrefabCache.getPrefab(ref)

    /*val file = BlenderFile(ref.unsafeFile)
    val main = MainLib(file)
    file.close()

    val folder = InnerFolder(ref)

    main.mesh.

    fun createMesh(mesh: Mesh) {
        val name = mesh.id.name.asString()
        // , col: ${mesh.totcol} crashes

        println("poly: ${mesh.totpoly}, vert: ${mesh.totvert}, edges: ${mesh.totedge}, " +
                "faces: ${mesh.totface}, loops: ${mesh.totloop}, select: ${mesh.totselect}")
        val polys = mesh.mpoly.toArray(mesh.totpoly) ?: null
        println(polys?.joinToString())
        val vert = mesh.mvert.toArray(mesh.totvert) ?: null
        println(vert?.joinToString())
        println("vdata: ${mesh.vdata.totlayer}")
    }

    // todo get all data
    var m: MainLib? = main
    while (m != null) {
        if (m.mesh != null) {
            println("found mesh ${m.mesh.id.name.asString()}")
            createMesh(m.mesh)
        }
        m = main.next ?: null
    }


    val scene = main.scene
    val world = scene.world.get()

    val camera = scene.camera.get()*/

}