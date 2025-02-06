package org.recast4j.recast

enum class PartitionType {
    /**
     * Watershed partitioning
     *  - the classic Recast partitioning
     *  - creates the nicest tessellation
     *  - usually slowest
     *  - partitions the heightfield into nice regions without holes or overlaps
     *  - there are some corner cases where this method creates produces holes and overlaps
     *  - holes may appear when a small obstacles is close to large open area (triangulation can handle this)
     *  - overlaps may occur if you have narrow spiral corridors (i.e., stairs), this make triangulation to fail
     *  - generally the best choice if you precompute the navmesh, use this if you have large open areas
     * */
    WATERSHED,

    /**
     * Monotone portioning
     * - fastest
     * - partitions the heightfield into regions without holes and overlaps (guaranteed)
     * - creates long thin polygons, which sometimes causes paths with detours
     * - use this if you want fast navmesh generation
     * */
    MONOTONE,

    /**
     * Layer partitioning
     *  - quite fast
     *  - partitions the heightfield into non-overlapping regions
     *  - relies on the triangulation code to cope with holes (thus slower than monotone partitioning)
     *  - produces better triangles than monotone partitioning
     *  - does not have the corner cases of watershed partitioning
     *  - can be slow and create ugly tessellation (still better than monotone) if you have large open areas with small obstacles (not a problem if you use tiles)
     *  - good choice to use for tiled navmesh with medium and small sized tiles
     * */
    LAYERS
}