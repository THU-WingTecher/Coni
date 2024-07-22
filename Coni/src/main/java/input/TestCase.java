package input;

import java.util.List;

public class TestCase {
    private final String id;
    private final List<TestUnit> units;

    public List<TestUnit> getUnits() {
        return this.units;
    }

    public TestCase(String id, List<TestUnit> units) {
        this.id = id;
        this.units = units;
    }

    public String getId() {
        return id;
    }
}
