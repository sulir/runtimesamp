package com.github.sulir.runtimesamp.agent.variables;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class LocalVariable extends Variable {
    private int index;

    public static Variable fromInstruction(AbstractInsnNode instruction, VariableMap map) {
        int index;

        if (instruction instanceof VarInsnNode && instruction.getOpcode() != Opcodes.RET)
            index = ((VarInsnNode) instruction).var;
        else if (instruction instanceof IincInsnNode)
            index = ((IincInsnNode) instruction).var;
        else
            return null;

        LocalVariableNode variable = map.getLocalVariable(index, instruction);

        if (variable != null && isNotSynthetic(variable.name))
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
