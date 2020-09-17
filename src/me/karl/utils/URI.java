package me.karl.utils;

import me.anno.utils.OS;

import java.io.*;

/**
 * Represents a "file" inside a Jar File. Used for accessing resources (models, textures), as they
 * are all inside a jar file when exported.
 *
 * @author Karl
 */
public class URI {

    private static final String FILE_SEPARATOR = "/";

    private String path;
    private String name;
    private File file;

    public URI(File file) {
        this.file = file;
    }

    public URI(String path) {
        this.path = FILE_SEPARATOR + path;
        String[] dirs = path.split(FILE_SEPARATOR);
        this.name = dirs[dirs.length - 1];
    }

    public URI(String... paths) {
        this.path = "";
        for (String part : paths) {
            this.path += (FILE_SEPARATOR + part);
        }
        String[] dirs = path.split(FILE_SEPARATOR);
        this.name = dirs[dirs.length - 1];
    }

    public URI(URI file, String subFile) {
        this.path = file.path + FILE_SEPARATOR + subFile;
        this.name = subFile;
    }

    public URI(URI file, String... subFiles) {
        this.path = file.path;
        for (String part : subFiles) {
            this.path += (FILE_SEPARATOR + part);
        }
        String[] dirs = path.split(FILE_SEPARATOR);
        this.name = dirs[dirs.length - 1];
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return getPath();
    }

    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file == null ?
				new File(new File(OS.INSTANCE.getDocuments(), "IdeaProjects\\TestDae\\src"), path.substring(1)) :
				file);
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
