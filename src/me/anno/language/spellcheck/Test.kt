package me.anno.language.spellcheck

import me.anno.language.Language
import me.anno.studio.rems.Project
import me.anno.studio.rems.RemsStudio.project
import me.anno.utils.LOGGER
import me.anno.utils.OS

fun main(){

    project = Project("", OS.downloads)
    project?.language = Language.AmericanEnglish

    Spellchecking.check("I love you", true, 0)

    var answer: List<*>? = null
    while (answer == null){
        answer = Spellchecking.check("I love you", true, 0)
        Thread.sleep(10)
    }

    LOGGER.info(answer)

}