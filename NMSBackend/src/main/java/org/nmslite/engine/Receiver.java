package org.nmslite.engine;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Base64;

public class Receiver extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    @Override
    public void start(Promise<Void> startPromise)
    {

        var zContext = new ZContext();

        var poller = zContext.createSocket(SocketType.PULL);

        poller.bind(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.RECEIVER_PORT));

        long pollTime = 40 * 1000;

        var vertx = Bootstrap.getVertx();

        vertx.setPeriodic(pollTime, tid ->
        {
            try
            {
                var result = poller.recvStr(ZMQ.DONTWAIT);

                if (result == null)
                {
                    logger.info("No Result Available");
                }
                else
                {

                    var decodedBytes = Base64.getDecoder().decode(result);


                    var decodedString = new String(decodedBytes);

                    var received = new JsonObject(decodedString);

                    logger.info("Result Received : {} ", received);

                    vertx.executeBlocking(future ->
                    {

                        String ip = received.getString(Constants.IP);

                        String status = received.getString(Constants.STATUS);

                        logger.trace("Status of device is : {}", status);

                        if (status.equals(Constants.SUCCESS))
                        {

                            JsonObject pollResult = received.getJsonObject(Constants.RESULT);

                            // Write result to a file
                            Utils.writeToFile(ip, pollResult);

                        }
                        else if (status.equals(Constants.FAILED))
                        {

                            JsonArray errors = received.getJsonArray(Constants.ERRORS);
                            for (int j = 0; j < errors.size(); j++) {

                                JsonObject error = errors.getJsonObject(j);

                                Utils.writeToFile(ip, error);
                            }
                        }
                    });
                }
            }
            catch (Exception exception)
            {
                logger.error("Exception in SetPeriodic");

                logger.error(exception.toString());

                logger.error(exception.getMessage());
            }

        });

        logger.info("Poll Receiver verticle started...");
        startPromise.complete();
    }

}
