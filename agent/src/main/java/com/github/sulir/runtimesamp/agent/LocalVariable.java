package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.VarInsnNode;

public class LocalVariable extends Variable {
    private int index;

    public static Variable fromInstruction(VarInsnNode instruction, VariableMap map) {
        LocalVariableNode variable = map.getLocalVariable(instruction.var);

        if (variable != null && isNotSynthetic(variable.name) && instruction.getOpcode() != Opcodes.RET)
            return new LocalVariable(variable.name, Type.getType(variable.desc), variable.index);
        else
            return null;
    }

    private LocalVariable(String name, Type type, int index) {
        super(name, type);
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public InsnList getLoadInstructions() {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
        return instructions;
    }

}
