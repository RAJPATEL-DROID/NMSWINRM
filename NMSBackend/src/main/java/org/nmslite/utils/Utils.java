package org.nmslite.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zmq.ZMQ;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Utils
{

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static final ConcurrentMap<String, Object> config = new ConcurrentHashMap<>();

    public static boolean readConfig()
    {

        try
        {
            var vertx = Bootstrap.getVertx();

            var data = vertx.fileSystem().readFileBlocking(Constants.CONFIG_PATH).toJsonObject();

            for (var key : data.fieldNames())
            {
                config.put(key, data.getValue(key));
            }

            logger.info("Config File Read Successfully...");

            logger.trace(config.toString());
        }
        catch (Exception exception)
        {
            logger.error("error reading config file {}", exception.getMessage());

            logger.error(Arrays.toString(exception.getStackTrace()));

            return false;
        }
        return true;
    }

    public static Object get(String key)
    {
        return config.get(key);
    }

    public static String encode(String message)
    {
        return Base64.getEncoder().encodeToString(message.getBytes(ZMQ.CHARSET));
    }

    public static JsonObject decode(String message)
    {
        var decodedBytes = Base64.getDecoder().decode(message);

        var decodedString = new String(decodedBytes);

        return new JsonObject(decodedString);
    }

    public static Long getLong(String object) throws NumberFormatException
    {
        return Long.parseLong(object);
    }

    public static Integer getInteger(String object) throws NumberFormatException
    {

        return Integer.parseInt(object);
    }

    public static JsonArray createContext(JsonObject targets, String requestType, Logger logger)
    {

        var contexts = new JsonArray();
        try
        {

            var context = new JsonObject();

            var discoveryInfo = targets.getJsonObject(Constants.DISCOVERY_DATA);

            context.put(Constants.DISCOVERY_ID, targets.getLong(Constants.ID));

            context.put(Constants.REQUEST_TYPE, requestType);

            context.put(Constants.DEVICE_PORT, Utils.getInteger(discoveryInfo.getString(Constants.DEVICE_PORT)));

            context.put(Constants.IP, discoveryInfo.getString(Constants.IP));

            context.put(Constants.CREDENTIAL_PROFILES, targets.getJsonArray(Constants.CREDENTIAL_PROFILES));

            contexts.add(context);

        }
        catch (Exception exception)
        {

            logger.error("Exception occurred in creating context : ", exception);

            logger.error(Arrays.toString(exception.getStackTrace()));

            return contexts;

        }
        return contexts;
    }

    public static JsonObject errorHandler(String error, Integer errorCode, String errorMessage)
    {

        return new JsonObject().put(Constants.ERROR, error)
                .put(Constants.ERROR_CODE, errorCode)
                .put(Constants.ERROR_MESSAGE, errorMessage)
                .put(Constants.STATUS, Constants.FAILED);
    }

    public static void writeToFileAsync(String ip, JsonObject result)
    {

        var vertx = Bootstrap.getVertx();

        var fileName = Constants.FILE_PATH + ip + ".txt";

        var now = LocalDateTime.now();

        var formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        var data = "{ \"" + formattedDateTime + "\" : " + result.toString() + "}\n";

        vertx.fileSystem().open(fileName, new OpenOptions().setAppend(true), asyncResult ->
        {
            if (asyncResult.succeeded())
            {

                var file = asyncResult.result();

                var buffer = Buffer.buffer(data);

                file.write(buffer, writeResult ->
                {
                    if (writeResult.succeeded())
                    {
                        logger.info("Result successfully written to file: {}", fileName);
                    }
                    else
                    {
                        logger.error("Error writing result to file: {}", writeResult.cause().getMessage());

                    }

                    file.close(closeResult ->
                    {
                        if (closeResult.failed())
                        {
                            logger.error("Error closing file: {}", closeResult.cause().getMessage());
                        }
                    });

                });
            }
            else
            {
                logger.error("Error opening file: {}", asyncResult.cause().getMessage());
            }
        });
    }

//    public static boolean checkAvailability(String ip)
//    {
//
//        ProcessBuilder processBuilder = new ProcessBuilder("fping", "-c", "3", "-q", ip);
//
//        processBuilder.redirectErrorStream(true);
//        try
//        {
//            Process process = processBuilder.start();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//            String line;
//
//            while (( line = reader.readLine() ) != null)
//            {
//                if (line.contains("/0%"))
//                {
//                    logger.info("Device with IP address {} is up", ip);
//
//                    return true;
//                }
//                else
//                {
//                    logger.info("Device with IP address {} is down", ip);
//                }
//            }
//
//        }
//        catch (Exception exception)
//        {
//            logger.error(exception.getMessage());
//
//            return false;
//
//        }
//        return false;
//
//    }

}

