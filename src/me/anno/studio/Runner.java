package me.anno.studio;

// currently most important topics:
// done file manager
// done project management
// done timeline cutting tool
// done general settings?
// todo merging cameras?
// done particle systems
// done move entries in tree

import me.anno.studio.cli.RemsCLI;
import me.anno.studio.rems.RemsStudio;

public class Runner {

    private static boolean contains(String[] values, String query) {
        if (values.length == 0) return false;
        String query2 = "-" + query;
        String query3 = "--" + query;
        String query4 = query + "=";
        for (String value : values) {
            if (value == null) continue;
            String lcValue = value.toLowerCase();
            if (lcValue.equals(query) || lcValue.equals(query2) || lcValue.equals(query3) || lcValue.startsWith(query4)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        if (contains(args, "o") || contains(args, "output") ||
                contains(args, "?") || contains(args, "help") ||
                contains(args, "i") || contains(args, "input")) {
            // somebody wants to use it as a console
            RemsCLI.main(args);
        } else {
            // start the editor
            RemsStudio.main(args);
        }
    }

}
