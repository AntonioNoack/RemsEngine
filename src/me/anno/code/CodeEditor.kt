package me.anno.code

import me.anno.config.DefaultConfig.style
import me.anno.gpu.Window
import me.anno.studio.StudioBase
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.custom.CustomListX
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.input.components.PureTextInputML
import me.anno.utils.OS
import me.anno.utils.listFiles2
import java.io.File

/**
 * Intellij Ideas support for JS is bad, so I want to give it a try to use my video studio code,
 * slightly altered, as a code editor
 * */
object CodeEditor : StudioBase(false) {

    var projectFolder = File(OS.documents, "IdeaProjects\\JSTest\\src")
    var currentTab: CodeTab? = null

    lateinit var tabs: PanelList

    fun openFile(file: File){
        if(tabs.children.all { (it as CodeTab).file != file }){
            tabs.add(CodeTab(file, style))
        }
        currentTab = tabs.children.filterIsInstance<CodeTab>().first { it.file == file }

    }

    override fun onGameLoopStart() {}
    override fun onGameLoopEnd() {}
    override fun onProgramExit() {}

    override fun createUI(){

        val style = style
        val main = PanelListY(style)

        tabs = PanelListX(style)
        projectFolder.listFiles2(false).forEach {
            if(!it.isDirectory){
                when(it.extension.toLowerCase()){
                    "html", "js" -> {
                        openFile(it)
                    }
                }
            }
        }
        // todo open all last files...?
        main += ScrollPanelX(tabs, Padding(0), style, AxisAlignment.MIN)

        // todo tree view for files? normal file explorer?

        val container = CustomListX(style)
        main += container

        val files = FileExplorer(style)
        files.folder = projectFolder
        files.weight = 1f
        container += files

        val code = PureTextInputML(style)
        code.weight = 2f
        container += code

        main += createConsole()

        windowStack.clear()
        windowStack.push(Window(main))

    }

    @JvmStatic
    fun main(args: Array<String>){
        run()
    }

}