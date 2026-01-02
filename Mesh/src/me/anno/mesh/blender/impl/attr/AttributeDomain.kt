package me.anno.mesh.blender.impl.attr

/**
 * https://github.com/blender/blender/blob/df05d3baea4fd8b210243ee226cea00e14b12e6d/source/blender/blenkernel/BKE_attribute.hh#L67
 * enum class AttrDomain : int8_t {
 *   Auto = -1,  /* Used to choose automatically based on other data. */
 *   Point = 0, /* Mesh, Curve or Point Cloud Point. */
 *   Edge = 1, /* Mesh Edge. */
 *   Face = 2, /* Mesh Face. */
 *   Corner = 3, /* Mesh Corner. */
 *   Curve = 4, /* A single curve in a larger curve data-block. */
 *   Instance = 5, /* Instance. */
 *   Layer = 6, /* A layer in a grease pencil data-block. */
 * };
 * */
enum class AttributeDomain {
    Point,
    Edge,
    Face,
    Corner,
    Curve,
    Instance,
    Layer
}