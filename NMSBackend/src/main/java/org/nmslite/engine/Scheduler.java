package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import static org.nmslite.utils.RequestType.POLLING;
import static org.nmslite.utils.RequestType.PROVISION;

public class Scheduler extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        ZContext zcontext = new ZContext();

        ZMQ.Socket reqSocket = zcontext.createSocket(SocketType.PUSH);

        // Bind the REQ socket to a local address
        reqSocket.bind(Constants.ZMQ_ADDRESS + Constants.PUSH_PORT);


        long pollTime = Long.parseLong(Utils.config.get(Constants.POLL_TIME).toString()) * 1000;

        logger.trace("Default Poll time set to {} ", pollTime);

        vertx.setPeriodic(pollTime, tid ->
        {
            try
            {
                var result = ConfigDB.read(PROVISION);

                // We Get Array of Discovery ID of Provisioned Device
                if (result.getJsonArray(Constants.PROVISION_DEVICES).isEmpty())
                {
                    logger.info("No Provisioned Device Found.");
                }
                else
                {
                    //  Get Discovery and Credential Details of Each Device ( Create Context ) :
                    for (var id : result.getJsonArray(Constants.PROVISION_DEVICES))
                    {
                        var res = ConfigDB.read(POLLING, Long.parseLong(id.toString()));
                        var deviceContext = res.getJsonObject(Constants.CONTEXT).toString();

                        // Send each device context as a separate message to the REQ socket
                        reqSocket.send(deviceContext.getBytes(ZMQ.CHARSET), ZMQ.DONTWAIT);
                    }
                }
            } catch (Exception exception)
            {
                logger.error(exception.toString());
                logger.error(exception.getMessage());
            }
        });

        logger.info("Scheduler Started");
        startPromise.complete();
    }
}

