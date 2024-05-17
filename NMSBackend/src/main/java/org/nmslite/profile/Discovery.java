package org.nmslite.profile;

import io.vertx.core.json.JsonObject;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Discovery {

    public static final Logger logger = LoggerFactory.getLogger(Discovery.class);

    public static JsonObject add(JsonObject request)
    {

        var response = new JsonObject();

        if (request.containsKey(Constants.IP) && request.containsKey(Constants.DEVICE_PORT) && request.containsKey(Constants.CREDENTIAL_IDS) && request.containsKey(Constants.NAME)) {

            if (!request.getString(Constants.IP).isEmpty()

                    && !request.getString(Constants.DEVICE_PORT).isEmpty()

                    && !request.getString(Constants.CREDENTIAL_IDS).isEmpty()

                    && !request.getString(Constants.NAME).isEmpty() )
            {

                var message = Constants.DISCOVERY + Constants.MESSAGE_SEPARATOR + request;

                var reply = new JsonObject(ConfigDB.create(message));

                response.mergeIn(reply);

                logger.info("Discovery response: {}", response);

                if (reply.containsKey(Constants.ERROR)) {

                    response.put(Constants.STATUS, Constants.FAILED);

                } else {

                    response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    response.put(Constants.STATUS, Constants.SUCCESS);

                }


            } else {
                logger.error("Invalid Fields in Request !! {}", request);


                response.put(Constants.ERROR, "Empty Fields")

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR_MESSAGE, "Fields Can't Be Empty")

                        .put(Constants.STATUS, Constants.FAILED);
            }
        } else {

            logger.error("Fields are Missing in the Request !!");

            response.put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR, "Necessary Fields Are Not Provided")

                    .put(Constants.ERROR_MESSAGE, "Enter IP,Password and Credential Id")

                    .put(Constants.STATUS, Constants.FAILED);

        }

        return response;
    }

    public static JsonObject remove(JsonObject request)
    {
        var response = new JsonObject();

        if(!request.getString(Constants.ID).isEmpty())
        {

            var message = Constants.DISCOVERY + Constants.MESSAGE_SEPARATOR + request.getLong(Constants.ID);

            var res = new JsonObject(ConfigDB.delete(message));

            response.mergeIn(res);

            if(!response.containsKey(Constants.ERROR)){

                response.put(Constants.STATUS, Constants.SUCCESS);
            }
            else
            {
                response.put(Constants.STATUS, Constants.FAILED);
            }
        }
        else
        {

            logger.error("Discovery Id Field is Empty !!");

            response.put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR,"Empty Fields")

                    .put(Constants.ERROR_MESSAGE, "Fields Can't Be Empty")

                    .put(Constants.STATUS, Constants.FAILED);
        }

        return response;
    }

}
