import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Environment<T> {

    public LinkedList<LinkedHashMap<String, T>> contexts = new LinkedList<>();

    protected String currentFuncName;

    public void clearContexts() {
        contexts = new LinkedList<LinkedHashMap<String, T>>();
    }

    public void setCurrentFunc(String functionName) {
        this.currentFuncName = functionName;
    }

    public String currentFuncName() {
        return currentFuncName;
    }

    public void newContext() {
        contexts.push(new LinkedHashMap<>());
    }

    public void removeContext() {
        contexts.pop();
    }

    public Environment<T> currentContextOnly() {
        Environment<T> scopeCopy = new Environment<T>();
        if (!contexts.isEmpty()) {
            scopeCopy.contexts.push(new LinkedHashMap<>(contexts.peek()));
        }
        scopeCopy.currentFuncName = this.currentFuncName;
        return scopeCopy;
    }

    public T lookupVar(String variableName) {
        // assuming latest context at index 0
        for (LinkedHashMap<String, T> context : this.contexts) {
            if (context.containsKey(variableName)) {
                return context.get(variableName);
            }
        }
        throw new TypeException("Variable " +
                variableName + " not found in contexts.");
    }

    public void assignVar(String variableName, T assignedValue) {
        // assuming latest context at index 0
        for (LinkedHashMap<String, T> context : this.contexts) {
            if (context.containsKey(variableName)) {
                context.put(variableName, assignedValue);
                return;
            }
        }
        throw new TypeException("Variable "
                + variableName + " not found in contexts.");
    }

    public boolean existsVar(String variableName) {
        // assuming latest context at index 0
        for (LinkedHashMap<String, T> context : this.contexts) {
            if (context.containsKey(variableName)) {
                return true;
            }
        }
        return false;
    }

    public void extendVar(String variableName, T variableType) {
        this.contexts.peek().put(variableName, variableType);
    }


}
