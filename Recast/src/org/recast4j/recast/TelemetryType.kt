package org.recast4j.recast

enum class TelemetryType {

    CONTOURS,
    CONTOURS_SIMPLIFY,
    CONTOURS_TRACE,
    CONTOURS_WALK,

    ERODE_AREA,
    MEDIAN_AREA,

    MARK_BOX_AREA,
    MARK_CYLINDER_AREA,
    MARK_CONVEX_POLY_AREA,

    REGIONS,
    REGIONS_FILTER,
    REGIONS_WATERSHED,
    REGIONS_EXPAND,
    REGIONS_FLOOD,

    DISTANCE_FIELD,
    DISTANCE_FIELD_DIST,
    DISTANCE_FIELD_BLUR,

    RASTERIZE_BOX,
    RASTERIZE_CAPSULE,
    RASTERIZE_CONVEX,
    RASTERIZE_CYLINDER,
    RASTERIZE_SPHERE,
    RASTERIZE_TRIANGLES,

    POLYMESH,
    POLYMESH_DETAIL,
    MERGE_POLYMESH,

    BUILD_LAYERS,
    BUILD_COMPACT_HEIGHTFIELD,

    FILTER_WALKABLE,
    FILTER_LEDGE,
    FILTER_LOW_OBSTACLES,

}