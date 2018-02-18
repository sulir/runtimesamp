package com.github.sulir.runtimesamp.plugin;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pass {
    private Map<Integer, List<Variable>> lines = new HashMap<>();

    public Pass() { }

    public Pass(String passName, Jedis db) {
        List<String> variables = db.lrange(passName, 0, -1);

        for (String variableString : variables) {
            String[] parts = variableString.split(":", 3);

            int line = Integer.valueOf(parts[0]);
            if (!lines.containsKey(line))
                lines.put(line, new ArrayList<>());

            if (parts.length == 3)
                lines.get(line).add(new Variable(parts[1], parts[2]));
        }
    }

    public List<Variable> getLine(int line) {
        return lines.get(line);
    }
}
