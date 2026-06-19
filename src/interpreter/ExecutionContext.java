package interpreter;

import java.util.HashMap;
import java.util.Map;


public class ExecutionContext {
        private final Map<String, Value> variables = new HashMap<>();
        private final ExecutionContext parent;

        ExecutionContext() {
            this.parent = null;
        }

        ExecutionContext(ExecutionContext parent) {
            this.parent = parent;
        }

        void declareVariable(String name, Value val) {
            if (variables.containsKey(name)) {
                throw new RuntimeException("Variable " + name + " is already declared in this scope");
            }
            variables.put(name, val);
        }

        void setVariable(String name, Value val) {
            if (variables.containsKey(name)) {
                variables.put(name, val);
            } else if (parent != null) {
                parent.setVariable(name, val);
            } else {
                throw new RuntimeException("Variable " + name + " not declared");
            }
        }

        Value getVariable(String name) {
            if (variables.containsKey(name)) {
                return variables.get(name);
            } else if (parent != null) {
                return parent.getVariable(name);
            } else {
                throw new RuntimeException("Variable " + name + " not declared");
            }
        }
}
