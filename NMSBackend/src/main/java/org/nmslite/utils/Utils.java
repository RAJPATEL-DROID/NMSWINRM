package org.nmslite.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    public static ZContext zcontext = new ZContext();

    public static ZMQ.Socket reqSocket = zcontext.createSocket(SocketType.PUSH);

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static ConcurrentMap<String,Object> config= new ConcurrentHashMap<>();

    private static final AtomicLong counter = new AtomicLong(0);



    public static long getId()
    {

        return counter.incrementAndGet();

    }

    public static Future<Void> readConfig()
    {
        Promise<Void> promise = Promise.promise();
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

                    // Bind the REQ socket to a local address
                    reqSocket.bind(Constants.ZMQ_ADDRESS +Utils.config.get(Constants.PUSH_PORT));

                    logger.info("Config File Read Successfully...");

                    logger.trace(config.toString());

                    promise.complete();
                }
                else
                {
                    logger.error("Error Occurred reading the config file :  ",handler.cause());

                    promise.fail(handler.cause());
                }
            });
        }
        catch (Exception exception)
        {
           logger.error("error reading config file {}", exception.getMessage());

           logger.error(Arrays.toString(exception.getStackTrace()));

           promise.fail("Exception Occurred in Reading Config File");
        }
        return promise.future();
    }

    public static JsonObject getData(RequestType type) {

        var response = ConfigDB.read(type);

        response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

        response.put(Constants.STATUS, Constants.SUCCESS);

        return response;
    };

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
        catch(Exception exception)
        {

            logger.error("Exception occurred in creating context : ",exception);

            logger.error(Arrays.toString(exception.getStackTrace()));

            return contexts;

        }
        return contexts;
    }

    public static JsonObject errorHandler(String error,Integer errorCode,String errorMessage)
    {
        return new JsonObject()
                        .put(Constants.ERROR,error)
                        .put(Constants.ERROR_CODE, errorCode)
                        .put(Constants.ERROR_MESSAGE, errorMessage)
                        .put(Constants.STATUS,Constants.FAILED);
    }

    public static boolean checkAvailability(String ip)
    {

        ProcessBuilder processBuilder = new ProcessBuilder("fping", "-c", "3", "-q", ip);

        processBuilder.redirectErrorStream(true);
        try
        {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null)
            {
                if (line.contains("/0%"))
                {
                    logger.info("Device with IP address {} is up", ip);

                    return true;
                }
                else
                {
                    logger.info("Device with IP address {} is down", ip);
                }
            }

        } catch (Exception exception)
        {
            logger.error(exception.getMessage());

            return false;

        }
        return false;

    }

    public static void writeToFile(String ip, JsonObject result)
    {
        String fileName = Constants.FILE_PATH +  ip + ".txt";

        LocalDateTime now = LocalDateTime.now();

        String formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (FileWriter fileWriter = new FileWriter(fileName,true))
        {
            fileWriter.write("{ \"" + formattedDateTime + "\" : " + result.toString() + "}\n");

            logger.info("Result successfully written to file: {} " , fileName);
        }
        catch (IOException exception)
        {
            logger.error("Error writing result to file: {}" , exception.getMessage());
        }
    }

    public static void sendContext(byte[] context)
    {
           reqSocket.send(context,ZMQ.DONTWAIT);
    }

}

