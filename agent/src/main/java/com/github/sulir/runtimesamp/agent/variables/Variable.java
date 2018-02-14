package com.github.sulir.runtimesamp.agent.variables;

import com.github.sulir.runtimesamp.agent.MethodTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

public abstract class Variable {
    private static final Map<Integer, MethodInsnNode> boxInstructions = new HashMap<>();

    protected String name;
    protected Type type;

    public abstract int getIndex();
    public abstract InsnList getLoadInstructions();

    static {
        final Type[] types = {Type.BOOLEAN_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE, Type.DOUBLE_TYPE,
                Type.FLOAT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.SHORT_TYPE};
        final String[] classes = {"Boolean", "Byte", "Character", "Double", "Float", "Integer", "Long", "Short"};

        for (int i = 0; i < types.length; i++) {
            String owner = "java/lang/" + classes[i];
            String descriptor = Type.getMethodDescriptor(Type.getObjectType(owner), types[i]);
            MethodInsnNode instruction = new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "valueOf", descriptor, false);
            boxInstructions.put(types[i].getSort(), instruction);
        }
    }

    public static Variable fromInstruction(AbstractInsnNode instruction, MethodTransformer transformer) {
        if (instruction instanceof FieldInsnNode) {
            if (instruction.getOpcode() == Opcodes.GETFIELD || instruction.getOpcode() == Opcodes.PUTFIELD)
                return InstanceVariable.fromInstruction((FieldInsnNode) instruction, transformer.getFieldAnalyzer());
        } else if (instruction instanceof VarInsnNode) {
            return LocalVariable.fromInstruction((VarInsnNode) instruction, transformer.getVariableMap());
        }
        return null;
    }

    static boolean isNotSynthetic(String name) {
        return !name.equals("this") && !name.contains("$");
    }

    Variable(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public InsnList getBoxedLoad() {
        InsnList instructions = new InsnList();
        instructions.add(getLoadInstructions());

        MethodInsnNode box = boxInstructions.get(type.getSort());
        if (box != null)
            instructions.add(box.clone(null));

        return instructions;
    }
}
