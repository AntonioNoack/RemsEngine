package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class BObject(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as BID
    val materials = getStructArray("**mat")
    val type = short("type")
    val parType = short("partype")
    val parent get() = getPointer("*parent") as? BObject

    // Final worldspace matrix with constraints & animsys applied.
    val finalWSMatrix get() = mat4x4("obmat[4][4]")

    val data get() = getPointer("*data") // type specific


    enum class BObjectType(val id: Int) {

        OB_EMPTY(0),
        OB_MESH(1),
        OB_CURVE(2),
        OB_SURF(3),
        OB_FONT(4),
        OB_MBALL(5),

        OB_LAMP(10),
        OB_CAMERA(11),

        OB_SPEAKER(12),
        OB_LIGHTPROBE(13),

        OB_LATTICE(22),

        OB_ARMATURE(25),

        // Grease Pencil object used in 3D view but not used for annotation in 2D.
        OB_GPENCIL(26),

        OB_HAIR(27),

        OB_POINTCLOUD(28),

        OB_VOLUME(29);


    }

    companion object {

        val objectTypeById = Array(30) {
            BObjectType.OB_EMPTY
        }

        init {
            for (v in BObjectType.values()) {
                objectTypeById[v.id] = v
            }
        }
    }
}