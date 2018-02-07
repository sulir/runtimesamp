package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class Branch {
    public enum Direction {
        BACKWARD, SAME_LINE, FORWARD
    }

    public static final AbstractInsnNode INTER_METHOD = null;

    private final AbstractInsnNode target;
    private final AbstractInsnNode source;
    private final int condition;
    private final Direction direction;

    public Branch(AbstractInsnNode source, AbstractInsnNode target, LineMap lineMap) {
        this(source, target, Opcodes.GOTO, lineMap);
    }

    public Branch(AbstractInsnNode source, AbstractInsnNode target, int condition, LineMap lineMap) {
        this.source = source;
        this.target = target;
        this.condition = condition;

        int sourceLine = lineMap.getLine(source);
        int targetLine = (target == INTER_METHOD) ? Integer.MAX_VALUE : lineMap.getLine(target);

        if (targetLine > sourceLine)
            direction = Direction.FORWARD;
        else if (targetLine == sourceLine)
            direction = Direction.SAME_LINE;
        else
            direction = Direction.BACKWARD;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getConditionNegation() {
        if (condition >= Opcodes.IFEQ && condition <= Opcodes.IF_ACMPNE)
            return (condition % 2 == 0) ? condition - 1 : condition + 1;

        switch (condition) {
            case Opcodes.IFNULL:
                return Opcodes.IFNONNULL;
            case Opcodes.IFNONNULL:
                return Opcodes.IFNULL;
            case Opcodes.NOP:
                return Opcodes.GOTO;
            default:
                return Opcodes.NOP;
        }
    }

    public InsnList getConditionalJump(LabelNode target) {
        InsnList list = new InsnList();

        InsnNode duplication = getConditionOperandDuplication();
        if (duplication != null)
            list.add(duplication);

        if (condition != Opcodes.NOP)
            list.add(new JumpInsnNode(condition, target));

        return list;
    }

    public boolean isNextInstruction() {
        return target != INTER_METHOD && target == source.getNext();
    }

    private InsnNode getConditionOperandDuplication() {
        if ((condition >= Opcodes.IFEQ && condition <= Opcodes.IFLE)
                || condition == Opcodes.IFNULL || condition == Opcodes.IFNONNULL) {
            return new InsnNode(Opcodes.DUP);
        } else if (condition >= Opcodes.IF_ICMPEQ && condition <= Opcodes.IF_ACMPNE) {
            return new InsnNode(Opcodes.DUP2);
        } else {
            return null;
        }
    }
}
