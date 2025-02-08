package me.anno.ui.editor.config

class TopicNode(val key: String, val title: String) {
    val sortKey = key.lowercase()
    var parent: TopicNode? = null
    val children = ArrayList<TopicNode>()
    var isCollapsed =false
}