package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nmslite.Bootstrap;
import org.nmslite.profile.Credential;
import org.nmslite.profile.Discovery;
import org.nmslite.profile.Provision;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class APIEngine extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(APIEngine.class);



    @Override
    public void start(Promise<Void> startPromise) {

        int port = (int) Utils.config.get(Constants.HTTP_PORT);

        String hostname = (String) Utils.config.get(Constants.HOST);

        logger.info("Starting server on port {}", port);

        logger.info("Starting server on host {}", hostname);

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);


        //------------------------------- Home Route ----------------------------------------------------//

        router.route("/home").handler(ctx ->
                ctx.response().end("Welcome to NMSLite")
        );


        //-------------------------------------Credential Profile APIs------------------------------------//

        router.post(Constants.CREDENTIAL_ROUTE).handler(ctx -> ctx.request().bodyHandler(buffer ->
        {
            try
            {
                if (buffer != null && buffer.length() > 0)
                {
                    var request = new JsonObject(buffer.toString());

                    var response = Credential.add(request);

                    if (response.getString(Constants.ERROR) != null)
                    {

                        ctx.response().setStatusCode(response.getInteger(Constants.ERROR_CODE));

                    }
                    else
                    {

                        ctx.response().setStatusCode(Constants.OK);

                    }

                    ctx.json(response);

                }
                else
                {
                    var response = errorHandler(ctx);


                    ctx.response().setStatusCode(Constants.BAD_REQUEST);

                    ctx.json(response);
                }
            }
            catch (Exception exception)
            {
                logger.error("Error creating credential profile :", exception);

                var response = errorHandler(ctx);

                ctx.response().setStatusCode(Constants.BAD_REQUEST);

                ctx.json(response);
            }

        }));

        router.get(Constants.CREDENTIAL_ROUTE).handler(ctx ->
        {
            var result = Utils.getData(Constants.CREDENTIAL);

            ctx.response().setStatusCode(Constants.OK);

            ctx.json(result);
        });

        router.delete(Constants.CREDENTIAL_DELETE_ROUTE).handler(ctx ->
        {
            if (!ctx.request().getParam(Constants.ID).isEmpty())
            {

                var request = new JsonObject().put(Constants.ID, Long.parseLong(ctx.request().getParam(Constants.ID )));

                var result = Credential.remove(request);

                if (result.getString(Constants.ERROR) != null)
                {

                    ctx.response().setStatusCode(result.getInteger(Constants.ERROR_CODE));

                }
                else
                {

                    ctx.response().setStatusCode(Constants.OK);

                }
                ctx.json(result);

            } else
            {

                var response = new JsonObject();

                response.put(Constants.ERROR, "Id not found in Parameter")

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR_MESSAGE, "Provide Valid Credential Id to Delete Credential ")

                        .put(Constants.STATUS, Constants.FAILED);

                ctx.response().setStatusCode(Constants.BAD_REQUEST);

                ctx.json(response);
            }

        });


        //-----------------------------------------Discovery Profile APIs-------------------------------------//

        router.post(Constants.DISCOVERY_ROUTE).handler(ctx -> ctx.request().bodyHandler(buffer ->
        {
            try {
                if (buffer != null && buffer.length() > 0)
                {

                    var request = new JsonObject(buffer.toString());

                    var response = Discovery.add(request);

                    if (response.getString(Constants.ERROR) != null) {

                        ctx.response().setStatusCode(response.getInteger(Constants.ERROR_CODE));

                    } else {

                        ctx.response().setStatusCode(Constants.OK);

                    }

                    ctx.json(response);


                }
                else
                {

                    var response = errorHandler(ctx);

                    ctx.response().setStatusCode(Constants.BAD_REQUEST);

                    ctx.json(response);

                }
            }
            catch (Exception exception)
            {
                logger.error("Error creating discovery profile :", exception);

                var response = errorHandler(ctx);

                ctx.response().setStatusCode(Constants.BAD_REQUEST);

                ctx.json(response);
            }
        }));


        router.get(Constants.DISCOVERY_ROUTE).handler(ctx ->
        {
            var result = Utils.getData(Constants.DISCOVERY);

            ctx.response().setStatusCode(Constants.OK);

            ctx.json(result);
        });

        router.delete(Constants.DISCOVERY_DELETE_ROUTE).handler(ctx ->
        {
            if (!ctx.request().getParam(Constants.ID).isEmpty())
            {

                var request = new JsonObject().put(Constants.ID, Long.parseLong(ctx.request().getParam(Constants.ID)));

                var result = Discovery.remove(request);

                if (result.getString(Constants.ERROR) != null)
                {

                    ctx.response().setStatusCode(result.getInteger(Constants.ERROR_CODE));

                }
                else
                {

                    ctx.response().setStatusCode(Constants.OK);

                }
                ctx.json(result);

            }
            else
            {

                var response = new JsonObject();

                response.put(Constants.ERROR, "Id not found in Parameter")

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR_MESSAGE, "Provide Valid Credential Id to Delete Credential ")

                        .put(Constants.STATUS, Constants.FAILED);

                ctx.response().setStatusCode(Constants.BAD_REQUEST);

                ctx.json(response);

            }
        });


        //-------------------------------------------Discovery Run Route--------------------------------------//

        router.post(Constants.DISCOVERY_RUN_ROUTE).handler(ctx ->
        {
            try
            {
                if (!ctx.request().getParam(Constants.ID).isEmpty())
                {

                    var request = new JsonObject().put(Constants.ID, Long.parseLong(ctx.request().getParam(Constants.ID)));

                    Bootstrap.vertx.eventBus().request(Constants.DISCOVERY_RUN, request, res ->
                    {

                        if (res.succeeded()) {

                            ctx.response().setStatusCode(200);

                            ctx.json(res.result().body());
                        }
                        else
                        {
                            ctx.response().setStatusCode(Constants.BAD_REQUEST);

                            ctx.json(new JsonObject(res.cause().getMessage()));

                        }
                    });

                } else
                {

                    var response = new JsonObject();

                    response.put(Constants.ERROR, "Id not found in Parameter")

                            .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                            .put(Constants.ERROR_MESSAGE, "Provide Valid Discovery Id to Run Discovery ")

                            .put(Constants.STATUS, Constants.FAILED);

                    ctx.response().setStatusCode(Constants.BAD_REQUEST);

                    ctx.json(response);

                }

            }
            catch (Exception exception)
            {
                logger.error("Error :", exception);

                logger.error(Arrays.toString(exception.getStackTrace()));

                ctx.response().setStatusCode(500);
                var response = new JsonObject();

                response.put(Constants.ERROR, "Device Discovery Failed")

                        .put(Constants.ERROR_CODE, 500)

                        .put(Constants.ERROR_MESSAGE, "Internal Server Error")

                        .put(Constants.STATUS, Constants.FAILED);
                ctx.json(response);
            }

        });


        //-------------------------------------------- Provision APIs-----------------------------------------//

        router.post(Constants.PROVISION_ROUTE).handler(ctx ->
        {
            try {
                if (!ctx.request().getParam(Constants.ID).isEmpty())
                {

                    var request = new JsonObject().put(Constants.ID, Long.parseLong(ctx.request().getParam(Constants.ID)));

                    var response = Provision.add(request);

                    if (response.getString(Constants.ERROR) != null)
                    {

                        ctx.response().setStatusCode(response.getInteger(Constants.ERROR_CODE));

                    }
                    else
                    {

                        ctx.response().setStatusCode(Constants.OK);

                    }

                    ctx.json(response);

                } else
                {

                    var response = errorHandler(ctx);

                    ctx.response().setStatusCode(Constants.BAD_REQUEST);

                    ctx.json(response);

                }

            }
            catch (Exception exception)
            {
                logger.error("Error Provisioning Device :", exception);

                var response = errorHandler(ctx);

                ctx.response().setStatusCode(Constants.BAD_REQUEST);

                ctx.json(response);
            }

        });

        router.get(Constants.GET_PROVISION_ROUTE).handler(ctx ->
        {
            var result = Utils.getData(Constants.PROVISION);

            ctx.response().setStatusCode(Constants.OK);

            ctx.json(result);

        });

        //------------------------------------------ Server Configuration and Listener------------------------//
        server.requestHandler(router).listen(port, hostname).onComplete(res -> {

            if (res.succeeded()) {

                logger.info("Server is now listening");

                startPromise.complete();
            } else {

                logger.error("Failed to bind!");

                startPromise.fail(res.cause());
            }
        });

    }

    private JsonObject errorHandler(RoutingContext ctx) {

        var response = new JsonObject();

        response.put(Constants.ERROR, "Invalid JSON Format")

                .put(Constants.ERROR_CODE, 400)

                .put(Constants.ERROR_MESSAGE, "Provide Valid JSON Format ")

                .put(Constants.STATUS, Constants.FAILED);

        return response;
    }

}
