package com.github.sulir.runtimesamp.plugin;

import com.intellij.openapi.editor.EditorLinePainter;
import com.intellij.openapi.editor.LineExtensionInfo;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.Gray;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class VariableLinePainter extends EditorLinePainter {
    private static final TextAttributes nameAttributes = new TextAttributes(Gray._135, null, null, null, Font.BOLD);
    private static final TextAttributes valueAttributes = new TextAttributes(Gray._135, null, null, null, Font.PLAIN);

    @Override
    public Collection<LineExtensionInfo> getLineExtensions(@NotNull Project project,
                                                           @NotNull VirtualFile file, int lineNumber) {
        List<Variable> variables = VariableValueService.getInstance(project).getVariables(file, lineNumber + 1);

        if (variables == null)
            return null;
        else if (variables.isEmpty())
            return getExecutionMark();
        else
            return getVariablesInfo(variables);
    }

    private Collection<LineExtensionInfo> getExecutionMark() {
        return Collections.singletonList(new LineExtensionInfo("  \u2705", valueAttributes));
    }

    private List<LineExtensionInfo> getVariablesInfo(List<Variable> variables) {
        List<LineExtensionInfo> list = new ArrayList<>();

        for (Variable variable : variables) {
            String name = "  " + variable.getName() + ": ";
            list.add(new LineExtensionInfo(name, nameAttributes));
            list.add(new LineExtensionInfo(variable.getValue(), valueAttributes));
        }
        return list;
    }
}
