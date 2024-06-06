package org.nmslite.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIServer extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(APIServer.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        try {

            var port = Utils.getInteger(Utils.get(Constants.HTTP_PORT).toString());

            var hostname = Utils.get(Constants.HOST).toString();

            logger.info("Starting server on port {}", port);

            logger.info("Starting server on host {}", hostname);

            var httpServerOptions = new HttpServerOptions().setPort(port).setHost(hostname);

            HttpServer server = vertx.createHttpServer(httpServerOptions);

            Router router = Router.router(vertx);

            Credential credential = new Credential();
            credential.init(router);

            Discovery discovery = new Discovery();
            discovery.init(router);

            Provision provision = new Provision();
            provision.init(router);

            server.requestHandler(router).listen().onComplete(res ->
            {

                if (res.succeeded())
                {

                    logger.info("Server is now listening");

                    startPromise.complete();

                }
                else
                {

                    logger.error("Failed to bind!");

                    startPromise.fail(res.cause());
                }
            });

            logger.info("SubRouters of credential,discovery and provision api have been deployed");

        }
        catch (Exception exception)
        {
            logger.error("Exception occurred during starting APIServer", exception);

            startPromise.fail(startPromise.future().cause());

        }
    }


}

