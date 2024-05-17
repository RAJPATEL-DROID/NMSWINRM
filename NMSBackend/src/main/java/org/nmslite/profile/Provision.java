package org.nmslite.profile;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Provision extends AbstractVerticle {

    public static final Logger logger = LoggerFactory.getLogger(Provision.class);

    public static JsonObject add(JsonObject request)
    {
        var response = new JsonObject();

        if(request.containsKey(Constants.ID) && !request.getString(Constants.ID).isEmpty()) {

                var message = Constants.PROVISION + Constants.MESSAGE_SEPARATOR + request.getString(Constants.ID);

                var res =new JsonObject(ConfigDB.create(message));

                response.mergeIn(res);

                if (response.containsKey(Constants.ERROR)) {

                    logger.error("Device {} not provisioned",request.getString(Constants.ID));

                    logger.error(response.getString(Constants.ERROR_MESSAGE));

                    response.put(Constants.STATUS, Constants.FAILED);

                } else {

                    response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    response.put(Constants.STATUS, Constants.SUCCESS);

                }

        }
        else
        {
            logger.error("ID is Missing in the Request !!");

            response.put(Constants.ERROR,"No ID Provided")

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE, "Provide Valid Id")

                    .put(Constants.STATUS, Constants.FAILED);

        }

        return response;

    }

}
