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

            var response = Utils.errorHandler(exception.toString(),Constants.BAD_REQUEST,exception.getMessage());

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

                        if (response.containsKey(Constants.STATUS))
                        {
                            response = Utils.errorHandler("Credential Profile Not Created",Constants.BAD_REQUEST,"Credential with Name " + request.getString(Constants.NAME) + " already exists");

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

                        response = Utils.errorHandler("Empty Fields",Constants.BAD_REQUEST,"Fields Can't Be Empty");

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                        context.json(response);

                    }
                }
                else
                {
                    logger.error("Credentials are Missing in the Request !!");

                    var response = Utils.errorHandler("No Credentials Provided",Constants.BAD_REQUEST, "Provide Username and Password");

                    context.response().setStatusCode(Constants.BAD_REQUEST);

                    context.json(response);
                }
            }
            else
            {
                var response = Utils.errorHandler("Invalid JSON Format",Constants.BAD_REQUEST, "Provide Valid JSON Format");

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error creating credential profile :", exception);

            var response = Utils.errorHandler(exception.toString(),Constants.BAD_REQUEST,exception.getMessage());

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);
        }
    }

}
