package state;

public enum ConnState {
    OPEN("Open"), CLOSE("Close");
    private final String displayName;

    ConnState(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
};
