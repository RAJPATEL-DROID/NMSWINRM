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

            var response = Utils.errorHandler(exception.toString(),Constants.BAD_REQUEST,exception.getMessage());

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
                        && !(request.getLong(Constants.DEVICE_PORT) < 1))
                {

                    if (!request.getString(Constants.IP).isEmpty()

                            && !request.getString(Constants.DEVICE_PORT).isEmpty()

                            && !request.getString(Constants.CREDENTIAL_IDS).isEmpty()

                            && !request.getString(Constants.NAME).isEmpty()) {


                        var response = ConfigDB.create(DISCOVERY, request);


                        logger.info("Discovery response: {}", response);

                        if (response.containsKey(Constants.STATUS))
                        {

                            response = Utils.errorHandler("Discovery Profile Not Created",Constants.BAD_REQUEST,"Provide Correct Discovery Name and Credential Ids");

                            context.response().setStatusCode(Constants.BAD_REQUEST);

                        }
                        else
                        {

                            response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                            response.put(Constants.STATUS, Constants.SUCCESS);

                            context.response().setStatusCode(Constants.OK);

                        }
                        context.json(response);

                    }
                    else
                    {
                        logger.error("Invalid Fields in Request !! {}", request);

                        var response = Utils.errorHandler( "Empty Fields",Constants.BAD_REQUEST,"Fields Can't Be Empty");

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                        context.json(response);
                    }
                }
                else
                {

                    logger.error(Constants.MISSING_FIELD);

                    logger.error("Received Request : {}", request);

                    var response = Utils.errorHandler("Necessary Fields Are Not Provided",Constants.BAD_REQUEST,"Enter IP,Device Port and Credential Ids in Proper Format");

                    context.response().setStatusCode(Constants.BAD_REQUEST);

                    context.json(response);
                }
            }
            else
            {
                var response = Utils.errorHandler("Invalid JSON Format",Constants.BAD_REQUEST,"Provide Valid JSON Format");

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        } catch (Exception exception) {
            logger.error("Error creating discovery profile :", exception);

            var response = Utils.errorHandler(exception.toString(),Constants.BAD_REQUEST,exception.getMessage());

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
                    var response = Utils.errorHandler("Invalid Discovery id",Constants.BAD_REQUEST,"Provide valid discovery id");

                    context.response().setStatusCode(Constants.BAD_REQUEST);

                    context.json(response);

                }
                else
                {
                    Vertx vertx = Bootstrap.getVertx();

                    vertx.eventBus().send(Constants.EVENT_RUN_DISCOVERY, entries.getJsonObject(Constants.CONTEXT));

                    var response = new JsonObject();

                    response.put(Constants.MESSAGE, "Request of Discovery Run has been received")

                            .put(Constants.STATUS, Constants.SUCCESS)

                            .put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    context.response().setStatusCode(Constants.OK);

                    context.json(response);

                }
            }
            else
            {

                var response = Utils.errorHandler("Valid Id Not Provided",Constants.BAD_REQUEST,"Provide Valid Numerical Id");

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error :", exception);

            context.response().setStatusCode(500);

            var response = Utils.errorHandler(exception.toString(),Constants.BAD_REQUEST,exception.getMessage());

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
                        response = Utils.errorHandler("Invalid Discovery Id", Constants.BAD_REQUEST,"Provide Valid Discovery Id");

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                        context.json(response);
                    }
                    else
                    {
                        response = Utils.errorHandler("Device Discovery Failed",Constants.BAD_REQUEST,"Device Not Discovered");

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

                var response = Utils.errorHandler("Valid Id Not Provided",Constants.BAD_REQUEST,"Provide Valid Numerical Id");

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error :", exception);

            logger.error(Arrays.toString(exception.getStackTrace()));

            context.response().setStatusCode(500);

            var response = Utils.errorHandler(exception.toString(),Constants.BAD_REQUEST,exception.getMessage());

            context.json(response);
        }
    }
}
