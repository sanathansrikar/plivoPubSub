package pubsub;

import java.util.HashMap;
import java.util.Map;

public class JsonUtil {

    public static Map<String, String> parse(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.replaceAll("[{}\"]", "");
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":");
            map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }
}
