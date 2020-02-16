package com.irfanh.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;

public class WorkerVerticle extends AbstractVerticle {


    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerVerticle.class);

    @Override
    public void start() {
        // create a apiRouter to handle the API
        vertx.eventBus().consumer("myTEST", myTest());

    }

/*    public void myTest() {
        String reply1 = sendCommand1("/api/myTest","hello");
        LOGGER.info("aaa" + reply1);
    }*/


    private Handler<Message<JsonObject>> myTest () {
        return handler -> {

            Future<String> reply1 = sendCommand("/api/myTest","hello");
            LOGGER.info("aaa" + reply1);

            JsonObject jsonObject = new JsonObject();
            jsonObject.put("status", "ok");

            handler.reply(jsonObject);

        };
    }


    public Future<String> sendCommand(String address, String command) {
        Future<String> future = Future.future();

        vertx.eventBus().send(address,command, handler -> {

            if(handler.succeeded()) {
                future.complete ("aaaaaaa");
            } else {
                future.fail("bbbbb");
            }
        });


        return future;
    }


    private String sendCommand1 (String address, String command) {

        final String[] message = new String[1];
        String response = "OK";
        LOGGER.info("FORWARDING!!!!!!!!!!!!!!!!!!!!!!!!");
        vertx.eventBus().send(address,command, handler -> {
            if(handler.succeeded()) {

                LOGGER.info("bbbb" + handler.result().body().toString());
                message[0] = "OK"; // example value

            }


        });

        return message[0];

    }
}
