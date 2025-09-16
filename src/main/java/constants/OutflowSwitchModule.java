package constants;

public enum OutflowSwitchModule {
    SWITCH_ALGORITHM_MODULE("switch_algorithm");

    private final String name;

    OutflowSwitchModule(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
