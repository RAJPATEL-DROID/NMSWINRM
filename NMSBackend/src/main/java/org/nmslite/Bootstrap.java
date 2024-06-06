package org.nmslite;

import io.vertx.core.Vertx;
import org.nmslite.apiserver.APIServer;
import org.nmslite.engine.ResponseProcessor;
import org.nmslite.engine.Scheduler;
import org.nmslite.utils.Utils;
import org.nmslite.server.ZMQRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private static final Vertx vertx = Vertx.vertx();
    public static Vertx getVertx()
    {
        return vertx;
    }

    public static void main(String[] args)
    {
        logger.info("Starting Backend Server...");

        if (Utils.readConfig())
        {
            vertx.deployVerticle(APIServer.class.getName())
                    .compose(deploy -> vertx.deployVerticle(ZMQRouter.class.getName()))
                    .compose(deploy -> vertx.deployVerticle(ResponseProcessor.class.getName()))
                    .compose(deploy -> vertx.deployVerticle(Scheduler.class.getName()))
                    .onComplete(status ->
                    {
                        if (status.succeeded())
                        {
                            logger.info("Backend Server started successfully...");
                        }
                        else
                        {
                            logger.error("Failed to start backend server", status.cause());
                        }
                    });
        }
        else
        {
            logger.error("Failed to start backend server");
        };
    }
}
