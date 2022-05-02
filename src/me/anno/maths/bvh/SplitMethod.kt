package me.anno.maths.bvh

enum class SplitMethod {
    MIDDLE,
    MEDIAN,
    SURFACE_AREA_HEURISTIC,
    HIERARCHICAL_LINEAR // https://research.nvidia.com/sites/default/files/pubs/2011-08_Simpler-and-Faster/main.pdf
}