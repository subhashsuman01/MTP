package dev.compL.iitmandi.utils;


import soot.Unit;

import java.util.ArrayList;
import java.util.List;

public class BranchInfo {
    int depth;
    List<BranchInfo> children;
    String type;
    int startLine;
    int endLine;
    Unit startUnit;
    Unit endUnit;
    BranchInfo parent;

    public BranchInfo(String type, int depth, int startLine, int endLine, Unit startUnit, Unit endUnit){
        this.parent = null;
        this.children = new ArrayList<>();
        this.type = type;
        this.depth = depth;
        this.startLine = startLine;
        this.endLine = endLine;
        this.startUnit = startUnit;
        this.endUnit = endUnit;
    }

    public BranchInfo getParent() {
        return parent;
    }

    private void setParent(BranchInfo parent) {
        this.parent = parent;
    }

    public void addChild(BranchInfo child){
        children.add(child);
        child.setParent(this);
    }

    public String getType() {
        return type;
    }

    public int getDepth() {
        return depth;
    }

    public List<BranchInfo> getChildren() {
        return children;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getStartLine() {
        return startLine;
    }

    public Unit getEndUnit() {
        return endUnit;
    }

    public Unit getStartUnit() {
        return startUnit;
    }

    public void setChildren(List<BranchInfo> children) {
        this.children = children;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public void setEndUnit(Unit endUnit) {
        this.endUnit = endUnit;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public void setStartUnit(Unit startUnit) {
        this.startUnit = startUnit;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "BranchInfo{" +
                "depth=" + depth +
                ", children=" + children +
                ", type='" + type + '\'' +
                ", startLine=" + startLine +
                ", endLine=" + endLine +
                ", startUnit=" + startUnit +
                ", endUnit=" + endUnit +
                ", parent=" + parent +
                '}';
    }

    public static void dfs(BranchInfo root){
        System.out.println(root);
        for (BranchInfo child: root.children) {
            dfs(child);
        }
    }
}
