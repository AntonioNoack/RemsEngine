package me.anno.graph.visual.render.scene

enum class DrawSkyMode {
    /**
     * benefit: if far away, color could be blurred with background sky
     * */
    BEFORE_GEOMETRY,

    /**
     * benefit: fastest ^^
     * */
    DONT_DRAW_SKY,

    /**
     * benefit: only visible sky pixels need to be computed
     * */
    AFTER_GEOMETRY
}