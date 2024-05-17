package org.nmslite.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.nmslite.Bootstrap;
import org.nmslite.utils.Constants;
import org.nmslite.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class Discoveryengine extends AbstractVerticle
{
    public static final Logger logger = LoggerFactory.getLogger(Discoveryengine.class);

    @Override
    public void start(Promise<Void> startPromise){

        EventBus eventBus = Bootstrap.vertx.eventBus();

        eventBus.localConsumer(Constants.EVENT_RUN_DISCOVERY, msg ->
        {
            try
            {
                var jsonb = new JsonObject(msg.body().toString());

                var count = jsonb.getInteger("context.size");

                var message = jsonb.getString("context");

                var encodedString = Base64.getEncoder().encodeToString(message.getBytes());

                var replyJson = Utils.spawnPluginEngine(encodedString,count);

                logger.info("Data Received for context {}", replyJson);

                msg.reply(replyJson);

            }
            catch (Exception exception)
            {
                msg.fail(501,new JsonObject().put(Constants.STATUS,Constants.FAILED).put(Constants.ERROR,new JsonObject().put(Constants.ERROR,exception.toString()).put(Constants.ERROR_CODE,501).put(Constants.ERROR_MESSAGE,"unable to run plugin engine")).toString());

                logger.error(exception.toString());
            }

        });

        startPromise.complete();

    }

}
