package com.sulir.github.runtimesamp.plugin;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;

public class VariableValueService {
    private final Jedis db = new Jedis("localhost");
    private final Project project;
    private Pass pass;
    private String className;

    public static VariableValueService getInstance(Project project) {
        return ServiceManager.getService(project, VariableValueService.class);
    }

    public VariableValueService(Project project) {
        this.project = project;

        try {
            db.connect();
            new LineChangeListener(project).register();
            updateVariables();
        } catch (JedisException e) { /* variables will not be updated */ }
    }

    public List<Variable> getVariables(VirtualFile file, int line) {
        if (className == null || !className.equals(getClassName(file)))
            return null;

        return pass.getLine(line);
    }

    void updateVariables() {
        pass = new Pass();

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null)
            return;

        className = getClassName(editor);
        int line = getLine(editor);
        List<String> passes = db.lrange("line:" + className + ":" + line, 0, -1);

        if (!passes.isEmpty()) {
            String passName = passes.get(0);
            pass = new Pass(passName, db);
        }
    }

    private String getClassName(Editor editor) {
        return getClassName(FileDocumentManager.getInstance().getFile(editor.getDocument()));
    }

    private String getClassName(VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

        if (!(psiFile instanceof PsiJavaFile))
            return null;

        for (PsiClass clazz : ((PsiJavaFile) psiFile).getClasses()) {
            if (clazz.getContainingClass() == null)
                return clazz.getQualifiedName();
        }

        return null;
    }

    private int getLine(Editor editor) {
        return editor.getCaretModel().getLogicalPosition().line + 1;
    }
}
