package org.nmslite.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class Utils
{

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static ConcurrentMap<String, Object> config = new ConcurrentHashMap<>();

    private static final AtomicLong counter = new AtomicLong(0);


    // TODO : GET Method for extracting Values from Config Map
    // TODO : Util method for Encode,Decode and getLong

    public static long getId()
    {

        return counter.incrementAndGet();

    }

    public static boolean readConfig()
    {


        try
        {
            Vertx vertx = Bootstrap.getVertx();

            vertx.fileSystem().readFile(Constants.CONFIG_PATH, handler ->
            {
                if (handler.succeeded())
                {
                    var data = handler.result().toJsonObject();

                    for (var key : data.fieldNames())
                    {
                        config.put(key, data.getValue(key));
                    }

                    logger.info("Config File Read Successfully...");

                    logger.trace(config.toString());
                }
                else
                {
                    logger.error("Error Occurred reading the config file :  ", handler.cause());

                }
            });
        }
        catch (Exception exception)
        {
            logger.error("error reading config file {}", exception.getMessage());

            logger.error(Arrays.toString(exception.getStackTrace()));

            return false;
        }
        return true;
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

            context.put(Constants.DEVICE_PORT, Integer.parseInt(discoveryInfo.getString(Constants.DEVICE_PORT)));

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

        Vertx vertx = Bootstrap.getVertx();

        String fileName = Constants.FILE_PATH + ip + ".txt";

        LocalDateTime now = LocalDateTime.now();

        String formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String data = "{ \"" + formattedDateTime + "\" : " + result.toString() + "}\n";

        vertx.fileSystem().open(fileName, new OpenOptions().setAppend(true), asyncResult ->
        {
            if (asyncResult.succeeded())
            {

                AsyncFile file = asyncResult.result();

                Buffer buffer = Buffer.buffer(data);

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

