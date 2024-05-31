package org.nmslite.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nmslite.utils.RequestType.VALID_DISCOVERY;

public class ResponseProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ResponseProcessor.class);

    public void receive()
    {
        long pollTime = Long.parseLong(Utils.config.get(Constants.POLL_TIME).toString()) * 1000;

        var vertx = Bootstrap.getVertx();

        new Thread( () ->
        {
            while (true)
            {
                try
                {
                    vertx.eventBus().localConsumer(Constants.DISCOVERY,handler ->
                    {
                        var received= new JsonObject(handler.body().toString()) ;

                        logger.info("Data Received from Device : {}", received);


                        if (received.containsKey(Constants.ERROR))
                        {

                            logger.info("Discovery Run Process Failed {} ", received.getString(Constants.ERROR));

                            logger.info("Error Message : {}", received.getString(Constants.ERROR_MESSAGE));

                        }
                        else
                        {

                            var credentialID = received.getInteger(Constants.CREDENTIAL_ID);

                            if (credentialID.equals(Constants.INVALID_CREDENTIALS))
                            {
                                logger.info("all given credentials are invalid. request: {}", received);

                                logger.info("Discovery Run Process Failed, No Valid Credential ID Found");

                            }
                            else
                            {
                                var validDevice = new JsonObject()

                                        .put(Constants.DISCOVERY_ID, received.getLong(Constants.DISCOVERY_ID))

                                        .put(Constants.CREDENTIAL_ID, credentialID)

                                        .put(Constants.IP, received.getString(Constants.IP));

                                var response = ConfigDB.create(VALID_DISCOVERY, validDevice);


                                if (response.containsKey(Constants.ERROR))
                                {
                                    logger.info("Discovery Run Process Failed {} ", response.getString(Constants.ERROR));

                                }
                                else
                                {
                                    logger.info("Discovery Run Process Success");
                                }
                            }
                        }

                    });

                    Thread.sleep(pollTime);

                }
                catch (Exception exception)
                {
                    logger.error("Exception in Loop");

                    logger.error(exception.toString());

                    logger.error(exception.getMessage());
                }
            }
        }).start();


        new Thread(() -> {
            while(true){
                try {
                    vertx.eventBus().localConsumer(Constants.POLLING,handler ->
                    {
                        var received = new JsonObject(handler.body().toString());

                        String ip = received.getString(Constants.IP);

                        String status = received.getString(Constants.STATUS);

                        logger.trace("Status of device is : {}", status);

                        if (status.equals(Constants.SUCCESS))
                        {

                            JsonObject pollResult = received.getJsonObject(Constants.RESULT);

                            // Write result to a file
                            Utils.writeToFileAsync(ip, pollResult);

                        }
                        else if (status.equals(Constants.FAILED))
                        {

                            JsonArray errors = received.getJsonArray(Constants.ERRORS);
                            for (int j = 0; j < errors.size(); j++) {

                                JsonObject error = errors.getJsonObject(j);

                                Utils.writeToFileAsync(ip, error);
                            }
                        }

                    });
                }
                catch (Exception exception)
                {

                    logger.error("Exception in Storing Poll Data");

                    logger.error(exception.toString());

                    logger.error(exception.getMessage());
                }
            }
        }).start();
    }

}

