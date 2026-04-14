package me.anno.graph.visual.function

import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.engine.EngineBase
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.StartNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import org.apache.logging.log4j.LogManager

object FunctionGraphUtils {

    private val LOGGER = LogManager.getLogger(FunctionGraphUtils::class)

    data class Signature(
        val args: List<Pair<String, String>>,
        val returns: List<Pair<String, String>>,
    )

    data class LoadedTemplate(
        val graph: FlowGraph?,
        val signature: Signature,
    )

    private object TemplateCache : CacheSection<FileKey, LoadedTemplate>("FunctionGraphTemplates")

    var timeoutMillis = 60_000L

    fun getSignature(graph: FlowGraph): Signature {
        val start = graph.nodes.firstInstanceOrNull2(StartNode::class)
        val returnNode = graph.nodes.firstInstanceOrNull2(ReturnNode::class)
        val args = start?.outputs
            ?.map { it.type to it.name }
            ?: emptyList()
        val returns = returnNode?.inputs
            ?.map { it.type to it.name }
            ?: emptyList()
        return Signature(args, returns)
    }

    fun dropLeadingFlow(pairs: List<Pair<String, String>>): List<Pair<String, String>> {
        return if (pairs.firstOrNull()?.first == "Flow") pairs.drop(1) else pairs
    }

    fun dropAllFlow(pairs: List<Pair<String, String>>): List<Pair<String, String>> {
        return pairs.filter { it.first != "Flow" }
    }

    fun getTemplate(file: FileReference): LoadedTemplate? {
        if (file == InvalidRef) return null
        return TemplateCache.getFileEntry(file, false, timeoutMillis) { key, result ->
            result.value = try {
                val text = key.file.readTextSync()
                val loaded = JsonStringReader.read(text, EngineBase.workspace, key.file.absolutePath, true)
                    .firstOrNull() as? FlowGraph
                loaded?.let { fixGraphBackLinks(it) }
                val sig = if (loaded != null) getSignature(loaded) else Signature(emptyList(), emptyList())
                LoadedTemplate(loaded, sig)
            } catch (e: Exception) {
                LOGGER.warn("Failed to load graph ${key.file}", e)
                LoadedTemplate(null, Signature(emptyList(), emptyList()))
            }
        }.waitFor()
    }

    private fun fixGraphBackLinks(graph: FlowGraph) {
        for (node in graph.nodes) {
            node.graph = graph
        }
    }

    fun cloneForCall(template: FlowGraph): FlowGraph {
        @Suppress("unchecked_cast")
        return template.clone() as FlowGraph
    }

    fun setStartArguments(start: StartNode, args: List<Any?>) {
        // outputs[0] = flow
        val maxArgs = minOf(args.size, start.outputs.size - 1)
        for (i in 0 until maxArgs) {
            start.setOutput(i + 1, args[i])
        }
    }

    fun readReturnValues(returnNode: ReturnNode, count: Int): List<Any?> {
        val max = minOf(count, returnNode.inputs.size - 1)
        return (0 until max).map { i -> returnNode.getInput(i + 1) }
    }

    fun call(graph: FlowGraph, args: List<Any?>): Pair<ReturnNode?, Node?> {
        val start = graph.nodes.firstInstanceOrNull2(StartNode::class)
            ?: throw IllegalStateException("Missing StartNode")
        setStartArguments(start, args)
        val last = graph.execute(start)
        val returnNode = last as? ReturnNode ?: graph.nodes.firstInstanceOrNull2(ReturnNode::class)
        return returnNode to last
    }

    fun rebuildInputs(node: Node, newInputs: List<Pair<String, String>>) {
        val keep = node.inputs.firstOrNull()?.takeIf { it.type == "Flow" }?.let { 1 } ?: 0
        rebuildConnectors(
            node = node,
            old = node.inputs,
            keep = keep,
            newPairs = newInputs,
            create = { type, name -> NodeInput(type, name, node, false) },
        )
    }

    fun rebuildOutputs(node: Node, newOutputs: List<Pair<String, String>>) {
        val keep = node.outputs.firstOrNull()?.takeIf { it.type == "Flow" }?.let { 1 } ?: 0
        rebuildConnectors(
            node = node,
            old = node.outputs,
            keep = keep,
            newPairs = newOutputs,
            create = { type, name -> NodeOutput(type, name, node, false) },
        )
    }

    private fun <C : me.anno.graph.visual.node.NodeConnector> rebuildConnectors(
        node: Node,
        old: MutableList<C>,
        keep: Int,
        newPairs: List<Pair<String, String>>,
        create: (type: String, name: String) -> C,
    ) {
        // Ensure we have enough connector objects; reuse them to preserve connections.
        while (old.size < keep + newPairs.size) {
            val (type, name) = if (old.size >= keep) newPairs[old.size - keep] else "Any?" to "?"
            old.add(create(type, name))
        }

        // Update/reuse connectors.
        for (i in 0 until newPairs.size) {
            val idx = keep + i
            val (newType, newName) = newPairs[i]
            val con = old[idx]
            val oldType = con.type
            val oldWasFlow = oldType == "Flow"
            val newIsFlow = newType == "Flow"
            if (oldWasFlow != newIsFlow) {
                // only disconnect if Flow <-> non-Flow changed
                con.disconnectAll()
            }
            con.type = newType
            con.name = newName
        }

        // Drop extra connectors, disconnecting only if Flow-ness differs (but these are removed anyway).
        val desiredSize = keep + newPairs.size
        for (i in old.lastIndex downTo desiredSize) {
            old[i].disconnectAll()
            old.removeAt(i)
        }
    }

    fun splitSignaturePairs(flatTypeNamePairs: List<String>): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>(flatTypeNamePairs.size / 2)
        var i = 0
        while (i + 1 < flatTypeNamePairs.size) {
            out.add(flatTypeNamePairs[i] to flatTypeNamePairs[i + 1])
            i += 2
        }
        return out
    }

    fun connectorPairsToFlat(pairs: List<Pair<String, String>>): List<String> {
        val out = ArrayList<String>(pairs.size * 2)
        for ((t, n) in pairs) {
            out.add(t)
            out.add(n)
        }
        return out
    }

    fun extractInputPairs(node: FlowGraphNode): List<Pair<String, String>> {
        val keep = node.inputs.takeIf { it.isNotEmpty() && it[0].type == "Flow" }?.let { 1 } ?: 0
        return node.inputs.drop(keep).map { it.type to it.name }
    }

    fun extractOutputPairs(node: FlowGraphNode): List<Pair<String, String>> {
        val keep = node.outputs.takeIf { it.isNotEmpty() && it[0].type == "Flow" }?.let { 1 } ?: 0
        return node.outputs.drop(keep).map { it.type to it.name }
    }
}

