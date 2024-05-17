package org.nmslite.profile;

import io.vertx.core.json.JsonObject;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Credential {

    private static final Logger logger = LoggerFactory.getLogger(Credential.class);

    public static JsonObject add(JsonObject request)
    {

        var response = new JsonObject();

        if(request.containsKey(Constants.USERNAME) && request.containsKey(Constants.PASSWORD) && request.containsKey(Constants.NAME)) {

            if(!request.getString(Constants.USERNAME).isEmpty() && !request.getString(Constants.PASSWORD).isEmpty() && !request.getString(Constants.NAME).isEmpty())
            {

                var message = Constants.CREDENTIAL + Constants.MESSAGE_SEPARATOR + request;

                var reply = new JsonObject(ConfigDB.create(message));

                response.mergeIn(reply);

                if(response.containsKey(Constants.ERROR))
                {
                    response.put(Constants.STATUS, Constants.FAILED);
                }else
                {
                    response.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    response.put(Constants.STATUS, Constants.SUCCESS);
                }

            }
            else
            {

                logger.error("Credentials are Invalid !!");

                response.put(Constants.ERROR,"Empty Fields")

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR_MESSAGE, "Fields Can't Be Empty")

                        .put(Constants.STATUS, Constants.FAILED);
            }
        }
        else
        {
            logger.error("Credentials are Missing in the Request !!");

            var error  = new JsonObject();

            response.put(Constants.ERROR,"No Credentials Provided")

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE, "Provide Username and Password")

                    .put(Constants.STATUS, Constants.FAILED);

        }

        return response;
    }
    public static JsonObject remove(JsonObject request)
    {
        var response = new JsonObject();

        if(!request.getString(Constants.ID).isEmpty())
        {

                var message = Constants.CREDENTIAL + Constants.MESSAGE_SEPARATOR + request.getLong(Constants.ID);

                var reply = new JsonObject(ConfigDB.delete(message));

                response.mergeIn(reply);

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

                logger.error("Credential Id Field is Empty !!");

                response.put(Constants.ERROR,"Empty Fields")

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                        .put(Constants.ERROR_MESSAGE, "Fields Can't Be Empty")

                        .put(Constants.STATUS, Constants.FAILED);
        }

        return response;
    }
}
