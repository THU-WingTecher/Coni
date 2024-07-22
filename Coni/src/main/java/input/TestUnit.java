package input;

import java.util.List;

public class TestUnit {
    private final String id;
    private final List<ContextualFunc> funcs;

    public List<ContextualFunc> getFuncs() {
        return funcs;
    }

    public TestUnit(String id, List<ContextualFunc> funcs) {
        this.id = id;
        this.funcs = funcs;
    }

    public String getId() {
        return id;
    }
}
