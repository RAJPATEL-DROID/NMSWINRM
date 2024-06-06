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

public class FileReader
{

    public static final Logger logger = LoggerFactory.getLogger(FileReader.class);

    private static final String FILE_PATH_PREFIX = Constants.FILE_PATH;

    public JsonArray readLastNLinesByIP(String ipAddress, int numLines)
    {

        var filePath = FILE_PATH_PREFIX + ipAddress + ".txt";

        var jsonArray = new JsonArray();
        try
        {
            var lastNLines = readLastNLines(filePath, numLines);

            for (var line : lastNLines)
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

    private static List<String> readLastNLines(String filePath, int numLines) throws IOException
    {

        List<String> lastNLines = new ArrayList<>();

        var path = Paths.get(filePath);

        try (var file = Files.lines(path))
        {
            long count = file.count();

            try (var lines = Files.lines(path))
            {
                if (count <= numLines)
                {
                    // If the file has fewer lines than numLines, read all lines
                    lastNLines = lines.collect(Collectors.toList());
                }
                else
                {
                    lastNLines = lines.skip(count - numLines).collect(Collectors.toList());
                }
            }
            catch (IOException exception)
            {
                logger.error("Error reading last n lines", exception);

                return null;
            }
        }
        catch (IOException exception)
        {
            logger.error("Error in File Reading : ", exception);

            return null;
        }

        return lastNLines;
    }

}
