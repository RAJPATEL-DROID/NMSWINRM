package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Base64;

import static org.nmslite.utils.RequestType.POLLING;
import static org.nmslite.utils.RequestType.PROVISION;

public class PollingEngine extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(PollingEngine.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        long pollTime  =  Long.parseLong(Utils.config.get(Constants.DEFAULT_POLL_TIME).toString()) * 1000;

        logger.trace("Default Poll time set to {} ", pollTime);

        vertx.setPeriodic(pollTime, tid ->
        {

            var result = ConfigDB.read(PROVISION);

            // We Get Array of Discovery Id of Provisioned Device
            if(result.getJsonArray(Constants.PROVISION_DEVICES).isEmpty())
            {
                logger.trace("No Provisioned Device Found : {}", result.getJsonArray(Constants.PROVISION_DEVICES));
            }
            else
            {
                var pollingArray = new JsonArray();

                //  Get Discovery and Credential Details of Each Device ( Create Context ) :
                for(var id : result.getJsonArray(Constants.PROVISION_DEVICES))
                {

                    var res = ConfigDB.read(POLLING,Long.parseLong(id.toString()));

                    pollingArray.add(res.getJsonObject(Constants.CONTEXT));

                }

              // Check Availability
                checkAvailability(pollingArray);

                logger.trace("Polling array : {}",pollingArray);

                //  tAKE OUT THE WHOLE CONTEXT FROM THE REQUEST;
                String encodedContext = Base64.getEncoder().encodeToString(pollingArray.toString().getBytes());

                try
                {
                    vertx.executeBlocking(future ->
                    {
                        var replyJson = Utils.spawnPluginEngine(encodedContext, pollingArray.size());

                        if(replyJson == null)
                        {
                            future.fail("Process Timeout");
                        }

                        future.complete(replyJson);

                    }).onComplete(handler ->
                    {
                        if(handler.succeeded())
                        {
                            var replyJson = new JsonArray(String.valueOf(handler.result()));

                            logger.trace("Polled Result : {}", replyJson);

                            for (int i = 0; i < replyJson.size(); i++) {

                                JsonObject response = replyJson.getJsonObject(i);

                                logger.info("Result of device {} is {} ", i, response);

                                String ip = response.getString(Constants.IP);
                                String status = response.getString(Constants.STATUS);

                                logger.info("Status of device {} is {} ", i, status);

                                if (status.equals(Constants.SUCCESS)) {

                                    JsonObject pollResult = response.getJsonObject(Constants.RESULT);

                                    // Write result to a file
                                    Utils.writeToFile(ip, pollResult);

                                } else if (status.equals(Constants.FAILED)) {

                                    JsonArray errors = response.getJsonArray(Constants.ERRORS);
                                    for (int j = 0; j < errors.size(); j++)
                                    {

                                        JsonObject error = errors.getJsonObject(j);

                                        Utils.writeToFile(ip,error);

                                    }
                                }
                            }
                        }
                        else
                        {
                            logger.info(handler.cause().getMessage());
                        }
                    });
                }
                catch (Exception exception)
                {

                    logger.error("Error in Spawning PluginEngine....");

                    logger.error(exception.toString());

                    logger.error(Arrays.toString(exception.getStackTrace()));

                    logger.error(exception.getMessage());
                }
            }
        });

        logger.info("Polling Engine Started");
        startPromise.complete();
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
