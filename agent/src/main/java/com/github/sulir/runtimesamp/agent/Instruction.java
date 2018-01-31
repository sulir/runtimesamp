package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.tree.AbstractInsnNode;

public class Instruction {
    private AbstractInsnNode node;
    private Variable variable;

    public Instruction(AbstractInsnNode node, MethodTransformer transformer) {
        this.node = node;
        variable = Variable.fromInstruction(node, transformer);
    }

    public boolean hasVariable() {
        return variable != null;
    }

    public Variable getVariable() {
        return variable;
    }

    public boolean isLastAtLine() {
        AbstractInsnNode node = this.node;

        if (node.getOpcode() == -1)
            return false;

        while ((node = node.getNext()) != null) {
            if (node.getType() != AbstractInsnNode.LABEL && node.getType() != AbstractInsnNode.FRAME)
                return node.getType() == AbstractInsnNode.LINE;
        }

        return true;
    }
}
