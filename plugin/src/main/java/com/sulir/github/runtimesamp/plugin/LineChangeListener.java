package com.sulir.github.runtimesamp.plugin;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class LineChangeListener implements FileEditorManagerListener, CaretListener, DocumentListener {
    private Project project;

    public LineChangeListener(Project project) {
        this.project = project;
    }

    public void register() {
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
        EditorFactory.getInstance().getEventMulticaster().addCaretListener(this, project);
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(this, project);
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VariableValueService.getInstance(project).updateVariables();

        if (event.getNewEditor() != null)
            event.getNewEditor().getComponent().repaint();
    }

    @Override
    public void caretPositionChanged(CaretEvent e) {
        if (e.getEditor().getProject() != project)
            return;

        if (e.getNewPosition().line != e.getOldPosition().line) {
            VariableValueService.getInstance(project).updateVariables();
            e.getEditor().getComponent().repaint();
        }
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        VariableValueService.getInstance(project).invalidateData();
    }
}
