package com.tikiyot.vfm;

// מקור הבורר לרשימות (106): עץ הרשימות של מבנה.txt.

import java.util.ArrayList;
import java.util.List;

final class TreeSource extends PickSource {

    private final Tree t;

    TreeSource(Tree t) {
        this.t = t;
    }

    @Override
    public List<String> children(List<String> path) {
        Tree.Node n = path.isEmpty() ? t.root : t.find(path);
        if (n == null) return null;
        List<String> out = new ArrayList<String>();
        for (Tree.Node c : n.children) out.add(c.name);
        return out;
    }

    @Override
    public boolean large() {
        return false;
    }

    @Override
    public String fullLabel(List<String> path) {
        return Tree.fullLabel(path);
    }
}
