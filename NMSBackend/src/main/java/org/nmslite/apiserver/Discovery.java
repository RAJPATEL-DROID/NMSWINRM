package org.nmslite.apiserver;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.nmslite.utils.RequestType.*;

public class Discovery {

    public static final Logger logger = LoggerFactory.getLogger(Discovery.class);

    private static Router router;

    public void init(Vertx vertx) {
        router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

    }

    public Router getRouter() {
        router.post(Constants.ROUTE_PATH).handler(this::add);

        router.get(Constants.ROUTE_PATH).handler(this::getDiscoveries);

//        router.get(Constants.PARAMETER).handler(Discovery::getDiscovery);

        router.post(Constants.RUN).handler(this::discoveryRunRequest);
//
        router.get(Constants.RUN_API_RESULT).handler(this::discoveryRunResult);


        return router;
    }

    private void getDiscoveries(RoutingContext context) {
        try {

            context.response().setStatusCode(Constants.OK);

            context.json(Utils.getData(DISCOVERY));
        }
        catch (Exception exception)
        {
            logger.error("Exception occurred while retrieving discovery profiles", exception);

            var response = new JsonObject();

            response.put(Constants.ERROR, exception)

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage())

                    .put(Constants.STATUS, Constants.FAILED);

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);

        }
    }

    private void add(RoutingContext context) {
        var data = context.body().asJsonObject();

        try {

            if (!data.isEmpty()) {
                var request = new JsonObject(data.toString());

                if (request.containsKey(Constants.IP) && request.containsKey(Constants.DEVICE_PORT)
                        && request.containsKey(Constants.CREDENTIAL_IDS) && request.containsKey(Constants.NAME)
                        && !(request.getLong(Constants.DEVICE_PORT) < 1)) {

                    if (!request.getString(Constants.IP).isEmpty()

                            && !request.getString(Constants.DEVICE_PORT).isEmpty()

                            && !request.getString(Constants.CREDENTIAL_IDS).isEmpty()

                            && !request.getString(Constants.NAME).isEmpty()) {


                        var response = ConfigDB.create(DISCOVERY, request);


                        logger.info("Discovery response: {}", response);

                        if (response.containsKey(Constants.ERROR)) {

                            response.put(Constants.STATUS, Constants.FAILED);

                            context.response().setStatusCode(Constants.BAD_REQUEST);

                        } else {

                            response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                            response.put(Constants.STATUS, Constants.SUCCESS);

                            context.response().setStatusCode(Constants.OK);

                        }
                        context.json(response);

                    } else {
                        logger.error("Invalid Fields in Request !! {}", request);

                        var response = new JsonObject();

                        response.put(Constants.ERROR, "Empty Fields")

                                .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                                .put(Constants.ERROR_MESSAGE, "Fields Can't Be Empty")

                                .put(Constants.STATUS, Constants.FAILED);

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                        context.json(response);
                    }
                } else {

                    logger.error(Constants.MISSING_FIELD);

                    logger.error("Received Request : {}", request);

                    var response = new JsonObject();

                    response.put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                            .put(Constants.ERROR, "Necessary Fields Are Not Provided")

                            .put(Constants.ERROR_MESSAGE, "Enter IP,Device Port and Credential Ids in Proper Format")

                            .put(Constants.STATUS, Constants.FAILED);

                    context.response().setStatusCode(Constants.BAD_REQUEST);

                    context.json(response);
                }
            } else {
                var response = new JsonObject();

                response.put(Constants.ERROR, "Invalid JSON Format")

                        .put(Constants.ERROR_CODE, 400)

                        .put(Constants.ERROR_MESSAGE, "Provide Valid JSON Format ")

                        .put(Constants.STATUS, Constants.FAILED);

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        } catch (Exception exception) {
            logger.error("Error creating discovery profile :", exception);

            var response = new JsonObject();

            response.put(Constants.ERROR, "Exception creating credential profile")

                    .put(Constants.ERROR_CODE, 400)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage())

                    .put(Constants.STATUS, Constants.FAILED);

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);
        }


    }

    private void discoveryRunRequest(RoutingContext context)
    {
        try {
            if (!context.request().getParam(Constants.ID).isEmpty() && !(Long.parseLong(context.request().getParam(Constants.ID)) < 1))
            {
                var entries = ConfigDB.read(DISCOVERY_RUN, Long.parseLong(context.request().getParam(Constants.ID)));

                if (entries.containsKey(Constants.ERROR))
                {
                    context.response().setStatusCode(Constants.BAD_REQUEST);

                    context.json(entries);

                }
                else
                {
                    Vertx vertx = Bootstrap.getVertx();

                    vertx.eventBus().send(Constants.EVENT_RUN_DISCOVERY, entries.getJsonObject(Constants.CONTEXT));

                    var response = new JsonObject();

                    response.put(Constants.STATUS, Constants.SUCCESS)

                            .put(Constants.ERROR_CODE, Constants.SUCCESS_CODE)

                            .put(Constants.MESSAGE, "Request of Discovery Run has been received");

                    context.response().setStatusCode(Constants.OK);

                    context.json(response);

                }
            }
            else
            {

                var response = new JsonObject();

                response.put(Constants.ERROR, "Valid Id Not Provided")

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR_MESSAGE, "Provide Valid Numerical Id")

                        .put(Constants.STATUS, Constants.FAILED);

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error :", exception);

            context.response().setStatusCode(500);

            var response = new JsonObject();

            response.put(Constants.ERROR, "Device Discovery Failed")

                    .put(Constants.ERROR_CODE, 500)

                    .put(Constants.ERROR_MESSAGE, "Internal Server Error")

                    .put(Constants.STATUS, Constants.FAILED);

            context.json(response);
        }
    }

    private void discoveryRunResult(RoutingContext context)
    {
        try
        {
            if (!context.request().getParam(Constants.ID).isEmpty() && !(Long.parseLong(context.request().getParam(Constants.ID)) < 1))
            {

                var response = ConfigDB.read(DISCOVERY_RUN_RESULT, Long.parseLong(context.request().getParam(Constants.ID)));


                if (response.getString(Constants.STATUS).equals(Constants.FAILED))
                {

                    if(response.containsKey(Constants.ERROR))
                    {
                        response.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                        context.json(response);
                    }
                    else
                    {
                        response.put(Constants.MESSAGE, "Device Discovery Failed!!");

                        response.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                        context.response().setStatusCode(Constants.OK);

                        context.json(response);

                    }
                }
                else
                {
                    response.put(Constants.MESSAGE, "Device is discovered successfully!!!");

                    response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    context.response().setStatusCode(Constants.OK);

                    context.json(response);
                }

            }
            else
            {

                var response = new JsonObject();

                response.put(Constants.ERROR, "Valid Id Not Provided")

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR_MESSAGE, "Provide Valid Numerical Id")

                        .put(Constants.STATUS, Constants.FAILED);

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error :", exception);

            logger.error(Arrays.toString(exception.getStackTrace()));

            context.response().setStatusCode(500);

            var response = new JsonObject();

            response.put(Constants.ERROR, "Exception Occurred")

                    .put(Constants.ERROR_CODE, 500)

                    .put(Constants.ERROR_MESSAGE, "Internal Server Error")

                    .put(Constants.STATUS, Constants.FAILED);

            context.json(response);
        }
    }
}
