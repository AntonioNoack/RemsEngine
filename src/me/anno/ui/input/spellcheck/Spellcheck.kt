package me.anno.ui.input.spellcheck

/*import org.languagetool.JLanguageTool
import org.languagetool.language.BritishEnglish
import org.languagetool.rules.RuleMatch

// checking spellings and grammar rules with LanguageTool is slow, but it should be worth it for the writing person :)

fun main() {

    //"/org/languagetool/resource", "/org/languagetool/rules"
    // TokenizerModel(ResourceHelper.loadResource("org/languagetool/rules/en-token.bin"))
    // println(String(getStream("/en-token.bin").readBytes()))

    val t0 = System.nanoTime()

    val langTool = JLanguageTool(BritishEnglish())

    val t1 = System.nanoTime()

    // comment in to use statistical ngram data:
    //      langTool.activateLanguageModelRules(new File("/data/google-ngram-data"));
    val matches: List<RuleMatch> =
        langTool.check("A sentence with a error in the Hitchhiker's Guide tot he Galaxy")

    val t2 = System.nanoTime()

    // 2s for checking, 1.1s for loading
    // uses multiple cores
    println("${(t2 - t1) * 1e-9f}s for checking, ${(t1 - t0) * 1e-9f}s for loading")

    for (match in matches) {
        println("Potential error at characters ${match.fromPos}-${match.toPos}: ${match.message}")
        println("Suggested correction(s): ${match.suggestedReplacements}")
    }

}*/
