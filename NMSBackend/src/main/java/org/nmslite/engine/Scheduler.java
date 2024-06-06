package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.nmslite.Bootstrap;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nmslite.utils.RequestType.POLLING;
import static org.nmslite.utils.RequestType.PROVISION;

public class Scheduler extends AbstractVerticle
{

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Override
    public void start(Promise<Void> startPromise)
    {

        var vertx = Bootstrap.getVertx();

        var pollTime = Utils.getLong(Utils.get(Constants.POLL_TIME).toString()) * 1000;

        logger.trace("Default Poll time set to {} ", pollTime);

        vertx.setPeriodic(pollTime, timer ->
        {
            try
            {
                var result = ConfigDB.read(PROVISION);

                // We Get Array of Discovery ID of Provisioned Device
                if (!result.getJsonArray(Constants.PROVISION_DEVICES).isEmpty())
                {
                    var pollingArray = new JsonArray();

                    //  Get Discovery and Credential Details of Each Device ( Create Context ) :
                    for (var id : result.getJsonArray(Constants.PROVISION_DEVICES))
                    {
                        var res = ConfigDB.read(POLLING, Utils.getLong(id.toString()));

                        pollingArray.add(res.getJsonObject(Constants.CONTEXT));

                    }

                    logger.info("Sending context Array : {}", pollingArray);

                    var context = Utils.encode(pollingArray.toString());

                    vertx.eventBus().send(Constants.ZMQ_PUSH, context);

                }
            }
            catch (Exception exception)
            {
                logger.error(exception.toString());

                logger.error(exception.getMessage());
            }
        });

        logger.info("Scheduler Class Deployed successfully...");

        startPromise.complete();
    }

}
