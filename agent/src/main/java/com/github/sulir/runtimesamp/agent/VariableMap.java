package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.Collectors;

public class VariableMap {
    private Map<Label, List<LocalVariableNode>> starts;
    private Map<Label, List<LocalVariableNode>> ends;
    private Map<Integer, LocalVariableNode> localVariablesInScope = new HashMap<>();
    private Map<String, Variable> variablesAtLine = new LinkedHashMap<>();

    public VariableMap(MethodNode method) {
        starts = method.localVariables.stream().collect(Collectors.groupingBy(var -> var.start.getLabel()));
        ends = method.localVariables.stream().collect(Collectors.groupingBy(var -> var.end.getLabel()));
    }

    public LocalVariableNode getLocalVariable(int index) {
        return localVariablesInScope.get(index);
    }

    public Collection<Variable> getVariablesAtLine() {
        return variablesAtLine.values();
    }

    public void update(AbstractInsnNode instruction, MethodTransformer transformer) {
        switch (instruction.getType()) {
            case AbstractInsnNode.LABEL:
                updateScope((LabelNode) instruction);
                break;
            case AbstractInsnNode.LINE:
                variablesAtLine.clear();
                break;
            default:
                Variable variable = Variable.fromInstruction(instruction, transformer);
                if (variable != null)
                    variablesAtLine.put(variable.getName(), variable);
        }
    }

    private void updateScope(LabelNode label) {
        for (LocalVariableNode variable : starts.getOrDefault(label.getLabel(), new ArrayList<>(0))) {
            localVariablesInScope.put(variable.index, variable);
        }

        for (LocalVariableNode varToRemove : ends.getOrDefault(label.getLabel(), new ArrayList<>(0))) {
            localVariablesInScope.remove(varToRemove.index);

            Iterator<Map.Entry<String, Variable>> variables = variablesAtLine.entrySet().iterator();
            while (variables.hasNext()) {
                Variable variable = variables.next().getValue();
                if (variable.getIndex() == varToRemove.index)
                    variables.remove();
            }
        }
    }
}
