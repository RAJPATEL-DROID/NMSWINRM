package org.nmslite.apiserver;

import io.vertx.core.AbstractVerticle;
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

import static org.nmslite.utils.RequestType.PROVISION;

public class Provision extends AbstractVerticle {

    public static final Logger logger = LoggerFactory.getLogger(Provision.class);

    private static Router router;

    public  void init(Vertx vertx)
    {
        router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

    }

    public Router getRouter()
    {
        router.post(Constants.PARAMETER).handler(this::add);

        router.get(Constants.ROUTE_PATH).handler(this::getProvisionedDevice);

        return router;
    }

    private void getProvisionedDevice(RoutingContext context) {

        try
        {

            context.response().setStatusCode(Constants.OK);

            context.json(Utils.getData(PROVISION));
        }
        catch (Exception exception)
        {
            logger.error("Exception occurred while retrieving Provisioned Devices", exception);

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
        try {
            if (!context.request().getParam(Constants.ID).isEmpty() && !(Long.parseLong(context.request().getParam(Constants.ID)) < 1))
            {

                    var request = new JsonObject().put(Constants.ID, Long.parseLong(context.request().getParam(Constants.ID)));


                    var response = ConfigDB.create(PROVISION ,request);

                    if (response.getString(Constants.STATUS).equals(Constants.SUCCESS)) {

                       logger.info("Device Provisioned Successfully");

                        response.put(Constants.MESSAGE,"Device Provisioned Successfully");

                        response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                        context.response().setStatusCode(Constants.OK);

                    } else {

                        if(response.containsKey(Constants.ERROR))
                        {
                            response.put(Constants.ERROR, "Provide Valid Discovery Id");

                            context.response().setStatusCode(Constants.BAD_REQUEST);

                        }
                        else
                        {
                            response.put(Constants.MESSAGE, "Device is not discovered yet!!");

                            context.response().setStatusCode(Constants.OK);
                        }
                    }
                    context.json(response);
            }
            else
            {
                logger.error(Constants.MISSING_FIELD);
                var response = new JsonObject();

                response.put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR, Constants.MISSING_FIELD)

                        .put(Constants.ERROR_MESSAGE, "Enter Valid Discovery Id as Parameter")

                        .put(Constants.STATUS, Constants.FAILED);

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error Provisioning Device :", exception);

            var response = new JsonObject();

            response.put(Constants.ERROR, "Exception in provisioning device")

                    .put(Constants.ERROR_CODE, 400)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage())

                    .put(Constants.STATUS, Constants.FAILED);

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);

        }
    }


}
