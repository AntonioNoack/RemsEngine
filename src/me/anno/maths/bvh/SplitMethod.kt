package me.anno.maths.bvh

enum class SplitMethod {
    MIDDLE,
    MEDIAN,
    MEDIAN_APPROX, // doesn't sort, uses statistical median; O(n) instead of O(n log n) -> 8x faster for Sponza
    SURFACE_AREA_HEURISTIC,
    HIERARCHICAL_LINEAR // https://research.nvidia.com/sites/default/files/pubs/2011-08_Simpler-and-Faster/main.pdf
}