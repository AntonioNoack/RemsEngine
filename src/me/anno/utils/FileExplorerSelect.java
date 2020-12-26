package me.anno.utils;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.io.File;
import java.io.IOException;

import static javafx.application.Platform.setImplicitExit;

public class FileExplorerSelect extends Application {

    /**
     * directoryChooser.getExtensionFilters().addAll(
     *     new ExtensionFilter("Text Files", "*.txt"),
     *     new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
     *     new ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
     *     new ExtensionFilter("All Files", "*.*"));
     * */

    private static boolean isDirectory;
    private static File initial;
    private static Function1<File, Unit> callback;
    private static boolean isLaunched = false;
    private static Stage stage;
    private static javafx.scene.image.Image image;
    private static boolean firstRequest = true;

    public static void selectFile(File initial, Function1<File, Unit> callback) {
        selectFileOrFolder(initial, false, callback);
    }

    public static void selectFolder(File initial, Function1<File, Unit> callback) {
        selectFileOrFolder(initial, true, callback);
    }

    public static void selectFileOrFolder(File initial, boolean isDirectory, Function1<File, Unit> callback) {
        FileExplorerSelect.initial = initial;
        FileExplorerSelect.callback = callback;
        if (!isLaunched) {
            isLaunched = true;
            launch();
        } else {
            FileExplorerSelect.isDirectory = isDirectory;
            if(isDirectory){
                selectFolder(true);
            } else {
                selectFile(true);
            }
        }
    }

    @Override
    public void start(Stage stage) {

        try {
            image = new javafx.scene.image.Image(ResourceHelper.INSTANCE.loadResource("icon.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (image != null) stage.getIcons().add(image);

        setImplicitExit(false);
        FileExplorerSelect.stage = stage;

        if(isDirectory){
            selectFolder(false);
        } else {
            selectFile(false);
        }

    }

    private static void selectFile(boolean runLater) {
        if (runLater) {
            Platform.runLater(() -> selectFile(false));
        } else {
            /*
             * hack to set the icon without an actual JavaFX window
             * https://stackoverflow.com/a/65320694/4979303
             * */
            if (firstRequest) {
                if (image != null) stage.getIcons().add(image);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setTitle("Select File");
            }
            stage.show();
            FileChooser fileChooser = new FileChooser();
            if (initial != null) {
                fileChooser.setInitialDirectory(
                        initial.isDirectory() ?
                                initial :
                                initial.getParentFile()
                );
            }
            File selectedFile = fileChooser.showOpenDialog(stage);
            callback.invoke(selectedFile);
            stage.hide();
            // stage.close();
            firstRequest = false;
        }
    }

    private static void selectFolder(boolean runLater) {
        if (runLater) {
            Platform.runLater(() -> selectFolder(false));
        } else {
            /*
             * hack to set the icon without an actual JavaFX window
             * https://stackoverflow.com/a/65320694/4979303
             * */
            if (firstRequest) {
                if (image != null) stage.getIcons().add(image);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setTitle("Select File");
            }
            stage.show();
            DirectoryChooser fileChooser = new DirectoryChooser();
            if (initial != null) {
                fileChooser.setInitialDirectory(
                        initial.isDirectory() ?
                                initial :
                                initial.getParentFile()
                );
            }
            File selectedFile = fileChooser.showDialog(stage);
            callback.invoke(selectedFile);
            stage.hide();
            // stage.close();
            firstRequest = false;
        }
    }

}