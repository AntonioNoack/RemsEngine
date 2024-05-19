package me.anno.mesh.blender.impl

/**
 * https://github.com/dfelinto/blender/blob/master/source/blender/makesdna/DNA_customdata_types.h
 * */
enum class BCustomLayerType(val id: Int) {
    AUTO_FROM_NAME(-1),
    M_VERT(0),
    M_STICKY(1), // deprecated
    M_DEFORM_VERT(2),
    M_EDGE(3),
    M_FACE(4),
    MT_FACE(5),
    M_COL(6),
    ORIG_INDEX(7),
    NORMAL(8),
    FACE_MAP(9),
    PROP_FLOAT(10),
    PROP_INT(11),
    PROP_STRING(12),
    ORIG_SPACE(13),
    ORCO(14),
    MT_EX_POLY(15),
    M_LOOP_UV(16),
    M_LOOP_COL(17),
    TANGENT(18),
    MDISPS(19),
    PREVIEW_M_COL(20),
    CLOTH_ORCO(23),
    M_POLY(25),
    M_LOOP(26),
    SHAPE_KEY_INDEX(27),
    SHAPE_KEY(28),
    BLEND_WEIGHT(29),
    CREASE(30),
    ORIG_SPACE_M_LOOP(31),
    PREVIEW_M_LOOP_COL(32),
    BM_ELEM_PYTHON_POINTER(33),

    PAINT_MASK(34),
    GRID_PAINT_MASK(35),
    M_VERT_SKIN(36),
    FREESTYLE_EDGE(37),
    FREESTYLE_FACE(38),
    M_LOOP_TANGENT(39),
    TESS_LOOP_NORMAL(40),
    CUSTOM_LOOP_NORMAL(41),
    SCULPT_FACE_SETS(42),

    PROP_INT8(45),
    PROP_COLOR(47),
    PROP_FLOAT32_V3(48),
    PROP_FLOAT32_V2(49),
    PROP_BOOL(50),

    HAIR_LENGTH(51);

    companion object {
        val idToValue = entries.associateBy { it.id }
    }
}