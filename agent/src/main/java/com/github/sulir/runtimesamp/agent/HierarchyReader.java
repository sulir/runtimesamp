package com.github.sulir.runtimesamp.agent;

import org.mutabilitydetector.asm.typehierarchy.TypeHierarchy;
import org.mutabilitydetector.asm.typehierarchy.TypeHierarchyReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HierarchyReader extends TypeHierarchyReader {
    private ClassLoader classLoader;
    private Map<Type, TypeHierarchy> cache = new HashMap<>();

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected ClassReader reader(Type type) throws IOException {
        String file = type.getInternalName().replace('.', '/') + ".class";
        return new ClassReader(classLoader.getResourceAsStream(file));
    }

    @Override
    public TypeHierarchy hierarchyOf(Type type) {
        TypeHierarchy hierarchy = cache.get(type);

        if (hierarchy == null) {
            hierarchy = super.hierarchyOf(type);
            cache.put(type, hierarchy);
        }

        return hierarchy;
    }
}
