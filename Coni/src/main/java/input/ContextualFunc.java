package input;

import state.ConnectorState;

import java.lang.reflect.Method;
import java.util.Objects;

public class ContextualFunc {
    private Method method;
    private String[] paramName;
    private final ConnectorState previousState;
    private final ConnectorState nextState;
    private String name;

    public ConnectorState getPreviousStates() {
        return previousState;
    }

    public String[] getParamName() {
        return paramName;
    }

    public ConnectorState getNextState() {
        return nextState;
    }

    public Method getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }

    public ContextualFunc(Method method, String[] paramName, ConnectorState previousState, ConnectorState nextState) {
        this.method = method;
        this.paramName = paramName;
        this.previousState = previousState;
        this.nextState = nextState;
        this.name = this.getClass().getSimpleName() + "." + this.getMethod().getName();
    }

    public boolean changeState() {
        return this.nextState.equals(this.previousState);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContextualFunc cs = (ContextualFunc) o;
        return this.previousState.equals(cs.previousState) && this.nextState.equals(cs.nextState) && this.method.equals(cs.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.method, this.previousState, this.nextState);
    }
}
