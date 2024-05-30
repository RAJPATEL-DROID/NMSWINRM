package org.nmslite.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.FileReader;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nmslite.utils.RequestType.DISCOVERY_RUN;
import static org.nmslite.utils.RequestType.POLLING_RESULT;

public class APIServer extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(APIServer.class);

    @Override
    public void start(Promise<Void> startPromise) {
        try {

            var port = Integer.parseInt(Utils.config.get(Constants.HTTP_PORT).toString());

            var hostname = Utils.config.get(Constants.HOST).toString();

            logger.info("Starting server on port {}", port);

            logger.info("Starting server on host {}", hostname);

            var httpServerOptions = new HttpServerOptions().setPort(port).setHost(hostname);

            HttpServer server = vertx.createHttpServer(httpServerOptions);

            Router router = Router.router(vertx);

            Credential credential = new Credential();
            credential.init(vertx);

            Discovery discovery = new Discovery();
            discovery.init(vertx);

            Provision provision = new Provision();
            provision.init(vertx);

            router.route(Constants.CREDENTIAL_ROUTE).subRouter(credential.getRouter());

            router.route(Constants.DISCOVERY_ROUTE).subRouter(discovery.getRouter());

            router.route(Constants.PROVISION_ROUTE).subRouter(provision.getRouter());

//            router.route("/data/:id").handler(this::getData);

            server.requestHandler(router).listen().onComplete(res -> {

                if (res.succeeded()) {

                    logger.info("Server is now listening");

                    startPromise.complete();
                } else {

                    logger.error("Failed to bind!");

                    startPromise.fail(res.cause());
                }
            });

            logger.info("SubRouters of credential,discovery and provision api have been deployed");

        } catch (Exception exception) {
            logger.error("Exception occurred during starting APIServer", exception);

            startPromise.fail(startPromise.future().cause());
        }
    }


}

