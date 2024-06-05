package org.nmslite.utils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Base64;


public class ZMQRouter
{
    private static final Logger logger = LoggerFactory.getLogger(ZMQRouter.class);

    private final Vertx vertx = Bootstrap.getVertx();

    public ZMQRouter()
    {

        ZContext zcontext = new ZContext();

        ZMQ.Socket reqSocket = zcontext.createSocket(SocketType.PUSH);

        ZMQ.Socket poller = zcontext.createSocket(SocketType.PULL);

        // Bind the REQ socket to a local address
        reqSocket.bind(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.PUSH_PORT));

        // Bind the REQ socket to Local Address
        poller.bind(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.RECEIVER_PORT));

        sendData(reqSocket);

        receiveData(poller);

        logger.info("ZMQ router started....");
    }

    private void sendData(ZMQ.Socket socket)
    {
        new Thread(() ->
        {
            while(true)
            {
                try
                {
                    vertx.eventBus().localConsumer(Constants.ZMQ_PUSH, handler ->
                    {
                        logger.trace("Sending Message to PluginEngine : {}", handler.body().toString());
                        socket.send(handler.body().toString().getBytes(ZMQ.CHARSET));

                    });
                }
                catch(Exception exception)
                {
                    logger.error(exception.toString());

                    logger.error(exception.getMessage());
                }
            }
        }).start();
    }


    private void receiveData(ZMQ.Socket poller)
    {

        new Thread( () ->
        {
            while (true){
                try
                {
                    var result = poller.recvStr(ZMQ.DONTWAIT);

                    if (result != null)
                    {
                        var decodedBytes = Base64.getDecoder().decode(result);

                        var decodedString = new String(decodedBytes);

                        var received = new JsonObject(decodedString);

                        logger.info("Result Received : {} ", received);

                        vertx.eventBus().send(received.getString(Constants.REQUEST_TYPE),received);
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

    }


}
