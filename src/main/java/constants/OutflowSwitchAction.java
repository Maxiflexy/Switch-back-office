package constants;

public enum OutflowSwitchAction {

    CREATE("create"),
    UPDATE("update"),
    DEACTIVATE("deactivate"),
    ACTIVATE("activate");

    private final String name;

    OutflowSwitchAction(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
