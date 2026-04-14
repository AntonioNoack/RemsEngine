package me.anno.graph.visual.function

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.CalculationNode
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.editor.graph.GraphPanel
import me.anno.ui.input.FileInput
import org.apache.logging.log4j.LogManager

class ExpressionGraphNode : CalculationNode {

    companion object {
        private val LOGGER = LogManager.getLogger(ExpressionGraphNode::class)
    }

    @Suppress("unused")
    constructor() : super("Expression Graph", emptyList(), listOf("Any?", "Result"))

    constructor(name: String) : super(name, emptyList(), listOf("Any?", "Result"))

    var file: FileReference = InvalidRef
    private var lastSignature: FunctionGraphUtils.Signature? = null

    private fun ensureTemplateUpToDate() {
        val tpl = FunctionGraphUtils.getTemplate(file) ?: return
        val sig = tpl.signature
        if (lastSignature != sig) {
            lastSignature = sig
            updatePortsFromSignature(sig)
        }
    }

    private fun updatePortsFromSignature(sig: FunctionGraphUtils.Signature) {
        FunctionGraphUtils.rebuildInputs(this, FunctionGraphUtils.dropAllFlow(sig.args))
        FunctionGraphUtils.rebuildOutputs(this, FunctionGraphUtils.dropAllFlow(sig.returns))
        if (outputs.isEmpty()) FunctionGraphUtils.rebuildOutputs(this, listOf("Any?" to "Result"))
    }

    override fun calculate(): Any? {
        ensureTemplateUpToDate()
        val template = FunctionGraphUtils.getTemplate(file)?.graph
            ?: throw IllegalStateException("Missing graph template for $file")
        val instance = FunctionGraphUtils.cloneForCall(template)
        val args = inputs.mapIndexed { index, _ -> getInput(index) }
        val (retNode, _) = FunctionGraphUtils.call(instance, args)
        val retCount = outputs.size
        if (retNode != null && retCount > 0) {
            val values = FunctionGraphUtils.readReturnValues(retNode, retCount)
            // store all outputs, return first for CalculationNode API users
            for (i in values.indices) {
                setOutput(i, values[i])
            }
            return values.firstOrNull()
        }
        return null
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