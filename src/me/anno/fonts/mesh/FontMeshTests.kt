package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticBuffer
import java.awt.Font

typealias AlignmentGroup =
        Triple<HashMap<
                Pair<Int, Int>, Double>, // |a| = |ab| - |b|
                HashMap<Int, Double>, // |a|
                HashMap<Int, StaticBuffer>> // triangles of a

fun main() {
    // italic, bold serif, 😉 had an issue, solved by randomizing the locations :)
    // we should detect such problem locations instead...
    FontMesh(Font.decode("Serif"), "\uD83D\uDE09", true)
    // FontMesh(Font.decode("Serif"), "Hi! \uD83D\uDE09")
}