package org.nmslite.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileReader {
    public static final Logger logger = LoggerFactory.getLogger(FileReader.class);

    private static final String FILE_PATH_PREFIX = Constants.FILE_PATH;

    public JsonArray readLastNLinesByIP(String ipAddress, int numLines) {

        String filePath = FILE_PATH_PREFIX + ipAddress + ".txt";

        JsonArray jsonArray = new JsonArray();
        try
        {
            List<String> lastNLines = readLastNLines(filePath, numLines);

            for (String line : lastNLines)
            {
                jsonArray.add(new JsonObject(line));
            }

        }
        catch (Exception exception)
        {
            return null;
        }

        return jsonArray;
    }

    private static List<String> readLastNLines(String filePath, int numLines) throws IOException {

        List<String> lastNLines = new ArrayList<>();

        Path path = Paths.get(filePath);

        long count = 0;

        try (Stream<String> file = Files.lines(path))
        {
            count = file.count();

            try (Stream<String> lines = Files.lines(path))
            {
                if (count <= numLines)
                {
                    // If the file has fewer lines than numLines, read all lines
                    lastNLines = lines.collect(Collectors.toList());
                }
                else
                {
                    lastNLines = lines.skip(count - numLines)
                            .collect(Collectors.toList());
                }
            }
            catch (IOException exception)
            {
                logger.error("Error reading last n lines", exception);
            }
        }
        catch (IOException exception)
        {
            logger.error("Error in File Reading : ", exception);
        }

        return lastNLines;
    }
}
