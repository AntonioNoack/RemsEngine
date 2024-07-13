package me.anno.ui.editor.code

import me.anno.utils.structures.Compare.ifSame

class CodeBlock(val start: Int, val count: Int) : Comparable<CodeBlock> {
    override fun compareTo(other: CodeBlock): Int {
        // early blocks first; large blocks first
        return start.compareTo(other.start).ifSame(other.count.compareTo(count))
    }
}