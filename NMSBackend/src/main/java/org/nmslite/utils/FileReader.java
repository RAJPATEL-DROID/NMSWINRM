package org.nmslite.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileReader {
    private static final String FILE_PATH_PREFIX = Constants.FILE_PATH;

    public Future<JsonArray> readLastNLinesByIP(Vertx vertx, String ipAddress, int numLines) {
        Promise<JsonArray> promise = Promise.promise();

        String filePath = FILE_PATH_PREFIX + ipAddress + ".txt";

        vertx.executeBlocking(future ->
        {
            try
            {
                List<String> lastNLines = readLastNLines(filePath, numLines);
                String content = String.join("\n", lastNLines);
                JsonArray jsonArray = processFileContent(content);
                future.complete(jsonArray);
            }
            catch (Exception exception)
            {
                future.fail(exception);

            }
        }, promise);

        return promise.future();
    }

    private static List<String> readLastNLines(String filePath, int numLines) throws IOException
    {
        Deque<String> deque = new ArrayDeque<>(numLines);
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.forEach(line -> {
                if (deque.size() == numLines) {
                    deque.removeFirst();
                }
                deque.addLast(line);
            });
        }
        return new ArrayList<>(deque);

    }

    private JsonArray processFileContent(String content) {
        JsonArray jsonArray = new JsonArray();
        Pattern pattern = Pattern.compile("\\{\\s*\"(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})\"\\s*:\\s*(\\{.*?\\})\\s*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String timestamp = matcher.group(1);
            String jsonString = matcher.group(2);
            JsonObject jsonObject = new JsonObject(jsonString);
            jsonArray.add(new JsonObject().put(timestamp, jsonObject));
        }

        return jsonArray;
    }
}
