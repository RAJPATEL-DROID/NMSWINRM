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

import static org.nmslite.utils.RequestType.VALID_DISCOVERY;

public class ResponseProcessor extends AbstractVerticle
{

    private static final Logger logger = LoggerFactory.getLogger(ResponseProcessor.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        discovery();

        polling();

        logger.info("Response Processor Verticle Deployed...");

        startPromise.complete();
    }

    // Receive Data of Discovery Run from ZMQRouter
    private void discovery()
    {
        try
        {
            vertx.eventBus().<JsonObject>localConsumer(Constants.DISCOVERY, handler ->
            {
                var received = handler.body();

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
        }
        catch (Exception exception)
        {
            logger.error("Exception in Loop");

            logger.error(exception.toString());

            logger.error(exception.getMessage());
        }

    }

    // Receive Poll Data from the ZMQRouter
    private void polling()
    {
        try
        {
            vertx.eventBus().<JsonObject>localConsumer(Constants.POLLING, handler ->
            {
                var received = handler.body();

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
                    for (int j = 0; j < errors.size(); j++)
                    {

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

}

