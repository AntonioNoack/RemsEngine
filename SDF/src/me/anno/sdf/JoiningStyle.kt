package me.anno.sdf

/**
 * like http://mercury.sexy/hg_sdf/
 * */
@Suppress("unused")
enum class JoiningStyle(val id: Int, val unionName: String, val interName: String) {
    DEFAULT(0, "sdMin", "sdMax"),
    ROUND(1, "unionRound", "interRound"),
    COLUMNS(2, "unionColumn", "interColumn"),
    STAIRS(3, "unionStairs", "interStairs"),
    CHAMFER(4, "unionChamfer", "interChamfer"),
    SOFT(5, "unionSoft", "sdMax"), // inter isn't really supported
}