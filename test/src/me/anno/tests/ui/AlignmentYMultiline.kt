package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.language.translation.NameDesc
import me.anno.ui.UIColors
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    val text = TextButton(
        NameDesc(
            "Hover me!", "This is a very long line that should break at some point, doesn't it? Bye! " +
                    "What do you think about it? Can we get a third line?", ""
        ), style
    )
    text.alignmentX = AxisAlignment.CENTER
    text.alignmentY = AxisAlignment.CENTER
    text.backgroundColor = UIColors.blueishGray
    // todo we need sth like line-alignment for automatic line breaks
    text.textAlignmentX = AxisAlignment.MAX
    val p0 = PanelContainer(text, Padding(10), style)
    p0.alignmentX = AxisAlignment.CENTER
    p0.alignmentY = AxisAlignment.CENTER
    p0.backgroundColor = UIColors.midOrange
    val p1 = PanelContainer(p0, Padding(10), style)
    testUI3("AlignmentY Multiline", p1)
}