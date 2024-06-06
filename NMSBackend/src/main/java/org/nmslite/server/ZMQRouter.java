package org.nmslite.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Base64;

public class ZMQRouter extends AbstractVerticle
{

    private static final Logger logger = LoggerFactory.getLogger(ZMQRouter.class);

    @Override
    public void start(Promise<Void> startPromise)
    {

        try
        {

            ZContext zcontext = new ZContext();

            ZMQ.Socket reqSocket = zcontext.createSocket(SocketType.PUSH);

            ZMQ.Socket poller = zcontext.createSocket(SocketType.PULL);

            // Bind the REQ socket to a local address
            reqSocket.bind(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.PUSH_PORT));

            // Bind the REQ socket to Local Address
            poller.bind(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.RECEIVER_PORT));


            // Send the Request Context to PluginEngine via ZMQ
            vertx.eventBus().<String>localConsumer(Constants.ZMQ_PUSH, handler ->
            {
                logger.trace("Sending Message to PluginEngine : {}", handler.body());

                reqSocket.send(handler.body().getBytes(ZMQ.CHARSET), ZMQ.DONTWAIT);

            });

            // Continuously Look For Data on ZMQ Port and Send it to Appropriate Local Consumer in ResponseProcessor Verticle
            new Thread(()->
            {
                while (true)
                {
                    try
                    {
                        var result = poller.recvStr(ZMQ.DONTWAIT);

                        if (result != null)
                        {
                            // Make Decode method in Util Which Returns Directly JsonObject
                            var decodedBytes = Base64.getDecoder().decode(result);

                            var decodedString = new String(decodedBytes);

                            var received = new JsonObject(decodedString);

                            logger.info("Result Received : {} ", received);

                            vertx.eventBus().send(received.getString(Constants.REQUEST_TYPE), received);
                        }
                    }
                    catch (Exception exception)
                    {
                        logger.error("Exception in SetPeriodic");

                        logger.error(exception.toString());

                        logger.error(exception.getMessage());
                    }
                }

            }).start();

            startPromise.complete();

            logger.info("ZMQ router started....");
        }
        catch (Exception exception)
        {
            startPromise.fail(exception);

            logger.error("Failed to start ZMQ router ...", exception);

        }
    }
}
