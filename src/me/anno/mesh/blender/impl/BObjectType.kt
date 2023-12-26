package me.anno.mesh.blender.impl

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