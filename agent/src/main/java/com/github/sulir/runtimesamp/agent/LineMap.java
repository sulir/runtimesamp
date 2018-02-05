package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LineMap {
    public static final int NO_LINE = -1;

    private MethodNode method;
    private Map<AbstractInsnNode, Integer> instructionToLine = new HashMap<>();

    public LineMap(MethodNode method) {
        this.method = method;
    }

    public void construct() {
        Iterator<AbstractInsnNode> iterator = method.instructions.iterator();
        int currentLine = NO_LINE;

        while (iterator.hasNext()) {
            AbstractInsnNode instruction = iterator.next();

            if (instruction.getType() == AbstractInsnNode.LABEL) {
                AbstractInsnNode next = instruction.getNext();

                if (next != null && next.getType() == AbstractInsnNode.LINE)
                    currentLine = ((LineNumberNode) next).line;
            }

            if (instruction.getType() == AbstractInsnNode.LINE)
                currentLine = ((LineNumberNode) instruction).line;

            instructionToLine.put(instruction, currentLine);
        }
    }

    public int getLine(AbstractInsnNode instruction) {
        return instructionToLine.get(instruction);
    }
}
