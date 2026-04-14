package me.anno.graph.visual.function

import me.anno.graph.visual.actions.ActionNode
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.editor.graph.GraphPanel
import me.anno.ui.input.FileInput

/**
 * A node, which calls an action graph.
 * todo test using these & saving & loading graphs from files...
 * */
class ActionGraphNode : ActionNode {

    @Suppress("unused")
    constructor() : super("Action Graph")
    constructor(name: String) : super(name)

    var file: FileReference = InvalidRef
    private var lastSignature: FunctionGraphUtils.Signature? = null

    private fun ensureTemplateUpToDate() {
        val tpl = FunctionGraphUtils.getTemplate(file) ?: return
        val sig = tpl.signature
        if (lastSignature != sig) {
            lastSignature = sig
            FunctionGraphUtils.rebuildInputs(this, FunctionGraphUtils.dropLeadingFlow(sig.args))
            FunctionGraphUtils.rebuildOutputs(this, FunctionGraphUtils.dropLeadingFlow(sig.returns))
        }
    }

    override fun executeAction() {
        ensureTemplateUpToDate()
        val template = FunctionGraphUtils.getTemplate(file)?.graph
            ?: throw IllegalStateException("Missing graph template for $file")
        val instance = FunctionGraphUtils.cloneForCall(template)
        val args = List(FunctionGraphUtils.extractInputPairs(this).size) { index -> getInput(index + 1) }
        val (retNode, _) = FunctionGraphUtils.call(instance, args)
        if (retNode != null) {
            val outPairs = FunctionGraphUtils.extractOutputPairs(this)
            val values = FunctionGraphUtils.readReturnValues(retNode, outPairs.size)
            for (i in values.indices) {
                setOutput(i + 1, values[i])
            }
        }
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        list += FileInput(NameDesc.EMPTY, style, file, emptyList())
            .addChangeListener {
                file = it
                lastSignature = null
                ensureTemplateUpToDate()
                g.onChange(false)
            }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "file" -> {
                file = value as? FileReference ?: InvalidRef
                lastSignature = null
            }
            else -> super.setProperty(name, value)
        }
    }
}