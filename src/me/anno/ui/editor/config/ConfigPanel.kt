package me.anno.ui.editor.config

import me.anno.io.utils.StringMap
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.custom.CustomListX
import me.anno.ui.style.Style
import kotlin.math.max

// todo allow fields to be added
// todo add style section...
class ConfigPanel(val config: StringMap, style: Style) : CustomListX(style) {

    // todo reload ui when sth was changed?...

    // todo search bar for all
    // todo close-button

    val deep = style.getChild("deep")
    val topicTree = PanelListY(style)
    val contentList = PanelListY(style)

    fun create() {
        createTopics()
        if (topicTree.children.isNotEmpty()) {
            val tp = topicTree.children.first() as TopicPanel
            createContent(tp.topic)
        }
        fun add(panel: Panel, weight: Float) {
            this.add(ScrollPanelXY(panel.withPadding(5, 5, 5, 5), style), weight)
        }
        add(topicTree, 1f)
        add(contentList, 3f)
    }

    fun createTopics() {
        topicTree.clear()
        // concat stuff, that has only one entry?
        // descriptions???...
        val topics = config.keys.map {
            val lastIndex = it.lastIndexOf('.')
            it.substring(0, max(0, lastIndex))
        }.toHashSet()
        for (i in 0 until 5) {
            topics.addAll(
                topics.map {
                    val lastIndex = it.lastIndexOf('.')
                    it.substring(0, max(0, lastIndex))
                }
            )
        }
        for (topic in topics.sortedBy { it.toLowerCase() }) {
            if (topic.isNotEmpty()) {
                val lastIndex = topic.lastIndexOf('.')
                val topicName = topic.substring(lastIndex + 1)
                val panel = TopicPanel(topic, topicName, this, style)
                if (panel.topicDepth > 0) panel.hide()
                topicTree += panel
                /*panel.setSimpleClickListener {// show that there is more? change the name?
                    val start = "$topic."
                    val special = Input.isShiftDown || Input.isControlDown
                    val visible = if(special) Visibility.GONE else Visibility.VISIBLE
                    if(!special) createContent(topic, topicName)
                    topicTree.children
                        .filterIsInstance<TopicPanel>()
                        .forEach {
                            if(it.topic.startsWith(start)){
                                it.visibility = visible
                            }
                        }
                }*/
            }
        }
        invalidateLayout()
    }

    fun createContent(topic: String) {

        contentList.clear()

        val pattern = "$topic."
        val entries = config.entries
            .filter { it.value !is StringMap }
            .filter { it.key.startsWith(pattern) }
            .map {
                val fullName = it.key
                val relevantName = fullName.substring(pattern.length)
                val depth = relevantName.count { char -> char == '.' }
                val li = relevantName.lastIndexOf('.') + 1
                val key = relevantName.substring(0, max(0, li - 1))
                val shortName = relevantName.substring(li)
                ContentCreator(fullName, relevantName, depth, key, shortName, config)
            }
            .sortedBy { it.relevantName }

        val largeHeader = style.getChild("header")
        val smallHeader = style.getChild("header.small")

        val topList = entries.filter { it.depth == 0 }
        // add header
        contentList += TextPanel(topic, largeHeader).apply {
            font.isItalic = true
        }
        for (top in topList) {
            top.createPanels(contentList)
        }

        // add sub headers for all topics...
        val groups = entries
            .filter { it.depth > 0 }
            .groupBy { it.groupName }
        for (group in groups.entries.sortedBy { it.key }) {
            val groupName = group.key
            contentList += TextPanel(groupName, smallHeader)
            for (entry in group.value) {
                entry.createPanels(contentList)
            }
        }
        invalidateLayout()
    }

}