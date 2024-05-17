package org.nmslite;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import org.nmslite.engine.APIEngine;
import org.nmslite.engine.Discoveryengine;
import org.nmslite.engine.PollingEngine;
import org.nmslite.profile.DiscoveryRun;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args)
    {


        logger.info("Starting Backend Server...");


        if(!Utils.readConfig())
        {
            vertx.close();
            return;
        };

        var deploymentOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER);

        vertx.deployVerticle(new APIEngine()).onComplete(res->
        {
            if(res.succeeded())
            {
                logger.info("API Server started....");
            }
            else
            {
                logger.error("API Server failed to start");

                logger.error(res.cause().getMessage());

            }
        });

        vertx.deployVerticle(new DiscoveryRun(), deploymentOptions).onComplete( res->
        {
            if(res.succeeded())
            {
                logger.info("Run Discovery Verticle started....");
            }
            else
            {
                logger.error("Run Discovery Verticle failed to start");

                logger.error(res.cause().getMessage());
            }
        });

       vertx.deployVerticle(new Discoveryengine(),deploymentOptions).onComplete( res ->
       {
           if(res.succeeded())
           {
               logger.info("Discovery engine started....");
           }
           else
           {
               logger.error("Discovery engine failed to start");

               logger.error(res.cause().getMessage());

           }
       });


        vertx.deployVerticle(new PollingEngine(),new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER) , res ->
        {
            if(res.succeeded())
            {
                logger.info("Polling Engine started....");

            }
            else
            {
                logger.error("Polling Engine failed to start");

                logger.error(res.cause().getMessage());
            }
        });
    }
}
