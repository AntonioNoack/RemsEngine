package me.anno.graph.visual.render.scene

enum class DrawSkyMode(val id: Int) {
    /**
     * benefit: if far away, color could be blurred with background sky
     * */
    BEFORE_GEOMETRY(0),

    /**
     * benefit: fastest ^^
     * */
    DONT_DRAW_SKY(1),

    /**
     * benefit: only visible sky pixels need to be computed
     * */
    AFTER_GEOMETRY(2)
}