package org.nmslite;

import io.vertx.core.Vertx;
import org.nmslite.apiserver.APIServer;
import org.nmslite.engine.*;
import org.nmslite.utils.ResponseProcessor;
import org.nmslite.utils.Utils;
import org.nmslite.utils.ZMQRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{

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
            if (result.succeeded())
            {
                vertx.deployVerticle(APIServer.class.getName())
//                    .compose(deployment->
//                                vertx.deployVerticle(Receiver.class.getName()) )
//                    .compose(deployment ->
//                            vertx.deployVerticle(Scheduler.class.getName()))
                    .onComplete(status ->
                    {
                        if (status.succeeded())
                        {

                            try {

                                new ZMQRouter();

                                Scheduler scheduler = new Scheduler();
                                scheduler.schedule();

                                ResponseProcessor receiver = new ResponseProcessor();
                                receiver.receive();

                            }
                            catch (Exception exception)
                            {
                                logger.error("Failed to Make ZMQ Socket Connections", exception);
                            }

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
                logger.error("Failed to start backend server", result.cause());
            };
        });



    }
}
