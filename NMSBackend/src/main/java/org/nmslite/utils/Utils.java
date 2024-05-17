package org.nmslite.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.db.ConfigDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    private Utils(){
        throw new IllegalStateException("Utils class");
    }

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static ConcurrentMap<String,Object> config= new ConcurrentHashMap<>();

    private static final AtomicLong counter = new AtomicLong(0);

    public static long getId()
    {

        return counter.incrementAndGet();

    }

    public static Boolean readConfig()  {
        try
        {
            ObjectMapper mapper = new ObjectMapper();

            config = mapper.readValue(new File(Constants.CONFIG_PATH), ConcurrentMap.class);

            return true;
        }
        catch (Exception exception)
        {
           logger.error("error reading config file {}", exception.getMessage());

           return false;
        }
    }

    public static JsonObject getData(String type) {

        var response = new JsonObject();

        var result = new JsonObject(ConfigDB.read(type));

        response.mergeIn(result);

        response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

        response.put(Constants.STATUS, Constants.SUCCESS);

        return response;
    };

    public static JsonArray createContext(JsonArray targets, String requestType, Logger logger)
    {

        var contextArray = new JsonArray();

        try
        {
            for(var target: targets)
            {
                var targetData = new JsonObject(target.toString());

                var context = new JsonObject();

                var discoveryInfo = targetData.getJsonObject(Constants.DISCOVERY_DATA);

                context.put(Constants.REQUEST_TYPE, requestType);

                context.put(Constants.DEVICE_PORT, Integer.parseInt(discoveryInfo.getString(Constants.DEVICE_PORT)));

                context.put(Constants.IP, discoveryInfo.getString(Constants.IP));

                context.put(Constants.CREDENTIAL_PROFILES, targetData.getJsonArray(Constants.CREDENTIAL_PROFILES));

                contextArray.add(context);
            }

        }
        catch(Exception exception)
        {
            logger.error(String.format("Exception: %s",exception));

            logger.error(Arrays.toString(exception.getStackTrace()));

        }

        return contextArray;

    }

    public static boolean checkAvailability(String ip) {

        ProcessBuilder processBuilder = new ProcessBuilder("fping", "-c", "3", "-q", ip);

        processBuilder.redirectErrorStream(true);
        try
        {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            // TODO : Change This Logic
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


    public static JsonArray spawnPluginEngine(String encodedString, Integer size)
    {
        try
        {
            var currentDir = System.getProperty("user.dir");

            var processBuilder = new java.lang.ProcessBuilder(currentDir + Constants.PLUGIN_APPLICATION_PATH, encodedString);

            processBuilder.redirectErrorStream(true);

            var process = processBuilder.start();

            var exitStatus = process.waitFor((Integer) Utils.config.get(Constants.PLUGIN_PROCESS_TIMEOUT), TimeUnit.SECONDS);

            if(!exitStatus)
            {

                process.destroyForcibly();

                logger.error("Process Timed out, Killed Forcibly!!");

                return null;

            }

            // Read the output of the command
            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));


            var buffer = Buffer.buffer();

            String line;

            var count = 0;

            while ((line = reader.readLine()) != null && count <= size) {
                buffer.appendString(line);
                if (line.contains(Constants.UNIQUE_SEPARATOR   )) {
                    count++;
                }
            }

            logger.info("Context Received from the Plugin {} ", buffer);

            var contexts = buffer.toString().split(Constants.UNIQUE_SEPARATOR);

            var replyJson = new JsonArray();

            for (var context : contexts)
            {

                byte[] decodedBytes = Base64.getDecoder().decode(context);

                var decodedString = new String(decodedBytes);

                logger.info(decodedString);

                replyJson.add(decodedString);

            }

            return replyJson;

        }
        catch (Exception exception)
        {
            logger.error(exception.getMessage());

            logger.error(Arrays.toString(exception.getStackTrace()));

            // TODO : IN EXCEPTION CASE-> Create New OBJECT,INSERT ERRORS AND SEND IT
            return null;

        }
    }



}

