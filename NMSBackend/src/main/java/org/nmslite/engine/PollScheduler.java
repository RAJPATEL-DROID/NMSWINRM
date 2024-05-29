package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
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

public class PollScheduler extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(PollScheduler.class);

    @Override
    public void start(Promise<Void> startPromise)
    {

            ZContext zcontext = new ZContext();

            ZMQ.Socket socket = zcontext.createSocket(SocketType.PUSH);

            socket.bind(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.PUBLISHER_PORT));

            long pollTime  =  Long.parseLong(Utils.config.get(Constants.POLL_TIME).toString()) * 1000;

            logger.trace("Default Poll time set to {} ", pollTime);

            vertx.setPeriodic(pollTime, tid ->
            {
                try
                {
                    var result = ConfigDB.read(PROVISION);

                    // We Get Array of Discovery ID of Provisioned Device
                    if(result.getJsonArray(Constants.PROVISION_DEVICES).isEmpty())
                    {
                        logger.info("No Provisioned Device Found.");
                    }
                    else
                    {
                        var pollingArray = new JsonArray();

                        logger.trace(result.encodePrettily());

                        //  Get Discovery and Credential Details of Each Device ( Create Context ) :
                        for (var id : result.getJsonArray(Constants.PROVISION_DEVICES)) {

                            var res = ConfigDB.read(POLLING, Long.parseLong(id.toString()));

                            pollingArray.add(res.getJsonObject(Constants.CONTEXT));

                        }

                        var context = pollingArray.toString().getBytes(ZMQ.CHARSET);

                        // Send Polling Array Context to Collector
                        socket.send(context, ZMQ.DONTWAIT);
                    }
                }
                catch (Exception exception)
                {
                    logger.error(exception.toString());

                    logger.error(exception.getMessage());

                }

            });

        logger.info("Poll Scheduler Started");

        startPromise.complete();
    }



}
