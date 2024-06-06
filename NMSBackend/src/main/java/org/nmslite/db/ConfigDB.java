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
import java.util.concurrent.atomic.AtomicLong;

public class ConfigDB
{

    private ConfigDB()
    { }

    private static final AtomicLong counter = new AtomicLong(0);

    private static final Logger logger = LoggerFactory.getLogger(ConfigDB.class);

    private static final ConcurrentHashMap<Long, JsonObject> credentialsProfiles = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<Long, JsonObject> discoveryProfiles = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, JsonObject> validDevices = new ConcurrentHashMap<>();

    private static final ConcurrentHashSet<Long> provisionedDevices = new ConcurrentHashSet<>();

    public static long getId()
    {

        return counter.incrementAndGet();

    }

    public static JsonObject create(RequestType type, JsonObject request)
    {

        var reply = new JsonObject();

        logger.trace("Create Request for : {}", type);

        try
        {
            switch (type)
            {

                case CREDENTIAL ->
                {
                    boolean credentialExist = credentialsProfiles.values().stream()
                                            .anyMatch(credential -> credential.getString(Constants.NAME).equals(request.getString(Constants.NAME)));

                    if(credentialExist)
                    {
                        reply.put(Constants.STATUS, Constants.FAILED);
                    }
                    else
                    {

                        // If execution is here means credentials are new
                        var id = getId();

                        credentialsProfiles.put(id, request);

                        reply.put(Constants.CREDENTIAL_ID, id);
                    }

                }
                case DISCOVERY ->
                {

                    var credentialArray = request.getJsonArray(Constants.CREDENTIAL_IDS);

                    var allCredentialsExist = credentialArray.stream().map(Object::toString)
                            .map(Utils::getLong)
                            .allMatch(credentialsProfiles::containsKey);

                    if(!allCredentialsExist)
                    {
                        reply.put(Constants.STATUS, Constants.FAILED);
                    }
                    else
                    {
                        var discoveryProfileExists = discoveryProfiles.values().stream()
                                .anyMatch(discoveryProfile -> discoveryProfile.getString(Constants.NAME).equals(request.getString(Constants.NAME)));

                        if(discoveryProfileExists)
                        {
                            reply.put(Constants.STATUS, Constants.FAILED);
                        }
                        else
                        {
                            var id = getId();

                            discoveryProfiles.put(id, request);

                            reply.put(Constants.DISCOVERY_ID, id);
                        }
                    }
                }
                case VALID_DISCOVERY ->
                {

                    logger.trace(request.toString());

                    var ip = request.getString(Constants.IP);

                    request.remove(Constants.IP);

                    if (!validDevices.containsKey(ip))
                    {
                        validDevices.put(ip, request);

                        logger.info("Device added in list of valid devices {}",ip);

                        logger.info("Valid Devices : {} ",validDevices.keySet().toString());

                    }
                    else
                    {
                        logger.info("Device with Same IP Address already exists");

                        reply.put(Constants.ERROR, "Device with Same IP Address already exists");

                        return reply;

                    }

                    logger.info("For Device IP {} , credential id {} is valid", ip, validDevices.get(ip)
                            .getString(Constants.CREDENTIAL_ID));

                    reply.put(Constants.MESSAGE, "Device Discovered Successfully");

                }
                case PROVISION ->
                {

                    var id = request.getLong(Constants.ID);

                    // check if the discovery id is present in any valid devices
                    if (discoveryProfiles.containsKey(id))
                    {

                        boolean deviceDiscovered = validDevices.values().stream()
                                .anyMatch(device -> device.getLong(Constants.DISCOVERY_ID).equals(id));

                        if(deviceDiscovered)
                        {
                            if(!provisionedDevices.contains(id))
                            {
                                provisionedDevices.add(id);

                                logger.info("Device Provisioned Successfully for {}", id);
                            }
                            else
                            {
                                reply.put(Constants.ERROR, "Device already provisioned");

                                logger.trace("Device already provisioned successfully for {}", id);
                            }
                        }
                        else
                        {
                            reply.put(Constants.ERROR,"Device is not discovered yet!!");
                        }
                    }
                    else
                    {
                        reply.put(Constants.ERROR, "Invalid Discovery Id");
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

            logger.error("Exception Occurred in Create Method", exception);

            reply.put(Constants.ERROR, exception.toString())

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage());
        }

        return reply;

    }

    public static JsonObject read(RequestType request)
    {

        var reply = new JsonObject();

        logger.info("Read  Request for type {}", request);

        try
        {
            switch (request)
            {
                case CREDENTIAL ->
                {
                    var credentialObjects = credentialsProfiles.entrySet().stream()
                            .map(entry -> entry.getValue().copy().put("id", entry.getKey().toString()))
                            .collect(  JsonArray::new, JsonArray::add, JsonArray::addAll);

                    reply.put(Constants.CREDENTIAL_IDS, credentialObjects);

                }
                case DISCOVERY ->
                {
                    var discoveryObjects = discoveryProfiles.entrySet().stream()
                            .map(entry -> entry.getValue().copy().put("id", entry.getKey().toString()))
                            .collect(  JsonArray::new, JsonArray::add, JsonArray::addAll);

                    reply.put(Constants.DISCOVERY_IDS, discoveryObjects);

                }
                case PROVISION ->
                {

                    var provisionedMonitors = provisionedDevices.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

                    reply.put(Constants.PROVISION_DEVICES, provisionedMonitors);

                }
                default ->
                {

                    logger.error("Invalid Read Request Type in Database");

                    reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE)

                            .put(Constants.ERROR_CODE, Constants.BAD_REQUEST);
                }
            }
        }
        catch (Exception exception)
        {

            logger.error("Exception Occurred in Read Method", exception);

            reply.put(Constants.ERROR, exception.toString())

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage());
        }

        return reply;
    }

    public static JsonObject read(RequestType type, Long id)
    {

        var reply = new JsonObject();

        logger.trace("Read  Request for type {} of id {}", type, id);

        try
        {
            switch (type)
            {
                case DISCOVERY_RUN ->
                {
                    if (discoveryProfiles.containsKey(id))
                    {

                        var contextData = new JsonObject();

                        var discovery = discoveryProfiles.get(id);

                        var credentials = discovery.getJsonArray(Constants.CREDENTIAL_IDS).stream()
                                .map(credentialId -> {
                                    var credDetails = credentialsProfiles.get(Utils.getLong(credentialId.toString()));
                                    return credDetails.put(Constants.CREDENTIAL_ID, credentialId);
                                })
                                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

                        contextData.put(Constants.DISCOVERY_DATA, discovery)

                                .put(Constants.CREDENTIAL_PROFILES, credentials)

                                .put(Constants.ID, id);

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

                    if (discoveryProfiles.containsKey(id))
                    {
                        var deviceDiscovered  = validDevices.values().stream()
                                .anyMatch(deviceId -> deviceId.getLong(Constants.DISCOVERY_ID).equals(id));

                        if(deviceDiscovered)
                        {
                            logger.info("got the match of discovery id");

                            reply.put(Constants.STATUS, Constants.SUCCESS);

                        }
                        else
                        {
                            reply.put(Constants.STATUS, Constants.FAILED);
                        }
                    }
                    else
                    {
                        reply.put(Constants.ERROR, "Invalid Discovery Id");

                        reply.put(Constants.STATUS, Constants.FAILED);
                    }
                }
                case POLLING ->
                {
                    var details = new JsonObject();

                    details.put(Constants.IP, discoveryProfiles.get(id).getString(Constants.IP))
                            .put(Constants.DEVICE_PORT, discoveryProfiles.get(id).getString(Constants.DEVICE_PORT));

                    // Find Device which are Discovered ,and put their details in jsonObject
                    validDevices.values().stream()
                            .filter(device -> device.getLong(Constants.DISCOVERY_ID).equals(id))
                            .findFirst()
                            .ifPresent(device -> {
                                var credentialDetails = credentialsProfiles.get(device.getLong(Constants.CREDENTIAL_ID));
                                details.put(Constants.USERNAME, credentialDetails.getString(Constants.USERNAME))
                                        .put(Constants.PASSWORD, credentialDetails.getString(Constants.PASSWORD));
                            });

                    details.put(Constants.REQUEST_TYPE, Constants.POLLING);

                    reply.put(Constants.CONTEXT, details);

                    logger.trace("Read  Request for type {} of id {} performed, sending details {}", type, id, details);

                }
                case POLLING_RESULT ->
                {

                    if (provisionedDevices.contains(id))
                    {
                        reply.put(Constants.IP, discoveryProfiles.get(id).getString(Constants.IP));

                        logger.trace("Device with id {} is having Ip {}", id, discoveryProfiles.get(id).getString(Constants.IP));

                    }
                    else
                    {
                        reply.put(Constants.STATUS, Constants.FAILED);

                        logger.error("Device is not Provisioned!!");

                    }
                }
                default ->
                {
                    logger.error("Invalid Read Request Type in Database ");

                    reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE);

                }
            }
        }
        catch (Exception exception)
        {
            logger.error("Exception Occurred in Read Method with id Parameter", exception);

            reply.put(Constants.ERROR, exception.toString())

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage());
        }

        return reply;

    }

}
