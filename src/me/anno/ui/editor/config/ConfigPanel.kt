package me.anno.ui.editor.config

import me.anno.io.utils.StringMap
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.openMenuByPanels
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomList
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.types.Strings.isBlank2
import java.util.*
import kotlin.math.max

// todo allow fields to be added
class ConfigPanel(val config: StringMap, val isStyle: Boolean, style: Style) : PanelListY(style) {

    val deep = style.getChild("deep")
    val searchBar = PanelListX(deep)

    val mainBox = CustomList(false, style)
    val topicTree = PanelListY(style)
    val contentListUI = PanelListY(style)
    val contentList = ArrayList<Pair<String, Panel>>()

    val searchInput = TextInput(Dict["Search", "ui.general.search"], "", false, deep)

    fun create() {
        createTopics()
        if (topicTree.children.isNotEmpty()) {
            val tp = topicTree.children.first() as TopicPanel
            createContent(tp.topic)
        }
        fun add(panel: Panel, weight: Float) {
            mainBox.add(ScrollPanelXY(panel.withPadding(5, 5, 5, 5), style), weight)
        }
        add(topicTree, 1f)
        add(contentListUI, 3f)
        searchBar += TextButton(Dict["Close", "ui.general.close"], false, deep)
            .addLeftClickListener { windowStack.pop().destroy() }
        searchBar += TextButton(Dict["Add Field", "ui.general.addField"], false, deep)
            .addLeftClickListener {
                val keyPanel = TextInput("Key", "", lastTopic, style)
                val valuePanel = TextInput("Value", "", "", style)
                val submit = TextButton("Set", false, style)
                // todo we also need a way to delete fields
                submit.addLeftClickListener {
                    if (keyPanel.value.isNotBlank()) {
                        config[keyPanel.value.trim()] = valuePanel.value
                        createContent(lastNotEmptyTopic)
                    }
                    Menu.close(keyPanel)
                }
                val cancel = TextButton("Cancel", false, style)
                cancel.addLeftClickListener { Menu.close(keyPanel) }
                val buttons = PanelListX(style)
                buttons += cancel
                buttons += submit
                openMenuByPanels(
                    windowStack, NameDesc("New Value"),
                    listOf(keyPanel, valuePanel, buttons)
                )
            }
        if (isStyle) {
            searchBar += TextButton(Dict["Apply", "ui.general.apply"], false, deep).addLeftClickListener {
                createTopics()
                lastTopic = "-"
                applySearch(searchInput.value)
            }
        }
        searchBar += searchInput.apply {
            addChangeListener { query -> applySearch(query) }
            weight = 1f
        }
        this += mainBox
        this += searchBar
    }

    private fun applySearch(query: String) {

        if (query.isBlank2()) {
            createContent(lastNotEmptyTopic)
        } else {

            val queryTerms = query
                .replace('\t', ',')
                .replace(' ', ',')
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.lowercase(Locale.getDefault()) }

            if (lastTopic.isNotEmpty()) createContent("")
            for ((name, ui) in contentList) {
                ui.isVisible = queryTerms.all { it in name }
            }
        }
    }

    var lastTopic = ""
    var lastNotEmptyTopic = ""
    private fun createTopics() {
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
        for (topic in topics.sortedBy { it.lowercase(Locale.getDefault()) }) {
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

        lastTopic = topic
        if (topic.isNotEmpty()) lastNotEmptyTopic = topic

        contentListUI.clear()
        contentList.clear()

        val pattern = if (topic.isEmpty()) "" else "$topic."
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

        val largeHeaderText = style.getChild("header")
        val smallHeaderStyle = style.getChild("header.small")

        val topList = entries.filter { it.depth == 0 }

        // add header
        val headerTitle = topic.camelCaseToTitle()
        val largeHeader2 = TextPanel(headerTitle, largeHeaderText).apply { font = font.withItalic(true) }
        contentListUI += largeHeader2
        val subChain = StringBuilder(topList.size * 2)
        for (entry in topList) {
            val subList = PanelListY(style)
            subList.tooltip = entry.fullName
            entry.createPanels(subList)
            val searchKey = entry.fullName.lowercase(Locale.getDefault())
            contentList += searchKey to subList
            contentListUI += subList
            subChain.append(searchKey)
            subChain.append(' ')
        }
        contentList += subChain.toString() to largeHeader2


        // add sub headers for all topics...
        val groups = entries
            .filter { it.depth > 0 }
            .groupBy { it.groupName }

        for (group in groups.entries.sortedBy { it.key }) {
            val groupName = group.key
            val title = groupName.camelCaseToTitle()
            val smallHeader = TextPanel(title, smallHeaderStyle)
            contentListUI += smallHeader
            val subChain2 = StringBuilder(group.value.size * 2)
            for (entry in group.value) {
                val subList = PanelListY(style)
                subList.tooltip = entry.fullName
                entry.createPanels(subList)
                val searchKey = entry.fullName.lowercase(Locale.getDefault())
                contentList += searchKey to subList
                contentListUI += subList
                subChain2.append(searchKey)
                subChain2.append(' ')
            }
            contentList += subChain2.toString() to smallHeader
        }

        invalidateLayout()
    }
}