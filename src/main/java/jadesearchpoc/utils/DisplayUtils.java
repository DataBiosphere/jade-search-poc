package jadesearchpoc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import jadesearchpoc.application.APIPointers;

public final class DisplayUtils {

    private DisplayUtils() { }

    public static String prettyPrintJson(Object obj) {
        try {
            String json = APIPointers.getJacksonObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
            return json;
        } catch (JsonProcessingException jsonEx) {
            return buildJsonError("error pretty printing json", jsonEx);
        }
    }

    public static String buildJsonError(String message, Exception ex) {
        return "{\"message\":\"" + message + "\", "
                + "\"exception_message\":\"" + ex.getMessage() + "\"}";
    }
}
