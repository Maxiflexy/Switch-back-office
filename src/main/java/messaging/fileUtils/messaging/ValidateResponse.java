package messaging.fileUtils.messaging;

import messaging.fileUtils.util.BaseBean;

import javax.json.JsonObject;

public class ValidateResponse {
    public static boolean isValidResponse(JsonObject jsonObject, BaseBean bean) {

        for (String s : bean.keySet()) {
            String key = s.toLowerCase();
            if (!jsonObject.containsKey(key) || !responseNotNull(jsonObject)) {
                return false;
            }
        }
        return true;
    }

    private static boolean responseNotNull(JsonObject jsonObject) {

        for (String s : jsonObject.keySet() ) {
            String key = s.toLowerCase();
            if (jsonObject.get(key).toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
