package org.nmslite.profile;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryRun extends AbstractVerticle {

    public static final Logger logger = LoggerFactory.getLogger(DiscoveryRun.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        EventBus eb = Bootstrap.vertx.eventBus();
        eb.localConsumer(Constants.DISCOVERY_RUN, msg ->
        {

            try {
                var response = new JsonObject();

                var request = new JsonObject(msg.body().toString());

                if (!request.getString(Constants.ID).isEmpty())
                {
                    // Get the Required Data from DB
                    var entries = new JsonObject(ConfigDB.read(request.getLong(Constants.ID), Constants.DISCOVERY_RUN));

                    if (!entries.containsKey(Constants.ERROR)) {

                        logger.trace("Context Details : {} " , entries);

                        var contextArray = entries.getJsonArray(Constants.Context);

                        for (var targets : contextArray)
                        {

                            // Check Availability of Device
                            var targetData = new JsonObject(targets.toString());
//
                            var discoveryInfo = targetData.getJsonObject(Constants.DISCOVERY_DATA);

                            if (!Utils.checkAvailability(discoveryInfo.getString(Constants.IP) ) )
                            {

                                response.put(Constants.ERROR, "Device Discovery Failed");

                                response.put(Constants.ERROR_MESSAGE, "Device is Unreachable");

                                msg.fail(400, response.toString());

                            }
                            else
                            {

                                var context = Utils.createContext(entries.getJsonArray(Constants.Context), Constants.DISCOVERY, logger);


                                logger.trace("Received Context from the Util : {}", context);

                                if (!context.isEmpty()) {

                                    var jsonObj = new JsonObject();

                                    jsonObj.put("context.size", context.size());

                                    jsonObj.put("context", context.toString());

                                    eb.request(Constants.EVENT_RUN_DISCOVERY, jsonObj.toString(), res ->
                                    {
                                        if (res.succeeded()) {

                                            if(res.result().body().toString().isEmpty()){

                                                response.put(Constants.ERROR, "Device Discovery Failed")

                                                        .put(Constants.ERROR_CODE, 501)

                                                        .put(Constants.ERROR_MESSAGE, "Process Timed Out,Try Again Later")

                                                        .put(Constants.STATUS, Constants.FAILED);

                                                msg.fail(501, response.toString());
                                            }

                                            var results = new JsonArray(res.result().body().toString());
                                            // As One Device is discovered, but output format is in JsonArray
                                            var result = new JsonObject(results.getString(0));


                                            if (result.containsKey(Constants.ERROR)) {

                                                response.mergeIn(result);

                                                msg.fail(501, response.toString());

                                            }
                                            else
                                            {

                                                var credentialID = result.getString(Constants.CREDENTIAL_ID);

                                                if (credentialID.equals(Constants.INVALID_CREDENTIALS))
                                                {
                                                    logger.trace(String.format("all given credentials are invalid. request: %s", context));

                                                    response.put(Constants.STATUS, Constants.FAILED).put(Constants.ERROR_MESSAGE, "no valid credential profile is present");

                                                    response.put(Constants.ERROR_CODE, 400);

                                                    msg.fail(501, response.toString());

                                                }
                                                else
                                                {

                                                    // Store the discovery Id with Correct Credential ID

                                                    var object = new JsonObject()

                                                            .put(Constants.DISCOVERY_ID, request.getString(Constants.ID) )

                                                            .put(Constants.CREDENTIAL_ID, credentialID);

                                                    var validDiscoveryRequest = Constants.VALID_DISCOVERY + Constants.MESSAGE_SEPARATOR + object.toString();

                                                    var reply = new JsonObject(ConfigDB.create(validDiscoveryRequest));

                                                    response.mergeIn(reply);


                                                    if (response.containsKey(Constants.ERROR)) {

                                                        response.put(Constants.STATUS, Constants.FAILED);

                                                    } else {

                                                        response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                                                        response.put(Constants.STATUS, Constants.SUCCESS);

                                                    }

                                                    msg.reply(response);
                                                }
                                            }
                                        }else{

                                            response.put(Constants.STATUS, Constants.FAILED);

                                            var failureMsg = new JsonObject(res.cause().getMessage());

                                            response.put(Constants.ERROR, failureMsg.getJsonObject(Constants.ERROR));
                                        }

                                    });

                                } else {

                                    response.put(Constants.ERROR, "Error Generating Context!!")

                                            .put(Constants.ERROR_CODE, 500)

                                            .put(Constants.ERROR_MESSAGE, "Try Again After Some Time")

                                            .put(Constants.STATUS, Constants.FAILED);

                                    msg.fail(501, response.toString());
                                }
                            }

                        }
                    }
                    else
                    {
                        // Merge Errors Received from ConfigDB.get()
                        response.mergeIn(entries);

                        msg.fail(501, response.toString());
                    }

                }
                else
                {
                    logger.error("Empty Fields in Request !! {}", request);


                    response.put(Constants.ERROR, "NO Discovery ID")

                            .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                            .put(Constants.ERROR_MESSAGE, "Provide discovery ID")

                            .put(Constants.STATUS, Constants.FAILED);

                    msg.fail(400, response.toString());
                }

            }
            catch (Exception exception)
            {
                logger.error(exception.getMessage(), exception);

                msg.fail(500, exception.getMessage());

            }

            });


        }

    }
