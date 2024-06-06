package org.nmslite.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZMQRouter extends AbstractVerticle
{

    private static final Logger logger = LoggerFactory.getLogger(ZMQRouter.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        try
        {

            var zcontext = new ZContext();

            var reqSocket = zcontext.createSocket(SocketType.PUSH);

            var poller = zcontext.createSocket(SocketType.PULL);

            // Bind the REQ socket to a local address
            reqSocket.bind(Constants.ZMQ_ADDRESS + Utils.get(Constants.PUSH_PORT));

            // Bind the REQ socket to Local Address
            poller.bind(Constants.ZMQ_ADDRESS + Utils.get(Constants.RECEIVER_PORT));

            // Local Bus to send the incoming request Context to PluginEngine via ZMQ
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
                            var received = Utils.decode(result);

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
