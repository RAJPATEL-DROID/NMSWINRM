package org.nmslite.engine;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;

import static org.nmslite.utils.RequestType.POLLING;
import static org.nmslite.utils.RequestType.PROVISION;

public class Scheduler {


    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    public void schedule() {

        Vertx vertx = Bootstrap.getVertx();

        long pollTime = Long.parseLong(Utils.config.get(Constants.POLL_TIME).toString()) * 1000;

        logger.trace("Default Poll time set to {} ", pollTime);

        new Thread(() ->
        {
            while(true)
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
                        var pollingArray = new JsonArray();

                        //  Get Discovery and Credential Details of Each Device ( Create Context ) :
                        for (var id : result.getJsonArray(Constants.PROVISION_DEVICES))
                        {
                            var res = ConfigDB.read(POLLING, Long.parseLong(id.toString()));

                            pollingArray.add(res.getJsonObject(Constants.CONTEXT));

                        }

                        logger.info("Sending context Array : {}", pollingArray);

                        var context = Base64.getEncoder().encodeToString(pollingArray.toString().getBytes(zmq.ZMQ.CHARSET));

                        vertx.eventBus().send(Constants.ZMQ_PUSH,context);

                    }

                    Thread.sleep(pollTime);
                }
                catch(Exception exception)
                {
                    logger.error(exception.toString());

                    logger.error(exception.getMessage());
                }
            }
        }).start();

    }

}
