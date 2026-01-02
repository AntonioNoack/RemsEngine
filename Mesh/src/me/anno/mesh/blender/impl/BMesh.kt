package me.anno.mesh.blender.impl

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.attr.AttributeStorage
import me.anno.mesh.blender.impl.mesh.MDeformVert
import me.anno.mesh.blender.impl.mesh.MEdge
import me.anno.mesh.blender.impl.mesh.MLoop
import me.anno.mesh.blender.impl.mesh.MLoopCol
import me.anno.mesh.blender.impl.mesh.MLoopUV
import me.anno.mesh.blender.impl.mesh.MPoly
import me.anno.mesh.blender.impl.mesh.MVert

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_mesh_types.h#L52
 * */
@Suppress("SpellCheckingInspection", "unused", "UNCHECKED_CAST")
class BMesh(ptr: ConstructorData) : BlendData(ptr) {

    val id get() = getPartStruct("id") as BID

    val materials get() = getPointerArray("**mat")

    val numFacesOld get() = i32("totface")
    val numVertices get() = i32("totvert")
    val numEdges get() = i32("totedge")
    val numPolygons get() = i32("totpoly") // = num_faces
    val numLoops get() = i32("totloop")
    val numColors get() = i32("totcol")

    // dvert = deform group vertices

    val vertices get() = getInstantList<MVert>("*mvert", numVertices)
    val polygons get() = getInstantList<MPoly>("*mpoly", numPolygons)
    val loops get() = getInstantList<MLoop>("*mloop", numLoops)

    // in newer Blender versions, this apparently has become a custom data layer,
    //  however we find those; src: https://projects.blender.org/blender/blender/commit/6c774feba2c9a1eb5834646f597a0f2c63177914
    // -> in my sample mesh, I found it in lData with name 'UVMap' as MLoopUV, too :)
    val loopUVs get() = getInstantList<MLoopUV>("*mloopuv")
    val loopColor get() = getInstantList<MLoopCol>("*mloopcol")

    // old
    // val mFaces = ptr("*mface")
    // val mtFaces = ptr("*mtface")
    // val tFaces = ptr("*tface")
    val edges get() = getInstantList<MEdge>("*medge")
    val colors get() = getInstantList<MLoopCol>("*mcol")

    // texture space (?)
    // val location get() = vec3f("loc[3]")
    // val size get() = vec3f("size[3]")

    val vData get() = getPartStruct("vdata") as BCustomData
    val eData get() = getPartStruct("edata") as BCustomData
    val fData get() = getPartStruct("fdata") as BCustomData
    val pData get() = getPartStruct("pdata") as BCustomData
    val lData get() = getPartStruct("ldata") as BCustomData

    var fileRef: FileReference = InvalidRef

    // vertex groups:
    // in old files in MDeformVert
    val vertexGroupNames = getPartStruct("vertex_group_names") as? BListBase<BDeformGroup>
    val vertexGroups get() = getInstantList<MDeformVert>("*dvert")

    val editMesh get() = getPointer("*edit_mesh")

    val polyOffsetIndices by lazy {
        // data could become complicated, if it was split into multiple blocks
        val offset = getOffset("*poly_offset_indices")
        if (offset >= 0) {
            val pointer = pointer(offset)
            val block = file.blockTable.findBlock(file, pointer)
            if (block != null) {
                val dataPosition = pointer + block.dataOffset
                getRawI32s(dataPosition.toInt(), numPolygons + 1)
            } else null
        } else null
    }

    /**
     * stores vertex and face data for Blender 5
     * */
    val attributes: AttributeStorage?
        get() {
           val value = getPartStruct("attribute_storage") as? AttributeStorage
            println("Got AttributeStorage@$positionInFile for Mesh@${value?.positionInFile}")
            return value
        }

}