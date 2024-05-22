package org.nmslite;

import io.vertx.core.Vertx;
import org.nmslite.apiserver.APIServer;
import org.nmslite.engine.DiscoveryEngine;
import org.nmslite.engine.PollingEngine;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static final Vertx vertx = Vertx.vertx();
    public static Vertx getVertx()
    {
        return vertx;
    }

    public static void main(String[] args)
    {


        logger.info("Starting Backend Server...");


        Utils.readConfig().onComplete(result ->
        {
            if(result.succeeded())
            {
                vertx.deployVerticle(APIServer.class.getName())
                        .compose(deploymentId -> vertx.deployVerticle(DiscoveryEngine.class.getName()))
                        .compose(deploymentId ->
                                vertx.deployVerticle(DiscoveryEngine.class.getName()))
                        .compose(deployementId ->
                                vertx.deployVerticle(PollingEngine.class.getName()))
                        .onComplete(status ->
                        {
                            if(status.succeeded())
                            {
                                logger.info("Backend Server started successfully...");
                            }else{
                                logger.error("Failed to start backend server", status.cause());
                            }
                        });
            }

        });

    }
}
