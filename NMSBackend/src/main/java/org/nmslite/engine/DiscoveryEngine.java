package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmslite.db.ConfigDB;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static org.nmslite.utils.RequestType.VALID_DISCOVERY;

public class DiscoveryEngine extends AbstractVerticle
{
    public static final Logger logger = LoggerFactory.getLogger(DiscoveryEngine.class);

    @Override
    public void start(Promise<Void> startPromise)
    {

        EventBus eventBus = vertx.eventBus();

        eventBus.localConsumer(Constants.EVENT_RUN_DISCOVERY, msg ->
        {
            try
            {
                logger.trace("Contexts Received : {} " , msg.body().toString());

                var discoveryContext = new JsonObject(msg.body().toString());

                var context = Utils.createContext(discoveryContext, Constants.DISCOVERY, logger);

                logger.trace("Received Context Array from the Util : {}", context);

                if (!context.isEmpty())
                {

                    var count = context.size();

                    var message = context.toString();

                    var encodedContext = Base64.getEncoder().encode(message.getBytes());

                    Utils.sendContext(encodedContext);

                }
                else
                {

                    logger.error("Error in creating context!!!");

                }

            }
            catch (Exception exception)
            {
                logger.error(exception.toString());
            }
        });



        logger.info("Discovery Engine Started...");

        startPromise.complete();

    }



}
