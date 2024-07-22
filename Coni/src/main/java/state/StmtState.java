package state;

public enum StmtState {
    CREATED("Created"), BATCH("Batch"),
    PREPARED("Prepared"), FILLED("Filled"),
    EXECUTED("Executed"), CLOSE("Close");

    private final String displayName;

    StmtState(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
};
