package cloudbrain.windmill.utils;

import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfReadUtils {
    public static JsonObject getServerConfByJson(String filename) throws IOException {
        Path path = Paths.get(filename);
        String data = new String(Files.readAllBytes(path));
        return new JsonObject(data);
    }
}
