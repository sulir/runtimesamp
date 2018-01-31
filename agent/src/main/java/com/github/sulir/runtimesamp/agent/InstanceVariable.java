package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

public class InstanceVariable extends Variable {
    private final String owner;

    public static Variable fromInstruction(FieldInsnNode instruction, FieldAnalyzer analyzer) {
        if (analyzer.operatesOnThis(instruction) && isNotSynthetic(instruction.name))
            return new InstanceVariable(instruction.name, Type.getType(instruction.desc), instruction.owner);
        else
            return null;
    }

    private InstanceVariable(String name, Type type, String owner) {
        super(name, type);
        this.owner = owner;
    }

    @Override
    public int getIndex() {
        return -1;
    }

    @Override
    public InsnList getLoadInstructions() {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, owner, name, type.getDescriptor()));
        return instructions;
    }
}
