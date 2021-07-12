package me.anno.io.windows;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class WindowsShortcutMain {

    public static void main(String[] args) throws IOException, ParseException {
        String[] filenames = {
               /* "src/test/fixtures/Local file.lnk",
                "src/test/fixtures/Local folder.lnk",
                "src/test/fixtures/Remote folder.lnk",
                "src/test/fixtures/Remote folder (mapped to X-drive).lnk",
                "src/test/fixtures/Hazarsolve Eduction Tubes II.pdf - Shortcut.lnk",
                "src/test/fixtures/HSBD-10AS Instructions.pdf - Shortcut.lnk",
                "src/test/fixtures/HSBD-50E Instructions.pdf - Shortcut.lnk",
                "src/test/fixtures/test.pdf - Shortcut.lnk",*/
                "C:/Users/Antonio/Desktop/Eclipse.lnk"
        };
        for (String filename : filenames) {
            printLink(filename);
        }
    }

    public static void printLink(String filename) throws IOException, ParseException {

        File file = new File(filename);
        WindowsShortcut link = new WindowsShortcut(file);

        System.out.printf("-------%s------ \n", filename);
        System.out.printf("getRealFilename: %s \n", link.getRealFilename());
        System.out.printf("getDescription: %s \n", link.getDescription());
        System.out.printf("getRelativePath: %s \n", link.getRelativePath());
        System.out.printf("getWorkingDirectory: %s \n", link.getWorkingDirectory());
        System.out.printf("getCommandLineArguments: %s \n", link.getCommandLineArguments());
        System.out.printf("isLocal: %b \n", link.isLocal());
        System.out.printf("isDirectory: %b \n", link.isDirectory());

    }
}