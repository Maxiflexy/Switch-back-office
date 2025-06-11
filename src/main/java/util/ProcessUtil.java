package util;

public enum ProcessUtil {

    ENTRUST("validatetoken", "ENTRUST"),
    USER_CREATION("validatetoken", "USER_CREATION");



    public String URI = null;
    public String MESSAGE_TYPE = null;

    ProcessUtil(String URI, String MESSAGE_TYPE) {

        this.URI = URI;
        this.MESSAGE_TYPE = MESSAGE_TYPE;

    }

}
