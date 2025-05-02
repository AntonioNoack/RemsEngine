package me.anno.utils.files

import me.anno.config.ConfigRef
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.WindowManagement
import me.anno.gpu.OSWindow
import me.anno.io.files.FileReference
import me.anno.language.translation.DefaultNames
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.input.EnumInput
import me.anno.ui.input.InputPanel
import me.anno.ui.input.TextInput
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.types.Strings.isNotBlank2

object FileChooser {

    var openInSeparateWindow by ConfigRef("ui.fileChooser.openInSeparateWindow", true)

    @JvmStatic
    fun selectFiles(
        nameDesc: NameDesc, allowFiles: Boolean, allowFolders: Boolean,
        allowMultiples: Boolean, toSave: Boolean, startFolder: FileReference,
        filters: List<FileExtensionFilter>, callback: (List<FileReference>) -> Unit
    ) {
        if (!allowFiles && !allowFolders) {
            callback(emptyList())
            return
        }
        createFileChooser(
            nameDesc, allowFiles, allowFolders, allowMultiples, toSave,
            startFolder, filters, style, callback
        )
    }

    @JvmStatic
    private fun filterFiles(
        selected: List<FileReference>,
        allowFiles: Boolean,
        allowFolders: Boolean,
        allowMultiples: Boolean
    ): List<FileReference> {
        var selectedFiles = selected
        if (!allowFiles || !allowFolders) {
            selectedFiles = selectedFiles.filter {
                if (it.isDirectory) allowFolders else allowFiles
            }
        }
        if (selectedFiles.size > 1 && !allowMultiples) {
            selectedFiles = listOf(selectedFiles.first())
        }
        return selectedFiles
    }

    @JvmStatic
    private fun updateFileListText(selected: List<FileReference>, filesList: Panel, submit: InputPanel<*>) {
        if (selected.isNotEmpty()) {
            val text = if (selected.size == 1) {
                selected.first().toLocalPath()
            } else {
                "${selected.first().getParent().toLocalPath()}: " +
                        selected.joinToString("; ") { it.name }
            }
            when (filesList) {
                is TextInput -> filesList.setValue(text, false)
                is TextPanel -> filesList.text = text
            }
            submit.isInputAllowed = true
        }
    }

    @JvmStatic
    fun createFileChooserUI(
        allowFiles: Boolean, allowFolders: Boolean,
        allowMultiples: Boolean, toSave: Boolean,
        startDirectory: FileReference,
        filters: List<FileExtensionFilter>,
        style: Style, callback: (List<FileReference>) -> Unit
    ): Panel {

        val cancel = TextButton(DefaultNames.cancel, style)
        val submit = TextButton(DefaultNames.select, style)
        submit.isInputAllowed = allowFolders

        val selected = ArrayList<FileReference>()
        if (allowFolders) selected.add(startDirectory)

        val filesList = if (toSave) {
            // if toSave, add a bar to enter a new name
            val filesList = TextInput(style)
            filesList.addChangeListener { string ->
                // change list of selected ones...
                selected.clear()
                val words = string.split("; ")
                selected.ensureCapacity(words.size)
                for (i in words.indices) {
                    val word = words[i]
                    if (word.isNotBlank2()) {
                        val file = word.trim().toGlobalFile()
                        selected.add(file)
                    }
                }
                submit.isInputAllowed = selected.isNotEmpty()
            }
            filesList.alignmentX = AxisAlignment.FILL
            filesList.base.enableSpellcheck = false
            filesList
        } else {
            TextPanel(
                if (allowMultiples) "No files selected"
                else "No file selected", style
            )
        }

        updateFileListText(selected, filesList, submit)

        val extensions = ArrayList<String>()
        val files = object : FileExplorer(startDirectory, true, style) {
            override fun shouldShowFile(file: FileReference): Boolean {
                return extensions.isEmpty() || file.lcExtension in extensions || file.isDirectory
            }

            override fun onUpdate() {
                super.onUpdate()
                val selectedList = filterFiles(content2d.children
                    .filterIsInstance<FileExplorerEntry>()
                    .filter { it.isInFocus }.map { it.ref1s },
                    allowFiles, allowFolders, allowMultiples
                )
                if (selectedList.isNotEmpty() && selectedList != selected) {
                    selected.clear()
                    selected.addAll(selectedList)
                    updateFileListText(selected, filesList, submit)
                    submit.isInputAllowed = true
                }
            }
        }
        files.fill(1f)

        val buttons = PanelListX(style)
        cancel.fill(1f)
        submit.fill(1f)
        cancel.addLeftClickListener(Menu::close)
        submit.addLeftClickListener {
            callback(selected)
            Menu.close(it)
        }
        buttons.fill(0f)
        buttons.add(cancel)
        buttons.add(submit)
        val ui = PanelListY(style)
        if (filters.isNotEmpty()) {
            val chosenByDefault = filters.first()
            if (filters.size > 1) {
                val select = EnumInput(NameDesc("Filter"), chosenByDefault.nameDesc, filters.map { it.nameDesc }, style)
                select.setChangeListener { _, index, _ ->
                    applyFilter(filters[index], extensions, files)
                }
                ui.add(select)
            } // else there isn't really any choice
            applyFilter(chosenByDefault, extensions, files)
        }
        ui.add(files)
        ui.add(filesList)
        ui.add(buttons)
        return ui
    }

    @JvmStatic
    private fun applyFilter(
        filter: FileExtensionFilter,
        extensions: ArrayList<String>,
        files: FileExplorer
    ) {
        extensions.clear()
        extensions.addAll(filter.extensions)
        files.invalidate(force = true)
    }

    @JvmStatic
    private fun createFileChooser(
        nameDesc: NameDesc,
        allowFiles: Boolean, allowDirectories: Boolean,
        allowMultiples: Boolean, toSave: Boolean,
        startDirectory: FileReference,
        filters: List<FileExtensionFilter>,
        style: Style, callback: (List<FileReference>) -> Unit
    ): Window {
        val ui = createFileChooserUI(
            allowFiles, allowDirectories, allowMultiples,
            toSave, startDirectory, filters, style,
            callback
        )
        val baseWindow = GFX.someWindow
        return if (openInSeparateWindow) {
            // make this window a little smaller than default, so it's more obvious
            val w = OSWindow.defaultWidth * 6 / 7
            val h = OSWindow.defaultHeight * 5 / 7
            val window = WindowManagement.createWindow(nameDesc.name, ui, w, h)
            window.showFPS = false
            window.windowStack.last()
        } else {
            baseWindow.windowStack.push(ui)
        }
    }
}