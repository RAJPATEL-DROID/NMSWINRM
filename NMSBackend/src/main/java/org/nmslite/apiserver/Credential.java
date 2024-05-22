package org.nmslite.apiserver;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nmslite.utils.RequestType.CREDENTIAL;

public class Credential {

    private static final Logger logger = LoggerFactory.getLogger(Credential.class);

    private static Router router;

    public  void init(Vertx vertx) {

        router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

    }

    public Router getRouter()
    {
        router.post(Constants.ROUTE_PATH).handler(this::add);

        router.get(Constants.ROUTE_PATH).handler(this::getCredentials);

//        router.get(Constants.PARAMETER).handler(Credential::getCredential);

        return router;
    }

    private void getCredentials(RoutingContext context)
    {
        try
        {
            context.response().setStatusCode(Constants.OK);

            context.json(Utils.getData(CREDENTIAL));
        }
        catch (Exception exception)
        {
            logger.error("Exception occurred while retrieving credentials", exception);

            var response = new JsonObject();

            response.put(Constants.ERROR, exception)

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage())

                    .put(Constants.STATUS, Constants.FAILED);

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);
        }
    }

    private void add(RoutingContext context)
    {
        var data = context.body().asJsonObject();
        try
        {
            if (!data.isEmpty())
            {
                var request = new JsonObject(data.toString());

                if (request.containsKey(Constants.USERNAME) && request.containsKey(Constants.PASSWORD) && request.containsKey(Constants.NAME))
                {

                    if (!request.getString(Constants.USERNAME).isEmpty() && !request.getString(Constants.PASSWORD).isEmpty() && !request.getString(Constants.NAME).isEmpty())
                    {

                        var response = ConfigDB.create(CREDENTIAL,request);

                        if (response.containsKey(Constants.ERROR))
                        {
                            response.put(Constants.STATUS, Constants.FAILED);

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

                        logger.error("Credentials are Invalid !!");
                        var response = new JsonObject();

                        response.put(Constants.ERROR, "Empty Fields")

                                .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                                .put(Constants.ERROR_MESSAGE, "Fields Can't Be Empty")

                                .put(Constants.STATUS, Constants.FAILED);

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                        context.json(response);

                    }
                }
                else
                {
                    logger.error("Credentials are Missing in the Request !!");

                    var error = new JsonObject();

                    var response = new JsonObject();

                    response.put(Constants.ERROR, "No Credentials Provided")

                            .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                            .put(Constants.ERROR_MESSAGE, "Provide Username and Password")

                            .put(Constants.STATUS, Constants.FAILED);

                    context.response().setStatusCode(Constants.BAD_REQUEST);

                    context.json(response);
                }
            }
            else
            {
                var response = new JsonObject();

                response.put(Constants.ERROR, "Invalid JSON Format")

                        .put(Constants.ERROR_CODE, 400)

                        .put(Constants.ERROR_MESSAGE, "Provide Valid JSON Format ")

                        .put(Constants.STATUS, Constants.FAILED);

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error creating credential profile :", exception);

            var response = new JsonObject();

            response.put(Constants.ERROR, "Exception creating credential profile")

                    .put(Constants.ERROR_CODE, 400)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage())

                    .put(Constants.STATUS, Constants.FAILED);

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);
        }
    }

}
