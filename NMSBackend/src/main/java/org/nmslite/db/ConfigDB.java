package org.nmslite.db;

import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.utils.Constants;
import org.nmslite.utils.RequestType;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;


public class ConfigDB {
    private ConfigDB() {
    }


    private static final Logger logger = LoggerFactory.getLogger(ConfigDB.class);

    public static final ConcurrentHashMap<Long, JsonObject> credentialsProfiles = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<Long, JsonObject> discoveryProfiles = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, JsonObject> validDevices = new ConcurrentHashMap<>();

    private static final ConcurrentHashSet<Long> provisionedDevices = new ConcurrentHashSet<>();

    public static JsonObject create(RequestType type, JsonObject request) {

        var reply = new JsonObject();

        logger.trace("Create Request for : {}", type );

        try {
            switch (type)
            {

                case CREDENTIAL ->
                {

                    // check if credential with same name exists
                    for (var credential : credentialsProfiles.values())
                    {
                        if (credential.getString(Constants.NAME).equals(request.getString(Constants.NAME)))
                        {
                            reply.put(Constants.STATUS, Constants.FAILED);

                            return reply;
                        }
                    }

                    // If execution is here means credentials are new
                    var id = Utils.getId();

                    credentialsProfiles.put(id, request);

                    reply.put(Constants.CREDENTIAL_ID, id);

                }
                case DISCOVERY ->
                {

                    var credentialArray = request.getJsonArray(Constants.CREDENTIAL_IDS);

                    for (Object credentialId : credentialArray)
                    {
                        if (!credentialsProfiles.containsKey(Long.parseLong(credentialId.toString())))
                        {
                            reply.put(Constants.STATUS, Constants.FAILED);

                            return reply;
                        }
                    }

                    for (var discovery : discoveryProfiles.values())
                    {
                        if (discovery.getString(Constants.NAME).equals(request.getString(Constants.NAME)))
                        {

                            reply.put(Constants.STATUS,Constants.FAILED);

                            return reply;
                        }

                    }

                    var id = Utils.getId();

                    discoveryProfiles.put(id, request);

                    reply.put(Constants.DISCOVERY_ID, id);


                }
                case VALID_DISCOVERY ->
                {

                    logger.trace(request.toString());

                    var discoveryID = request.getLong(Constants.DISCOVERY_ID);

                    logger.trace("Discovery Id done");

                    var ip = request.getString(Constants.IP);

                    request.remove(Constants.IP);


                    if (!validDevices.containsKey(ip))
                    {
                        validDevices.put(ip,request);

                        logger.info(validDevices.toString());

                    }
                    else
                    {
                        logger.info("Device with Same IP Address already exists");

                        reply.put(Constants.ERROR,"Device with Same IP Address already exists");

                        return reply;

                    }

                    logger.info("For Device IP {} , credential id {} is valid", ip, validDevices.get(ip).getString(Constants.CREDENTIAL_ID));

                    reply.put(Constants.MESSAGE, "Device Discovered Successfully");

                }
                case PROVISION ->
                {

                    var id = request.getLong(Constants.ID);

                    // check if the discovery id is present in any valid devices
                    if(discoveryProfiles.containsKey(id))
                    {
                        for(var device : validDevices.values())
                        {
                            if(device.getLong(Constants.DISCOVERY_ID).equals(id))
                            {
                                if(!provisionedDevices.contains(id))
                                {
                                    provisionedDevices.add(id);

                                    logger.trace("Device provisioned successfully for {}", id);

                                }
                                else
                                {
                                    reply.put(Constants.STATUS,Constants.FAILED);

                                    reply.put(Constants.ERROR,"Device already provisioned");

                                    logger.trace("Device already provisioned successfully for {}", id);

                                }
                                return reply;
                            }
                        }
                        reply.put(Constants.STATUS, Constants.FAILED);
                    }
                    else
                    {
                        reply.put(Constants.ERROR, "Invalid Discovery Id");

                        reply.put(Constants.STATUS, Constants.FAILED);
                    }
                }
                default ->
                {

                    logger.error("Invalid Create Request Type in Database");

                    reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE)
                            .put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                }
            }
        }
        catch (Exception exception)
        {

            logger.error("Error while inserting discovery ", exception);

            reply.put(Constants.ERROR, exception.toString())

                    .put(Constants.ERROR_CODE, 400)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage());
        }

        return reply;

    }

    public static JsonObject read(RequestType request)
    {

        var reply = new JsonObject();

        logger.info("Read  Request for type {}", request);

        switch (request)
        {
            case CREDENTIAL ->
            {
                JsonArray credentialObjects = new JsonArray();

                for (var id : credentialsProfiles.keySet())
                {

                    credentialObjects.add(new JsonObject().put(id.toString(), credentialsProfiles.get(id)));

                }

                reply.put(Constants.CREDENTIAL_IDS, credentialObjects);
            }
            case DISCOVERY ->
            {
                JsonArray discoveryObjects = new JsonArray();

                for (var id : discoveryProfiles.keySet())
                {
                    discoveryObjects.add(new JsonObject().put(id.toString(), discoveryProfiles.get(id)));

                }
                reply.put(Constants.DISCOVERY_IDS, discoveryObjects);
            }
            case PROVISION ->
            {

                JsonArray provisionedMonitors = new JsonArray();

                for (var id : provisionedDevices)
                {
                    provisionedMonitors.add(id);
                }

                reply.put(Constants.PROVISION_DEVICES, provisionedMonitors);


            }
            default ->
            {

                logger.error("Invalid Read Request Type in Database");

                reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE)

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST);
            }
        }
        return reply;
    }

    public static JsonObject read( RequestType type,Long id)
    {

        var reply = new JsonObject();

        logger.trace("Read  Request for type {} of id {}", type, id);

        switch (type) {
            case DISCOVERY_RUN ->
            {

                if (discoveryProfiles.containsKey(id))
                {

                    var contextData = new JsonObject();

                    var discovery = discoveryProfiles.get(id);

                    var credentials = new JsonArray();

                    for (var credentialId : discovery.getJsonArray(Constants.CREDENTIAL_IDS))
                    {

                        var credDetails = credentialsProfiles.get(Long.parseLong(credentialId.toString()));

                        credDetails.put(Constants.CREDENTIAL_ID, credentialId);

                        credentials.add(credDetails);

                    }

                    contextData.put(Constants.DISCOVERY_DATA, discovery).put(Constants.CREDENTIAL_PROFILES, credentials);

                    contextData.put(Constants.ID, id);

                    reply.put(Constants.CONTEXT, contextData);

                }
                else
                {
                    reply.put(Constants.STATUS, Constants.FAILED);

                    logger.error("Invalid Discovery Id {}", reply);
                }

            }
            case DISCOVERY_RUN_RESULT ->
            {

                if(discoveryProfiles.containsKey(id))
                {
                    for(var device : validDevices.values())
                    {
                        if(device.getLong(Constants.DISCOVERY_ID).equals(id))
                        {
                            logger.info("got the match of discovery id");

                            reply.put(Constants.STATUS, Constants.SUCCESS);

                            return reply;
                        }
                    }
                    reply.put(Constants.STATUS, Constants.FAILED);
                }
                else
                {
                    reply.put(Constants.ERROR, "Invalid Discovery Id");

                    reply.put(Constants.STATUS, Constants.FAILED);
                }

                return reply;
            }
            case POLLING ->
            {
                var details = new JsonObject();

                details.put(Constants.IP, discoveryProfiles.get(id).getString(Constants.IP))
                        .put(Constants.DEVICE_PORT, discoveryProfiles.get(id).getString(Constants.DEVICE_PORT));


                for(var device : validDevices.values())
                {
                    if(device.getLong(Constants.DISCOVERY_ID).equals(id))
                    {
                        var credentialDetails = credentialsProfiles.get(device.getLong(Constants.CREDENTIAL_ID));

                        details.put(Constants.USERNAME, credentialDetails.getString(Constants.USERNAME))
                                .put(Constants.PASSWORD, credentialDetails.getString(Constants.PASSWORD));

                    }
                }

                details.put(Constants.REQUEST_TYPE, Constants.POLLING);

                reply.put(Constants.CONTEXT, details);

                logger.trace("Read  Request for type {} of id {} performed, sending details {}", type, id,details);
            }
            case POLLING_RESULT ->
            {
                if(provisionedDevices.contains(id))
                {
                    reply.put(Constants.IP,discoveryProfiles.get(id).getString(Constants.IP));

                    logger.trace("Device with id {} is having Ip {}", id, discoveryProfiles.get(id).getString(Constants.IP));

                }
                else
                {
                    reply.put(Constants.STATUS, Constants.FAILED);

                    logger.error("Device is not Provisioned!!");

                }

                return reply;
            }
            default ->
            {
                logger.error("Invalid Read Request Type in Database");

                reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE);

            }
        }
        return reply;

    }


}
