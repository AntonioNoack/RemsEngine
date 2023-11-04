package me.anno.mesh.blender.impl

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_mesh_types.h#L52
 * */
@Suppress("SpellCheckingInspection", "unused", "UNCHECKED_CAST")
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

    val vertices = getInstantList<MVert>("*mvert")
    val polygons = getInstantList<MPoly>("*mpoly")
    val loops = getInstantList<MLoop>("*mloop")

    // in newer Blender versions, this apparently has become a custom data layer,
    //  however we find those; src: https://projects.blender.org/blender/blender/commit/6c774feba2c9a1eb5834646f597a0f2c63177914
    // -> in my sample mesh, I found it in lData with name 'UVMap' as MLoopUV, too :)
    val loopUVs = getInstantList<MLoopUV>("*mloopuv")
    val loopColor = getInstantList<MLoopCol>("*mloopcol")
    // old
    // val mFaces = ptr("*mface")
    // val mtFaces = ptr("*mtface")
    // val tFaces = ptr("*tface")
    val edges = getInstantList<MEdge>("*medge")
    val colors = getInstantList<MLoopCol>("*mcol")

    // texture space (?)
    // val location get() = vec3f("loc[3]")
    // val size get() = vec3f("size[3]")

    val vData get() = inside("vdata") as BCustomData
    val eData get() = inside("edata") as BCustomData
    val fData get() = inside("fdata") as BCustomData
    val pData get() = inside("pdata") as BCustomData
    val lData get() = inside("ldata") as BCustomData

    var fileRef: FileReference = InvalidRef

    // vertex groups:
    // in old files in MDeformVert
    val vertexGroupNames = inside("vertex_group_names") as BListBase<BDeformGroup>
    val vertexGroups = getInstantList<MDeformVert>("*dvert")

}