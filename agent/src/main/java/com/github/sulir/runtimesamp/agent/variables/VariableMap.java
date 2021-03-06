package com.github.sulir.runtimesamp.agent.variables;

import com.github.sulir.runtimesamp.agent.MethodTransformer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.stream.Collectors;

public class VariableMap {
    private Map<LabelNode, List<LocalVariableNode>> starts;
    private Map<LabelNode, List<LocalVariableNode>> ends;
    private Map<Integer, LocalVariableNode> localVariablesInScope = new HashMap<>();
    private Map<String, Variable> variablesAtLine = new LinkedHashMap<>();

    public VariableMap(MethodNode method) {
        starts = method.localVariables.stream().collect(Collectors.groupingBy(var -> var.start));
        ends = method.localVariables.stream().collect(Collectors.groupingBy(var -> var.end));
    }

    public LocalVariableNode getLocalVariable(int index, AbstractInsnNode instruction) {
        LocalVariableNode variable = localVariablesInScope.get(index);

        if (variable == null && instruction.getNext() instanceof LabelNode) {
            List<LocalVariableNode> nextLocals = starts.getOrDefault(instruction.getNext(), Collections.emptyList());

            for (LocalVariableNode local : nextLocals) {
                if (local.index == index)
                    return local;
            }
        }

        return variable;
    }

    public Variable[] getVariablesAtLine() {
        return variablesAtLine.values().toArray(new Variable[0]);
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
        for (LocalVariableNode variable : starts.getOrDefault(label, Collections.emptyList())) {
            localVariablesInScope.put(variable.index, variable);
        }

        for (LocalVariableNode varToRemove : ends.getOrDefault(label, Collections.emptyList())) {
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
