package org.nmslite.db;

import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigDB {
    private ConfigDB() {
    }

    private static final Logger logger = LoggerFactory.getLogger(ConfigDB.class);

    private static final ConcurrentHashMap<Long, JsonObject> credentialsProfiles = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, JsonObject> discoveryProfiles = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, Long> validDevices = new ConcurrentHashMap<>();

    private static final ConcurrentHashSet<Long> provisionedDevices = new ConcurrentHashSet<>();

    public static String create(String request) {

        // TODO : Remove Split,Pass Type in Request
        var reply = new JsonObject();

        var token = request.split(Constants.MESSAGE_SEPARATOR, 2);

        logger.trace("Create Request for : {}", (Object) token);

        var requestType = token[0];

        try {
            switch (requestType) {

                case Constants.CREDENTIAL -> {

                    var newCredentials = new JsonObject(token[1]);

                    // check if credential with same name exists
                    for (var credential : credentialsProfiles.values()) {
                        if (credential.getString(Constants.NAME).equals(newCredentials.getString(Constants.NAME)))
                        {
                            reply.put(Constants.ERROR, "Credential Profile Not Created");

                            reply.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                            reply.put(Constants.ERROR_MESSAGE, "Credential with Name " + newCredentials.getString(Constants.NAME) + " already exists");

                            return reply.toString();
                        }
                    }

                    // If execution is here means credentials are new
                    var id = Utils.getId();

                    credentialsProfiles.put(id, newCredentials);

                    reply.put(Constants.CREDENTIAL_ID, id);

                }
                case Constants.DISCOVERY -> {

                    var newDiscoveryProfile = new JsonObject(token[1]);

                    var credentialArray = newDiscoveryProfile.getJsonArray("credential.ids");

                    for (Object credentialId : credentialArray) {
                        if (!credentialsProfiles.containsKey(Long.parseLong(credentialId.toString()))) {

                            reply.put(Constants.ERROR, "Credential Profiles Are Not Valid")

                                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                            return reply.toString();
                        }
                    }

                    for (var discoveryprofile : discoveryProfiles.values())
                    {
                        if (discoveryprofile.getString(Constants.NAME).equals(newDiscoveryProfile.getString(Constants.NAME)))
                        {

                            reply.put(Constants.ERROR, "Discovery Profiles Not Created")

                                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                                    .put(Constants.ERROR_MESSAGE, "Discovery Profile with Name " + newDiscoveryProfile.getString(Constants.NAME) + " already exists");

                            return reply.toString();
                        }

                    }

                    var id = Utils.getId();

                    discoveryProfiles.put(id, newDiscoveryProfile);

                    reply.put(Constants.DISCOVERY_ID, id);

                }
                case Constants.VALID_DISCOVERY -> {
                    var jsonObject = new JsonObject(token[1]);

                    var discoveryID = Long.parseLong(jsonObject.getString("discovery.id"));

                    if (!validDevices.containsKey(discoveryID)) {
                        validDevices.put(discoveryID, Long.parseLong(jsonObject.getString("credential.id")));
                    }

                    reply.put("message", "Device Discovered Successfully");

                    logger.info("For Discovery ID {}, credential id {} is valid", discoveryID, validDevices.get(discoveryID));

                }
                case Constants.PROVISION -> {
                    var id = Long.parseLong(token[1]);

                    if (validDevices.containsKey(id) && !provisionedDevices.contains(id)) {

                        provisionedDevices.add(id);

                        logger.trace("Device provisioned successfully for {}", id);

                        logger.trace("Provisioned Device : {} ", provisionedDevices.toArray());

                        reply.put("Message", "Device Provisioned Successfully ");

                    } else {

                        reply.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                        reply.put(Constants.ERROR, "Request Invalid!!");

                        if (!validDevices.containsKey(id)) {
                            reply.put(Constants.ERROR_MESSAGE, "Device not Discovered yet");
                        } else {
                            reply.put(Constants.ERROR_MESSAGE, "Device Already Provisioned");
                        }

                    }
                }
                default -> {

                    logger.error("Invalid Create Request Type in Database");

                    reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE)
                            .put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                }
            }
        } catch (Exception exception) {
            logger.error("Error while inserting discovery ", exception);

            reply.put(Constants.ERROR, exception.toString())

                    .put(Constants.ERROR_CODE, 400)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage());
        }

        return reply.toString();

    }

    public static String read(String request) {

        var reply = new JsonObject();

        logger.trace("Read  Request for type {}", request);

        switch (request) {
            case Constants.CREDENTIAL -> {
                JsonArray credentialObjects = new JsonArray();

                for (var id : credentialsProfiles.keySet()) {

                    credentialObjects.add(new JsonObject().put(id.toString(), credentialsProfiles.get(id)));

                }

                reply.put(Constants.CREDENTIAL_IDS, credentialObjects);
            }
            case Constants.DISCOVERY -> {
                JsonArray discoveryObjects = new JsonArray();

                for (var id : discoveryProfiles.keySet()) {
                    discoveryObjects.add(new JsonObject().put(id.toString(), discoveryProfiles.get(id)));

                }
                reply.put(Constants.DISCOVERY_IDS, discoveryObjects);
            }
            case Constants.PROVISION -> {
                JsonArray provisionedMonitors = new JsonArray();

                for (var id : provisionedDevices) {
                    provisionedMonitors.add(id);
                }

                reply.put(Constants.PROVISION_DEVICES, provisionedMonitors);
            }
            default -> {

                logger.error("Invalid Read Request Type in Database");

                reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE)

                        .put(Constants.ERROR_CODE, Constants.BAD_REQUEST);
            }
        }
        return reply.toString();
    }

    public static String read(Long id, String type) {

        var reply = new JsonObject();

        logger.trace("Read  Request for type {} of id {}", type, id);

        switch (type) {
            case Constants.DISCOVERY_RUN -> {
                if (discoveryProfiles.containsKey(id)) {
                    var contextData = new JsonArray();

                    var discovery = discoveryProfiles.get(id);

                    var credentialArray = new JsonArray();

                    for (var credId : discovery.getJsonArray(Constants.CREDENTIAL_IDS)) {

                        var credDetails = credentialsProfiles.get(Long.parseLong(credId.toString()));

                        credDetails.put(Constants.CREDENTIAL_ID, credId);

                        credentialArray.add(credDetails);

                    }

                    contextData.add(new JsonObject().put(Constants.DISCOVERY_DATA, discovery).put(Constants.CREDENTIAL_PROFILES, credentialArray));

                    reply.put(Constants.Context, contextData);
                } else {
                    reply.put(Constants.STATUS, Constants.FAILED)

                            .put(Constants.ERROR, "Invalid Discovery id")

                            .put(Constants.ERROR_CODE, 400)

                            .put(Constants.ERROR_MESSAGE, "Provide valid discovery id");

                    logger.error("Invalid Discovery Id {}", reply);
                }

            }
            case Constants.POLLING -> {
                var details = new JsonObject();

                details.put(Constants.IP, discoveryProfiles.get(id).getString(Constants.IP))
                        .put(Constants.DEVICE_PORT, discoveryProfiles.get(id).getString(Constants.DEVICE_PORT));

                var credentialDetails = credentialsProfiles.get(validDevices.get(id));

                details.put(Constants.USERNAME, credentialDetails.getString(Constants.USERNAME))
                        .put(Constants.PASSWORD, credentialDetails.getString(Constants.PASSWORD));


                details.put(Constants.REQUEST_TYPE, Constants.POLLING);

                reply.put(Constants.Context, details);

            }
            default -> {
                logger.error("Invalid Read Request Type in Database");

                reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE);

            }
        }
        return reply.toString();

    }

    public static String delete(String request) {
        var reply = new JsonObject();

        var token = request.split(Constants.MESSAGE_SEPARATOR, 2);

        logger.trace("Delete Request for : {}", (Object) token);

        var requestType = token[0];

        try {
            var id = Long.parseLong(token[1]);
            switch (requestType) {

                case Constants.CREDENTIAL -> {

                    if (!credentialsProfiles.containsKey(id)) {
                        reply.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                        reply.put(Constants.ERROR, "Cannot delete credential Id");

                        reply.put(Constants.ERROR_MESSAGE, "Credential Id not found");

                        return reply.toString();

                    }

                    // If Device is not Discovered through that credential
                    if (!validDevices.containsValue(id)) {
                        credentialsProfiles.remove(id);

                        logger.trace("Credential Id {} deleted from Credential Store", id);

                        for (var discoveryId : discoveryProfiles.keySet()) {
                            if (discoveryProfiles.get(discoveryId).getJsonArray(Constants.CREDENTIAL_IDS).contains(id)) {
                                discoveryProfiles.get(discoveryId).getJsonArray(Constants.CREDENTIAL_IDS).remove(id);

                                logger.trace("Credential Id {} deleted from the Json array of Discovery ID {}", id, discoveryProfiles.get(discoveryId));

                                logger.trace(discoveryProfiles.get(discoveryId).toString());
                            }
                            ;
                        }
                    } else {
                        // Check if discovery ID is provisioned or not
                        for (var discoveryId : provisionedDevices) {
                            // Check if for any discovery id which is provisioned is related to Credential that is to be deleted;
                            if (validDevices.get(discoveryId).equals(id)) {

                                logger.debug("unable to delete credential profile {} because is provisioned", id);

                                reply.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                                reply.put(Constants.ERROR, "Cannot delete credential profile");


                                reply.put(Constants.ERROR_MESSAGE, "Device is in provision");

                                return reply.toString();
                            }

                        }

                        // If you are here , then device is not provisioned
                        credentialsProfiles.remove(id);

                        for (var discoveryId : discoveryProfiles.keySet()) {
                            if (discoveryProfiles.get(discoveryId).getJsonArray(Constants.CREDENTIAL_IDS).contains(id)) {
                                discoveryProfiles.get(discoveryId).getJsonArray(Constants.CREDENTIAL_IDS).remove(id);
                            }
                            ;
                        }

                        for (var key : validDevices.keySet()) {
                            if (validDevices.get(key).equals(id)) {

                                validDevices.remove(key);
                            }
                        }
                    }

                    logger.info("credential profile {} deleted", id);

                    reply.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    reply.put(Constants.MESSAGE, "Credential profile deleted successfully");

                }
                case Constants.DISCOVERY -> {
                    if (!discoveryProfiles.containsKey(id)) {
                        reply.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                        reply.put(Constants.ERROR, "Cannot delete Discovery Id");

                        reply.put(Constants.ERROR_MESSAGE, "Discovery Id not found");

                        return reply.toString();
                    }

                    //Check if Device is provisioned or not
                    for (var discoveryId : provisionedDevices) {
                        if (discoveryId.equals(id)) {
                            reply.put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                            reply.put(Constants.ERROR, "Cannot delete Discovery Id");

                            reply.put(Constants.ERROR_MESSAGE, "Device is provisioned");

                            return reply.toString();
                        }
                    }

                    // If you are here means device is not provisioned
                    validDevices.remove(id);

                    discoveryProfiles.remove(id);

                    logger.info("Discovery profile with id  {} deleted", id);

                    reply.put(Constants.ERROR_CODE, Constants.SUCCESS_CODE);

                    reply.put(Constants.MESSAGE, "Discovery profile deleted successfully");


                }
                default -> {

                    logger.error("Invalid Delete Request Type in Database");

                    reply.put(Constants.ERROR, Constants.INVALID_REQUEST_TYPE)
                            .put(Constants.ERROR_CODE, Constants.BAD_REQUEST);

                }
            }
        } catch (Exception exception) {
            logger.error("Error while Deleting Type ", exception);
            logger.error(Arrays.toString(exception.getStackTrace()));

            reply.put(Constants.ERROR, exception.toString())

                    .put(Constants.ERROR_CODE, 400)

                    .put(Constants.ERROR_MESSAGE, exception.getMessage());
        }

        return reply.toString();

    }


}
