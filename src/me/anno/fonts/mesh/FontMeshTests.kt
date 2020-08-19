package me.anno.fonts.mesh

import me.anno.gpu.buffer.StaticFloatBuffer
import java.awt.Font


typealias AlignmentGroup =
        Triple<HashMap<
                Pair<Int, Int>, Double>, // |a| = |ab| - |b|
                HashMap<Int, Double>, // |a|
                HashMap<Int, StaticFloatBuffer>> // triangles of a

fun main() {
    // italic, bold serif, 😉 had an issue, solved by randomizing the locations :)
    // maybe we should detect such problem locations instead...
    FontMesh(Font.decode("Serif"), "\uD83D\uDE09", true)
    // FontMesh(Font.decode("Serif"), "Hi! \uD83D\uDE09")

    // todo try to calculate the spacing between characters...
    // spacing = half left + space + half right


}