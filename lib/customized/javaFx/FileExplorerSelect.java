package me.anno.utils.files;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static javafx.application.Platform.setImplicitExit;

@SuppressWarnings("unused")
public class FileExplorerSelect extends Application {

	private static boolean isDirectory;
	private static File initial;
	private static Function1<List<File>, Unit> callback;
	private static boolean isLaunched = false;
	private static Stage stage;
	private static javafx.scene.image.Image image;
	private static boolean firstRequest = true;
	private static boolean allowMultiples = false, toSave = false;

	private static String[][] extensionFilters; // first one is title, others are *.extensions

	public static void select(
			boolean allowFiles, boolean allowFolders, boolean allowMultiples,
			File initial, boolean toSave, String[][] extensionFilters, Function1<List<File>, Unit> callback
	) {
		me.anno.utils.files.FileExplorerSelect.initial = initial == null || initial.isDirectory() ? initial : initial.getParentFile();
		me.anno.utils.files.FileExplorerSelect.callback = callback;
		me.anno.utils.files.FileExplorerSelect.extensionFilters = extensionFilters;
		me.anno.utils.files.FileExplorerSelect.toSave = toSave;
		me.anno.utils.files.FileExplorerSelect.allowMultiples = allowMultiples;
		if (!isLaunched) {
			isLaunched = true;
			launch();
		} else {
			isDirectory = allowFolders && !allowFiles;
			Platform.runLater(me.anno.utils.files.FileExplorerSelect::select);
		}
	}

	@Override
	public void start(Stage stage) {

		try {
			InputStream stream = me.anno.utils.files.FileExplorerSelect.class.getClassLoader().getResourceAsStream("icon.png");
			if (stream == null) throw new FileNotFoundException("icon.png");
			image = new javafx.scene.image.Image(stream);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (image != null) stage.getIcons().add(image);

		setImplicitExit(false);
		me.anno.utils.files.FileExplorerSelect.stage = stage;
		select();
	}

	private static void setIcon() {
		/*
		 * hack to set the icon without an actual JavaFX window
		 * https://stackoverflow.com/a/65320694/4979303
		 * */
		if (firstRequest) {
			if (image != null) stage.getIcons().add(image);
			stage.initStyle(StageStyle.UNDECORATED);
			stage.setTitle("Select Files");
		}
	}

	private static void initStage() {
		setIcon();
		stage.show();
	}

	private static void finishStage() {
		stage.hide();
		// stage.close();
		firstRequest = false;
	}

	private static void select() {
		initStage();
		List<File> selected = isDirectory ?
				selectFolder() :
				selectFile();
		if (selected == null) selected = Collections.emptyList();
		callback.invoke(selected);
		finishStage();
	}

	private static List<File> selectFile() {
		FileChooser fileChooser = new FileChooser();
		if (initial != null) fileChooser.setInitialDirectory(initial);
		for (String[] filter : extensionFilters) fileChooser.getExtensionFilters().add(createFilter(filter));
		if (allowMultiples) return fileChooser.showOpenMultipleDialog(stage);
		File selectedFile = toSave ? fileChooser.showSaveDialog(stage) : fileChooser.showOpenDialog(stage);
		return selectedFile == null ? null : Collections.singletonList(selectedFile);
	}

	private static FileChooser.ExtensionFilter createFilter(String[] values) {
		String[] extensions = Arrays.copyOfRange(values, 1, values.length);
		for (int i = 0; i < extensions.length; i++) extensions[i] = "*." + extensions[i];
		return new FileChooser.ExtensionFilter(values[0], extensions);
	}

	private static List<File> selectFolder() {
		DirectoryChooser fileChooser = new DirectoryChooser();
		if (initial != null) fileChooser.setInitialDirectory(initial);
		File selectedFile = fileChooser.showDialog(stage);
		return Collections.singletonList(selectedFile);
	}

}