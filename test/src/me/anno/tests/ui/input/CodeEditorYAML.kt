package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.editor.code.tokenizer.YamlTokenizer

fun main() {
    OfficialExtensions.initForTests()
    val editor = CodeEditor(style)
    editor.setText(
        """
Buildings:
  CityAnchor:
    model: buildings/CityAnchor.json
    customColor0: LightGray
    customColor1: DarkGray
    cost:
      money: 10
    processes:
      - inputs: Money
        outputs: Concrete
      - inputs: Money
        outputs: Water
      - inputs: Money
        outputs: IronOre
      - inputs: Money
        outputs: Wool
      - inputs: Money
        outputs: Electricity
      - inputs: Sewage, Money
        outputs:
    fluidInputType: Sewage
    fluidOutputType: Water
        """.trimIndent()
    )
    editor.language = YamlTokenizer()
    testCodeEditor("Code Editor YAML", editor)
}