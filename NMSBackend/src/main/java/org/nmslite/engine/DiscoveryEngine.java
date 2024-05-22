package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static org.nmslite.utils.RequestType.VALID_DISCOVERY;

public class DiscoveryEngine extends AbstractVerticle
{
    public static final Logger logger = LoggerFactory.getLogger(DiscoveryEngine.class);

    @Override
    public void start(Promise<Void> startPromise)
    {

        EventBus eventBus = vertx.eventBus();

        eventBus.localConsumer(Constants.EVENT_RUN_DISCOVERY, msg ->
        {
            try
            {
                logger.trace("Contexts Received : {} " , msg.body().toString());

                var discoveryContext = new JsonObject(msg.body().toString());

                // Check Availability of Device
                var discoveryInfo = discoveryContext.getJsonObject(Constants.DISCOVERY_DATA);

                vertx.executeBlocking(future ->
                {

                    if (!Utils.checkAvailability(discoveryInfo.getString(Constants.IP)))
                    {
                        logger.info("Device is not reachable {}", discoveryInfo.getString(Constants.IP));

                    }
                    else
                    {
                        future.complete();
                    }
                }).onComplete(handler ->
                        {
                            if (handler.succeeded())
                            {

                                var context = Utils.createContext(discoveryContext, Constants.DISCOVERY, logger);

                                logger.trace("Received Context Array from the Util : {}", context);

                                if (!context.isEmpty())
                                {

                                    var count = context.size();

                                    var message = context.toString();

                                    var encodedString = Base64.getEncoder().encodeToString(message.getBytes());

                                    vertx.executeBlocking(future ->
                                    {
                                        var replyJson = Utils.spawnPluginEngine(encodedString, count);

                                        if (replyJson == null) {
                                            future.fail("Process timed out");
                                        } else {
                                            future.complete(replyJson);
                                        }

                                    }).onComplete(deviceStatus ->
                                    {
                                        if (deviceStatus.succeeded())
                                        {
                                            var replyJson = new JsonArray(String.valueOf(deviceStatus.result()));

                                            logger.info("Data Received from Device : {}", replyJson);

                                            var result = replyJson.getJsonObject(0);

                                            if (result.containsKey(Constants.ERROR))
                                            {

                                                logger.info("Discovery Run Process Failed {} ", result.getString(Constants.ERROR));

                                                logger.info("Error Message : {}", result.getString(Constants.ERROR_MESSAGE));

                                            }
                                            else
                                            {

                                                var credentialID = result.getLong(Constants.CREDENTIAL_ID);

                                                if (credentialID < 1)
                                                {
                                                    logger.info("all given credentials are invalid. request: {}", context);

                                                    logger.info("Discovery Run Process Failed, No Valid Credential ID Found");

                                                }
                                                else
                                                {
                                                    var validDevice = new JsonObject()

                                                            .put(Constants.DISCOVERY_ID, discoveryContext.getLong(Constants.ID))

                                                            .put(Constants.CREDENTIAL_ID, credentialID)

                                                            .put(Constants.IP, result.getString(Constants.IP));

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
                                        } else {
                                            logger.info("Discovery Run Process Failed {}", deviceStatus.cause().getMessage());
                                        }
                                    });
                                }
                                else
                                {

                                    logger.error("Error in creating context!!!");

                                }
                            }
                            else
                            {
                                logger.info("Device Discovery Failed");
                            }
                        }
                );
            }
            catch (Exception exception)
            {
                logger.error(exception.toString());
            }
        });

        logger.info("Discovery Engine Started...");

        startPromise.complete();

    }

}
