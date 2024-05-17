package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class PollingEngine extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(PollingEngine.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        int pollTime  = (Integer) Utils.config.get(Constants.DEFAULT_POLL_TIME) * 1000;

        logger.trace("Default Poll time set to {} ", pollTime);

        //  TODO : Remove HardCoded Conversion,use parseXXX Methods

        Integer maxBatchSize = Integer.parseInt(Utils.config.get(Constants.MAX_BATCH_SIZE).toString());


        logger.trace("Default Max batch size set to {}", maxBatchSize);

        Bootstrap.vertx.setPeriodic(pollTime, tid ->
        {

            var result =new JsonObject(ConfigDB.read(Constants.PROVISION));

            // We Get Array of Discovery Id of Provisioned Device
            if(result.getJsonArray(Constants.PROVISION_DEVICES).isEmpty())
            {
                logger.trace("No Provisioned Device Found : {}", result.getJsonArray(Constants.PROVISION_DEVICES));
            }
            else
            {
                var pollingArray = new JsonArray();

                //  Get Discovery and Credential Details of Each Device ( Create Context ) :
                if(result.size() <= maxBatchSize){

                    for(var ele : result.getJsonArray(Constants.PROVISION_DEVICES))
                    {
                        var res = new JsonObject(ConfigDB.read(Long.parseLong(ele.toString()),Constants.POLLING));

                        pollingArray.add(res.getJsonObject(Constants.Context));
                    }
                }
                else
                {
                    var cnt =0;
                    while(cnt < maxBatchSize)
                    {
                        var res =  new JsonObject(ConfigDB.read(Long.parseLong(result.getJsonArray(Constants.PROVISION_DEVICES).getString(cnt)),Constants.POLLING));

                        pollingArray.add(res.getJsonObject(Constants.Context));

                        cnt++;
                    }

                }


//                 Check Availability
                 checkAvailability(pollingArray);

                logger.trace("Polling array : {}",pollingArray);

                ///  tAKE OUT THE WHOLE CONTEXT FROM THE REQUEST;
                String encodedContext = Base64.getEncoder().encodeToString(pollingArray.toString().getBytes());

                try
                {

                    var replyJson = Utils.spawnPluginEngine(encodedContext, pollingArray.size());

                    logger.trace("Polled Result : {}", replyJson);

                    if(replyJson != null){
                        for (int i = 0; i < replyJson.size(); i++) {

                            JsonObject jsonObject = new JsonObject(replyJson.getString(i));

                            logger.info("Result of device {} is {} ", i, jsonObject);

                            String status = jsonObject.getString(Constants.STATUS);

                            logger.info("Status of device {} is {} ", i, status);

                            if (status.equals(Constants.SUCCESS)) {

                                JsonObject res1 = jsonObject.getJsonObject(Constants.RESULT);

                                String ip = jsonObject.getString(Constants.IP);

                                // Write result to a file

                                writeResultToFile(ip, res1);

                            } else if (status.equals(Constants.FAILED)) {

                                JsonArray errors = jsonObject.getJsonArray(Constants.ERRORS);

                                // Log errors

                                logErrors(errors);
                            }
                        }
                    }
                }
                catch (Exception exception)
                {

                    logger.error("Error in Spawning PluginEngine....");

                    logger.error(exception.toString());

                    logger.error(exception.getStackTrace().toString());

                    logger.error(exception.getMessage());
                }
            }
        });

        startPromise.complete();
    }

    private static void writeResultToFile(String ip, JsonObject result)
    {
        String fileName = ip + ".json";

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

    private static void logErrors(JsonArray errors)
    {
        for (int i = 0; i < errors.size(); i++)
        {

            JsonObject error = errors.getJsonObject(i);

            String errorMessage = error.getString("Error.Message");

            String errorCode = error.getString("Error.code");

            logger.error("Error code: {} , Message : {} ", errorCode, errorMessage);
        }
    }

    private void checkAvailability(JsonArray pollingArray)
    {
        for(var element : pollingArray)
        {
            var discoveryInfo = new JsonObject(element.toString());

            if (!Utils.checkAvailability(discoveryInfo.getString(Constants.IP) ) )
            {
                pollingArray.remove(element);
            }

        }
    }
}
