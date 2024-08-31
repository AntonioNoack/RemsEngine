package me.anno.bugs.done

import me.anno.engine.OfficialExtensions
import me.anno.io.files.Reference.getReference

/**
 * stuff was broken :( -> because there was a inputStreamSync(), which was closed too soon
 * (I didn't test tars inside zips before)
 * */
fun main() {
    OfficialExtensions.initForTests()
    val ref = getReference(
        "E:/Assets/Animpic Studio/POLY-LiteSurvivalForest_Unity_UE.zip/" +
                "POLY-LiteSurvivalForest.unitypackage"
    )
    println(ref.listChildren())
}