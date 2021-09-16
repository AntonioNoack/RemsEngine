package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BMesh(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as? BID

    val materials = ptr("**mat")

    val numFaces = int("totface")
    val numVertices = int("totvert")
    val numEdges = int("totedge")
    val numPolygons = int("totpoly")
    val numLoops = int("totloop")
    val numColors = int("totcol")

    val vertices = ptr("*mvert")
    val polygons = ptr("*mpoly")
    val loops = ptr("*mloop")
    val loopUVs = ptr("*mloopuv")
    val loopColor = ptr("*mloopcol")
    val mFaces = ptr("*mface")
    val mtFaces = ptr("*mtface")
    val tFaces = ptr("*tface")
    val edges = ptr("*medge")
    val colors = ptr("*mcol")

    val location = vec3f("loc[3]")
    val size = vec3f("size[3]")

    val vData = inside("vdata") as BCustomData
    val eData = inside("edata") as BCustomData
    val fData = inside("fdata") as BCustomData
    val pData = inside("pdata") as BCustomData
    val lData = inside("ldata") as BCustomData


}