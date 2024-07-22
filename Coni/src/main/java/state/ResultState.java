package state;

public enum ResultState {
    CREATED("Created"), SCROLL("Scroll"), UPDATED("Updated"), CLOSE("Close");
    private final String displayName;

    ResultState(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
};
