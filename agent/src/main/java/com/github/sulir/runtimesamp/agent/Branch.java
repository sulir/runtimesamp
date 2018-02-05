package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

public class Branch {
    public static final AbstractInsnNode INTER_METHOD = null;
    private static final int NO_CONDITION = -1;

    private final AbstractInsnNode target;
    private final AbstractInsnNode source;
    private final int condition;
    private final LineMap lineMap;

    public Branch(AbstractInsnNode source, AbstractInsnNode target, LineMap lineMap) {
        this(source, target, NO_CONDITION, lineMap);
    }

    public Branch(AbstractInsnNode source, AbstractInsnNode target, int condition, LineMap lineMap) {
        this.source = source;
        this.target = target;
        this.condition = condition;
        this.lineMap = lineMap;
    }

    public int getCondition() {
        return condition;
    }

    public int getConditionNegation() {
        if (condition >= Opcodes.IFEQ && condition <= Opcodes.IF_ACMPNE)
            return (condition % 2 == 0) ? condition - 1 : condition + 1;

        switch (condition) {
            case Opcodes.IFNULL:
                return Opcodes.IFNONNULL;
            case Opcodes.IFNONNULL:
                return Opcodes.IFNULL;
            default:
                return NO_CONDITION;
        }
    }

    public InsnNode getConditionOperandDuplication() {
        if ((condition >= Opcodes.IFEQ && condition <= Opcodes.IFLE)
                || condition == Opcodes.IFNULL || condition == Opcodes.IFNONNULL) {
            return new InsnNode(Opcodes.DUP);
        } else if (condition >= Opcodes.IF_ICMPEQ && condition <= Opcodes.IF_ACMPNE) {
            return new InsnNode(Opcodes.DUP2);
        } else {
            return null;
        }
    }

    public boolean isNextInstruction() {
        return target != INTER_METHOD && target == source.getNext();
    }

    public boolean isSameLine() {
        return target != INTER_METHOD && lineMap.getLine(source) == lineMap.getLine(target);
    }
}
