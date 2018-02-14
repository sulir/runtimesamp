package com.github.sulir.runtimesamp.agent;

import com.github.sulir.runtimesamp.support.Data;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.Arrays;
import java.util.Iterator;

public class MethodTransformer {
    private final InsnList instructions;
    private final String className;
    private final LineMap lineMap;
    private final FieldAnalyzer fieldAnalyzer;
    private final VariableMap variableMap;
    private final int passVariable;
    private InsnList inserted;

    public MethodTransformer(MethodNode method, String className) {
        instructions = method.instructions;
        this.className = className;
        lineMap = new LineMap(method);
        fieldAnalyzer = new FieldAnalyzer(method);
        variableMap = new VariableMap(method);
        passVariable = method.maxLocals;
    }

    public void transform() {
        lineMap.construct();
        fieldAnalyzer.analyze();

        Iterator<AbstractInsnNode> iterator = instructions.iterator();
        instructions.insert(getResetPass());

        while (iterator.hasNext()) {
            AbstractInsnNode instruction = iterator.next();
            variableMap.update(instruction, this);

            if (lineMap.getLine(instruction) == LineMap.NO_LINE)
                continue;

            ControlNode node = new ControlNode(instruction, lineMap);

            if (node.canGoToOtherLine()) {
                inserted = new InsnList();
                generateInstrumentation(node);

                if (node.isSequential())
                    instructions.insert(instruction, inserted);
                else
                    instructions.insertBefore(instruction, inserted);
            }
        }
    }

    public FieldAnalyzer getFieldAnalyzer() {
        return fieldAnalyzer;
    }

    public VariableMap getVariableMap() {
        return variableMap;
    }

    private void generateInstrumentation(ControlNode node) {
        int lineId = lineMap.getLineId(node.getInstruction());
        int line = lineMap.getLine(node.getInstruction());

        LabelNode skipRecording = new LabelNode();
        LabelNode skipToEnd = new LabelNode();

        skipIfLineWillStay(node, skipToEnd);
        skipIfNoHitsLeft(lineId, skipRecording);

        storeVariables(lineId, line);

        inserted.add(skipRecording);
        resetPassIfBackward(node, skipToEnd);
        inserted.add(skipToEnd);
    }

    private void skipIfLineWillStay(ControlNode node, LabelNode skip) {
        for (Branch branch : node.getBranches()) {
            if (branch.getDirection() == Branch.Direction.SAME_LINE)
                inserted.add(branch.getConditionalJump(skip));
        }
    }

    private void skipIfNoHitsLeft(int lineId, LabelNode skip) {
        inserted.add(Data.getReadLineHitsLeft());
        inserted.add(getPushInstruction(lineId));
        inserted.add(new InsnNode(Opcodes.BALOAD));
        inserted.add(new JumpInsnNode(Opcodes.IFEQ, skip));
    }

    private void storeVariables(int lineId, int line) {
        inserted.add(getPushInstruction(lineId));
        inserted.add(new VarInsnNode(Opcodes.ILOAD, passVariable));
        inserted.add(new LdcInsnNode(className));
        inserted.add(getPushInstruction(line));

        Variable[] variables = variableMap.getVariablesAtLine();

        if (variables.length <= Data.MAX_VARIABLE_ARGS) {
            for (Variable variable : variables) {
                inserted.add(new LdcInsnNode(variable.getName()));
                inserted.add(variable.getBoxedLoad());
            }
        } else {
            InsnList[] names = Arrays.stream(variables)
                    .map((v) -> getList(new LdcInsnNode(v.getName()))).toArray(InsnList[]::new);
            inserted.add(getArrayInstructions(names, "java/lang/String"));

            InsnList[] values = Arrays.stream(variables).map(Variable::getBoxedLoad).toArray(InsnList[]::new);
            inserted.add(getArrayInstructions(values, "java/lang/Object"));
        }

        inserted.add(Data.getInvokeStoreVariables(variables.length));
        inserted.add(new VarInsnNode(Opcodes.ISTORE, passVariable));
    }

    private void resetPassIfBackward(ControlNode node, LabelNode skip) {
        if (node.hasNoBackwardBranch())
            return;

        for (Branch branch : node.getBranches()) {
            if (branch.getDirection() == Branch.Direction.FORWARD)
                inserted.add(branch.getConditionalJump(skip));
        }

        inserted.add(getResetPass());
    }

    private InsnList getResetPass() {
        return getList(new InsnNode(Opcodes.ICONST_0), new VarInsnNode(Opcodes.ISTORE, passVariable));
    }

    private InsnList getArrayInstructions(InsnList[] elements, String type) {
        InsnList list = new InsnList();

        list.add(getPushInstruction(elements.length));
        list.add(new TypeInsnNode(Opcodes.ANEWARRAY, type));

        for (int i = 0; i < elements.length; i++) {
            list.add(new InsnNode(Opcodes.DUP));
            list.add(getPushInstruction(i));
            list.add(elements[i]);
            list.add(new InsnNode(Opcodes.AASTORE));
        }

        return list;
    }

    private AbstractInsnNode getPushInstruction(int constant) {
        if (constant >= -1 && constant <= 5)
            return new InsnNode(Opcodes.ICONST_0 + constant);

        if (constant >= Byte.MIN_VALUE && constant <= Byte.MAX_VALUE)
            return new IntInsnNode(Opcodes.BIPUSH, constant);

        if (constant >= Short.MIN_VALUE && constant <= Short.MAX_VALUE)
            return new IntInsnNode(Opcodes.SIPUSH, constant);

        return new LdcInsnNode(constant);
    }

    private InsnList getList(AbstractInsnNode... instructions) {
        InsnList list = new InsnList();

        for (AbstractInsnNode instruction : instructions)
            list.add(instruction);

        return list;
    }

    private void printInstructions() {
        Printer printer = new Textifier();
        TraceMethodVisitor tracer = new TraceMethodVisitor(printer);

        for (AbstractInsnNode instruction : instructions.toArray()) {
            instruction.accept(tracer);
        }

        for (Object line : printer.getText()) {
            System.out.print(line);
        }
    }
}
