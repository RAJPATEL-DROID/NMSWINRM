package org.nmslite.db;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nmslite.utils.Constants;
import org.nmslite.utils.RequestType;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigDBTest {

    private static JsonObject credentialRequest;
    private static JsonObject discoveryRequest;

    @BeforeAll
    static void setUp() {
        // Set up test data for CREDENTIAL request type
        credentialRequest = new JsonObject()
                .put(Constants.NAME, "raj")
                .put(Constants.USERNAME, "admin")
                .put(Constants.PASSWORD, "passwd");

        // Set up test data for DISCOVERY request type
        JsonArray credentialIds = new JsonArray().add(1L).add(2L);
        discoveryRequest = new JsonObject()
                .put(Constants.NAME, "linux")
                .put(Constants.IP, "192.168.0.1")
                .put(Constants.DEVICE_PORT, "8080")
                .put(Constants.CREDENTIAL_IDS, credentialIds);
    }

    @Test
    @Order(1)
    void testCreateCredential(VertxTestContext testContext)
    {
        // Test the create method for CREDENTIAL request type
        JsonObject result = ConfigDB.create(RequestType.CREDENTIAL, credentialRequest);

        testContext.verify(() -> {

            assertNotNull(result.getLong(Constants.CREDENTIAL_ID));

            assertTrue(ConfigDB.credentialsProfiles.containsKey(result.getLong(Constants.CREDENTIAL_ID)));
            JsonObject createdCredential = ConfigDB.credentialsProfiles.get(result.getLong(Constants.CREDENTIAL_ID));

            assertEquals(credentialRequest.getString(Constants.NAME), createdCredential.getString(Constants.NAME));

            assertEquals(credentialRequest.getString(Constants.USERNAME), createdCredential.getString(Constants.USERNAME));

            assertEquals(credentialRequest.getString(Constants.PASSWORD), createdCredential.getString(Constants.PASSWORD));
        });

        testContext.completeNow();
    }

    @Test
    @Order(2)
    void testCreateDiscovery(VertxTestContext testContext) {
        // Test the create method for DISCOVERY request type
        JsonObject result = ConfigDB.create(RequestType.DISCOVERY, discoveryRequest);
        testContext.verify(() ->
        {
            assertNotNull(result.getLong(Constants.DISCOVERY_ID));

            assertTrue(ConfigDB.discoveryProfiles.containsKey(result.getLong(Constants.DISCOVERY_ID)));

            JsonObject createdDiscovery = ConfigDB.discoveryProfiles.get(result.getLong(Constants.DISCOVERY_ID));

            assertEquals(discoveryRequest.getString(Constants.NAME), createdDiscovery.getString(Constants.NAME));

            assertEquals(discoveryRequest.getString(Constants.IP), createdDiscovery.getString(Constants.IP));

            assertEquals(discoveryRequest.getString(Constants.DEVICE_PORT), createdDiscovery.getString(Constants.DEVICE_PORT));

            JsonArray createdCredentialIds = createdDiscovery.getJsonArray(Constants.CREDENTIAL_IDS);

            assertEquals(discoveryRequest.getJsonArray(Constants.CREDENTIAL_IDS), createdCredentialIds);
        });
        testContext.completeNow();
    }

    // Add more test methods for other methods and request types
}