package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

public class SourceInstruction {
    private final AbstractInsnNode instruction;
    private final LineMap lineMap;
    private final Branch[] branches;

    public SourceInstruction(AbstractInsnNode instruction, LineMap lineMap) {
        this.instruction = instruction;
        this.lineMap = lineMap;
        branches = computeBranches();
    }

    private Branch[] computeBranches() {
        int opcode = instruction.getOpcode();

        switch (opcode) {
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.RETURN:
            case Opcodes.ATHROW: // assume ATHROW exits a method or jumps forward to another line
                return new Branch[] {new Branch(instruction, Branch.INTER_METHOD, lineMap)};
            case Opcodes.GOTO:
                return new Branch[] {new Branch(instruction, ((JumpInsnNode) instruction).label, lineMap)};
            case Opcodes.LOOKUPSWITCH: // assume cases are not at the same line as switch
                return new Branch[] {new Branch(instruction, ((LookupSwitchInsnNode) instruction).dflt, lineMap)};
            case Opcodes.TABLESWITCH:
                return new Branch[] {new Branch(instruction, ((TableSwitchInsnNode) instruction).dflt, lineMap)};
            default:
                if (instruction instanceof JumpInsnNode) {
                    Branch positive = new Branch(instruction, ((JumpInsnNode) instruction).label, opcode, lineMap);
                    Branch negative = new Branch(instruction, instruction.getNext(),
                            positive.getConditionNegation(), lineMap);
                    return new Branch[] {positive, negative};
                } else {
                    return new Branch[] {new Branch(instruction, instruction.getNext(), lineMap)};
                }
        }
    }

    public Branch[] getBranches() {
        return branches;
    }

    public boolean isSequential() {
        return branches.length == 1 && branches[0].isNextInstruction();
    }

    public boolean canGoToOtherLine() {
        for (Branch branch : branches) {
            if (!branch.isSameLine())
                return true;
        }
        return false;
    }
}
