package util;

import constants.OutflowSwitchModule;
import persistence.OutflowSwitchRequest;
import persistence.SwitchAlgorithmDbHelper;

public class OutFlowSwitchFactory {

    public static OutflowSwitchRequest createOutFlowSwitchRequest(String moduleType) {
        if (moduleType.equalsIgnoreCase(OutflowSwitchModule.SWITCH_ALGORITHM_MODULE.getName())) {
            return new SwitchAlgorithmDbHelper();
        }
        throw new IllegalArgumentException("Invalid module type: " + moduleType);
    }
}
