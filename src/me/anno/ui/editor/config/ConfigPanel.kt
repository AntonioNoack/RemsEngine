package me.anno.ui.editor.config

import me.anno.config.DefaultStyle
import me.anno.io.utils.StringMap
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.Search
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelContainer.Companion.withPadding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.openMenuByPanels
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.FPSPanelSpacer
import me.anno.ui.input.TextInput
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.isNotBlank2
import kotlin.math.max
import kotlin.math.min

/**
 * UI for editing the base configuration and style
 * */
class ConfigPanel(val config: StringMap, val type: ConfigType, style: Style) : PanelListY(style) {

    val deep = style.getChild("deep")
    val bottomBar = PanelListX(deep)

    val body = CustomList(false, style)
    val left = PanelListY(style)

    val rootNode = TopicNode("", "")
    val leftContent = TopicTree(this, style)

    val right = PanelListY(style)

    val contentList = ArrayList<Pair<List<String>, Panel>>()

    val largeHeaderStyle = style.getChild("header")
    val smallHeaderStyle = style.getChild("header.small")

    private val searchInput = TextInput(NameDesc("Search Term", "", "ui.general.search"), "", false, deep)
        .addChangeListener { query -> applySearch(query) }
    private val closeButton = TextButton(NameDesc("Close", "", "ui.general.close"), deep)
        .addLeftClickListener(Menu::close)
    private val addFieldButton = TextButton(NameDesc("Add Field", "", "ui.general.addField"), deep)
        .addLeftClickListener { openNewPropertyMenu() }
    private val applyButton = TextButton(NameDesc("Apply", "", "ui.general.apply"), deep)
        .addLeftClickListener { showChanges() }
    private val clearAllButton = TextButton(NameDesc("Reset All"), deep)
        .addLeftClickListener {
            ask(it.windowStack, NameDesc("This will reset all custom settings. Are you sure?")) {
                resetAll()
            }
        }

    private fun openNewPropertyMenu() {
        val keyPanel = TextInput(NameDesc("Key"), "", lastTopic?.title, style)
        val valuePanel = TextInput(NameDesc("Value"), "", "", style)
        val submit = TextButton(NameDesc("Set"), style)
        submit.addLeftClickListener {
            if (keyPanel.value.isNotBlank2()) {
                config[keyPanel.value.trim()] = valuePanel.value
                buildSettingsForTopic(lastNotEmptyTopic)
            }
            Menu.close(keyPanel)
        }
        val cancel = TextButton(NameDesc("Cancel"), false, style)
        cancel.addLeftClickListener { Menu.close(keyPanel) }
        val buttons = PanelListX(style)
        buttons += cancel
        buttons += submit
        openMenuByPanels(
            windowStack, NameDesc("New Value"),
            listOf(keyPanel, valuePanel, buttons)
        )
    }

    private fun resetAll() {
        config.clear()
        if (type == ConfigType.STYLE) DefaultStyle.initDefaults()
        showChanges()
    }

    private fun addToBody(panel: Panel, weight: Float) {
        body.add(panel.withPadding(5, 5, 5, 5), weight)
    }

    private fun createRightSideDefault() {
        buildSettingsForTopic(rootNode.children.firstOrNull())
    }

    private fun showChanges() {
        createLeftSide()
        lastTopic = null // idk what we need this line for
        applySearch(searchInput.value)
    }

    init {
        addToBody(left, 1f)
        addToBody(ScrollPanelXY(right, style), 3f)
        left.add(searchInput)
        left.add(leftContent.fill(1f))

        createLeftSide()
        createRightSideDefault()

        fillBottomBar()

        add(body)
        add(bottomBar)
    }

    private fun fillBottomBar() {
        bottomBar.add(closeButton.fill(1f))
        bottomBar.add(addFieldButton.fill(1f))
        if (type == ConfigType.STYLE) bottomBar.add(applyButton.fill(1f))
        bottomBar.add(clearAllButton.fill(1f))
        bottomBar.add(FPSPanelSpacer(style)) // todo better: draw fps transparently
    }

    private fun applySearch(userQuery: String) {
        if (userQuery.isBlank2()) {
            buildSettingsForTopic(lastNotEmptyTopic)
        } else {
            val search = Search(userQuery)
            if (lastTopic != null) buildSettingsForTopic(null)
            for ((name, ui) in contentList) {
                ui.isVisible = search.matches(name)
            }
        }
    }

    var lastTopic: TopicNode? = null
    var lastNotEmptyTopic: TopicNode? = null
    private fun createLeftSide() {
        buildTopicTree()
    }

    fun buildTopicTree() {
        val knownNodes = HashMap<String, TopicNode>()
        knownNodes[rootNode.key] = rootNode
        rootNode.children.clear()

        for ((key, value) in config) {
            if (value is StringMap) continue
            val list = key.split('.')
            var parent: TopicNode = rootNode
            val maxLevels =
                if (type == ConfigType.KEYMAP) min(2, list.lastIndex)
                else list.lastIndex
            for (len in 1 until maxLevels) {
                val topic = list.subList(0, len).joinToString(".")
                parent = knownNodes.getOrPut(topic) {
                    val topicName = list[len - 1].camelCaseToTitle()
                    val newNode = TopicNode(topic, topicName)
                    parent.children.add(newNode)
                    newNode.parent = parent
                    newNode
                }
            }
        }

        for (node in knownNodes.values) {
            node.children.sortBy { it.sortKey }
            node.isCollapsed = node.children.size > 1 && node.key.count { it == '.' } > 1
        }
    }

    fun buildSettingsForTopic(topic: TopicNode?) {

        // todo if type is KeyMap, create an appropriate different layout!

        lastTopic = topic
        if (topic != null) lastNotEmptyTopic = topic

        right.clear()
        contentList.clear()

        val pattern = if (topic == null) "" else "${topic.key}."
        val entries = config.entries
            .filter { (key, value) -> value !is StringMap && key.startsWith(pattern) }
            .map { (fullName, _) ->
                val relevantName = fullName.substring(pattern.length)
                val depth = relevantName.count { char -> char == '.' }
                val li = relevantName.lastIndexOf('.') + 1
                val key = relevantName.substring(0, max(0, li - 1))
                val shortName = relevantName.substring(li)
                ContentCreator(fullName, relevantName, depth, key, shortName, config)
            }
            .sortedBy { it.relevantName }

        val topList = entries.filter { it.depth == 0 }

        // add header
        val headerTitle = topic?.title ?: "..."
        val largeHeader2 = TextPanel(headerTitle, largeHeaderStyle)
        processEntries(topList, largeHeader2)

        // add sub headers for all topics...
        val groups = entries
            .filter { it.depth > 0 }
            .groupBy { it.groupName }

        for (group in groups.entries.sortedBy { it.key }) {
            processEntries(group.key, group.value)
        }
    }

    private fun processEntries(groupName: String, entries: List<ContentCreator>) {
        val title = groupName.camelCaseToTitle()
        val smallHeader = TextPanel(title, smallHeaderStyle)
        processEntries(entries, smallHeader)
    }

    private fun processEntries(entries: List<ContentCreator>, smallHeader: TextPanel) {
        right.add(smallHeader)
        val subChain = ArrayList<String>()
        for (entry in entries) {
            processEntry(entry, subChain)
        }
        contentList.add(subChain to smallHeader)
    }

    private fun processEntry(entry: ContentCreator, subChain2: ArrayList<String>) {
        val subList = PanelListY(style)
        subList.tooltip = entry.fullName
        entry.createPanels(subList)
        val searchKey = entry.fullName.lowercase()
        contentList.add(listOf(searchKey) to subList)
        right.add(subList)
        subChain2.add(searchKey)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "BeginSearch" -> searchInput.requestFocus()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }
}