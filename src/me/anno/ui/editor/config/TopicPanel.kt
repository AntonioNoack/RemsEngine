package me.anno.ui.editor.config

import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style
import me.anno.utils.strings.StringHelper.camelCaseToTitle

class TopicPanel(val topic: String, topicName: String, val configPanel: ConfigPanel, style: Style) :
    TextPanel(topicName.camelCaseToTitle(), style) {
    val topicDepth = topic.count { char -> char == '.' }

    init {
        enableHoverColor = true
        padding.left += topicDepth * font.sizeInt
        addLeftClickListener {
            configPanel.createContent(topic)
        }
    }
}