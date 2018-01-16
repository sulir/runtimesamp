package com.sulir.github.runtimesamp.plugin;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorLinePainter;
import com.intellij.openapi.editor.LineExtensionInfo;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ValueLinePainter extends EditorLinePainter {
    private int pastLineAtCursor;

    @Override
    public Collection<LineExtensionInfo> getLineExtensions(@NotNull Project project,
                                                           @NotNull VirtualFile file, int lineNumber) {
        List<LineExtensionInfo> infos = new ArrayList<>();
        TextAttributes attributes = new TextAttributes();
        String text = "";
        LineExtensionInfo info = new LineExtensionInfo(text, attributes);
        infos.add(info);

        updateEditorLines(project);

        return infos;
    }

    private void updateEditorLines(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        if (editor != null) {
            int lineAtCursor = editor.getCaretModel().getLogicalPosition().line;

            if (lineAtCursor != pastLineAtCursor) {
                pastLineAtCursor = lineAtCursor;
                editor.getComponent().repaint();
            }
        }
    }
}
