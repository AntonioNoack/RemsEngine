package me.karl.utils;

import me.anno.utils.OS;
import org.lwjgl.system.CallbackI;

import java.io.*;

/**
 * Represents a "file" inside a Jar File. Used for accessing resources (models, textures), as they
 * are all inside a jar file when exported.
 *
 * @author Karl
 */
public class URI {

    private static final String FILE_SEPARATOR = "/";

    private CharSequence path;
    private String name;
    public File file;

    public URI(File file) {
        this.file = file;
    }

    public URI(String path) {
        this.path = FILE_SEPARATOR + path;
        String[] dirs = path.split(FILE_SEPARATOR);
        this.name = dirs[dirs.length - 1];
    }

    public URI(String... paths) {
        StringBuilder path2 = new StringBuilder(2 * paths.length);
        this.path = path2;
        for (String part : paths) {
            path2.append(FILE_SEPARATOR);
            path2.append(part);
        }
        String[] dirs = paths[paths.length-1].split(FILE_SEPARATOR);
        this.name = dirs[dirs.length - 1];
    }

    public URI(URI file, String subFile) {
        this.path = file.path + FILE_SEPARATOR + subFile;
        this.name = subFile;
    }

    public URI(URI file, String... subFiles) {
        StringBuilder path = new StringBuilder(subFiles.length * 2 + 1);
        path.append(file.path.toString());
        for (String part : subFiles) {
            path.append(FILE_SEPARATOR);
            path.append(part);
        }
        this.path = path;
        String[] dirs = subFiles[subFiles.length-1].split(FILE_SEPARATOR);
        this.name = dirs[dirs.length - 1];
    }

    public URI getParent(){
        if(file != null) return new URI(file.getParentFile());
        int si = path.toString().lastIndexOf(FILE_SEPARATOR);
        return new URI(path.toString().substring(0, si));
    }

    public URI getChild(String name){
        if(file != null) return new URI(new File(file, name));
        return new URI(this, name);
    }

    public CharSequence getPath() {
        return path;
    }

    @Override
    public String toString() {
        return file == null ? getPath().toString() : file.toString();
    }

    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file == null ? new File(new File(OS.INSTANCE.getDocuments(), "IdeaProjects\\VideoStudio\\src\\me\\karl"), path.toString().substring(1)) : file);
        // return Class.class.getResourceAsStream(path);
    }

    public BufferedReader getReader() throws Exception {
        try {
            InputStreamReader isr = new InputStreamReader(getInputStream());
            return new BufferedReader(isr);
        } catch (Exception e) {
            System.err.println("Couldn't get reader for " + path);
            throw e;
        }
    }

    public String getName() {
        return name;
    }

}
