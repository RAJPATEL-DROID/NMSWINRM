package org.nmslite.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.FileReader;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nmslite.utils.RequestType.POLLING_RESULT;
import static org.nmslite.utils.RequestType.PROVISION;

public class Provision extends AbstractVerticle
{

    public static final Logger logger = LoggerFactory.getLogger(Provision.class);

    private final Router router;

    public Provision()
    {

        this.router = Router.router(Bootstrap.getVertx());
    }

    public void init(Router router)
    {

        router.route(Constants.PROVISION_ROUTE).subRouter(this.router);

        this.router.route().handler(BodyHandler.create());

        this.router.post(Constants.PARAMETER).handler(this::add);

        this.router.get(Constants.ROUTE_PATH).handler(this::getProvisionedDevice);

        this.router.get("/data" + Constants.PARAMETER).handler(this::getData);

    }

    private void getProvisionedDevice(RoutingContext context)
    {

        try
        {

            context.response().setStatusCode(Constants.OK);

            var response = ConfigDB.read(PROVISION);

            response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

            response.put(Constants.STATUS, Constants.SUCCESS);

            context.json(response);
        }
        catch (Exception exception)
        {
            logger.error("Exception occurred while retrieving Provisioned Devices", exception);

            var response = Utils.errorHandler(exception.toString(), Constants.BAD_REQUEST, exception.getMessage());

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);

        }
    }

    private void add(RoutingContext context)
    {

        try
        {
            if (!context.request().getParam(Constants.ID).isEmpty() && !( Long.parseLong(context.request()
                    .getParam(Constants.ID)) < 1 ))
            {

                var request = new JsonObject().put(Constants.ID, Long.parseLong(context.request()
                        .getParam(Constants.ID)));

                var response = ConfigDB.create(PROVISION, request);

                if (response.containsKey(Constants.ERROR))
                {

                        response = Utils.errorHandler("Provision Unsuccessful", Constants.BAD_REQUEST, response.getString(Constants.ERROR));

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                }
                else
                {

                    logger.info("Device Provisioned Successfully");

                    response.put(Constants.MESSAGE, "Device Provisioned Successfully");

                    response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    context.response().setStatusCode(Constants.OK);

                }

                context.json(response);
            }
            else
            {

                logger.error(Constants.MISSING_FIELD);

                var response = Utils.errorHandler(Constants.MISSING_FIELD, Constants.BAD_REQUEST, "Enter Valid Discovery Id");

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);

            }
        }
        catch (Exception exception)
        {
            logger.error("Error Provisioning Device :", exception);

            var response = Utils.errorHandler(exception.toString(), Constants.BAD_REQUEST, exception.getMessage());

            context.response().setStatusCode(Constants.BAD_REQUEST);

            context.json(response);

        }
    }

    private void getData(RoutingContext context)
    {

        try
        {
            if (!context.request().getParam(Constants.ID).isEmpty() && !( Long.parseLong(context.request()
                    .getParam(Constants.ID)) < 1 ))
            {
                if (ConfigDB.discoveryProfiles.containsKey(Long.parseLong(context.request().getParam(Constants.ID))))
                {
                    var ip = ConfigDB.read(POLLING_RESULT, Long.parseLong(context.request().getParam(Constants.ID)));

                    if (ip.containsKey(Constants.STATUS))
                    {
                        var response = Utils.errorHandler("Device not provisioned!", Constants.BAD_REQUEST, "Provision the device first!!");

                        context.response().setStatusCode(Constants.BAD_REQUEST);

                        context.json(response);

                    }
                    else
                    {
                        var ipAddress = ip.getString(Constants.IP);

                        var fileReader = new FileReader();

                        var numLines = 0;

                        if (context.body().asJsonObject().containsKey(Constants.NUM_OF_ROWS))
                        {
                            numLines = context.body().asJsonObject().getInteger(Constants.NUM_OF_ROWS);
                        }
                        else
                        {
                            numLines = 5;
                        }

                        var jsonArray = fileReader.readLastNLinesByIP(ipAddress, numLines);

                        var response = new JsonObject();

                        if (jsonArray != null)
                        {

                            response.put(Constants.STATUS, Constants.SUCCESS).put(Constants.RESULT, jsonArray);

                            context.response().setStatusCode(200).putHeader("Content-Type", "application/json");

                        }
                        else
                        {

                            response = Utils.errorHandler("Failed to fetch Data", Constants.BAD_REQUEST, "Unable to read file");

                            context.response().setStatusCode(Constants.BAD_REQUEST);

                        }

                        context.json(response);
                    }
                }
                else
                {
                    var response = Utils.errorHandler("Invalid Discovery Id Provided", Constants.BAD_REQUEST, "Provide Valid Discovery Id");

                    context.response().setStatusCode(Constants.BAD_REQUEST);

                    context.json(response);

                }

            }
            else
            {
                var response = Utils.errorHandler("Parameter Not Provided", Constants.BAD_REQUEST, "Provide Valid Parameter Value");

                context.response().setStatusCode(Constants.BAD_REQUEST);

                context.json(response);
            }
        }
        catch (Exception exception)
        {

            logger.error("Error :", exception);

            context.response().setStatusCode(500);

            var response = Utils.errorHandler(exception.toString(), Constants.BAD_REQUEST, exception.getMessage());

            context.json(response);
        }

    }

}
