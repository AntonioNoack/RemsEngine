package me.anno.mesh.blender.impl

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class BMesh(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as BID

    val materials get() = getPointerArray("**mat")

    val numFaces = int("totface")
    val numVertices = int("totvert")
    val numEdges = int("totedge")
    val numPolygons = int("totpoly")
    val numLoops = int("totloop")
    val numColors = int("totcol")

    // dvert = deform group vertices

    val vertices = getQuickStructArray<MVert>("*mvert")
    val polygons = getQuickStructArray<MPoly>("*mpoly")
    val loops = getQuickStructArray<MLoop>("*mloop")
    val loopUVs = getQuickStructArray<MLoopUV>("*mloopuv")
    val loopColor = getStructArray("*mloopcol") // todo support this
    // old
    // val mFaces = ptr("*mface")
    // val mtFaces = ptr("*mtface")
    // val tFaces = ptr("*tface")
    val edges = getQuickStructArray<MEdge>("*medge")
    val colors = getQuickStructArray<MLoopCol>("*mcol")

    // texture space (?)
    // val location get() = vec3f("loc[3]")
    // val size get() = vec3f("size[3]")

    val vData get() = inside("vdata") as BCustomData
    val eData get() = inside("edata") as BCustomData
    val fData get() = inside("fdata") as BCustomData
    val pData get() = inside("pdata") as BCustomData
    val lData get() = inside("ldata") as BCustomData

    var fileRef: FileReference = InvalidRef

}