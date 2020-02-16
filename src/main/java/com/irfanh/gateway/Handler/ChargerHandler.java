package com.irfanh.gateway.Handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;

public class ChargerHandler {

    Vertx vertx;
    JWTAuth jwtAuth;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerHandler.class);


    public ChargerHandler(Vertx vertx, JWTAuth jwtAuth) {
        this.vertx = vertx;
        this.jwtAuth = jwtAuth;
    }



    public void getChargers (RoutingContext ctx) {

        JsonObject query = new JsonObject();

        final String page = ctx.request().getParam("page");

        final String searchQuery = ctx.request().getParam("query");

        if (page!= null) {
            query.put("chargers.page", page);
        }


        if (searchQuery!= null) {
            query.put("chargers.query", searchQuery);
        }

        vertx.eventBus().send("/api/getChargers", query, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));
    }


    public void getCharger (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String charger = ctx.request().getParam("charger");


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());


                JsonObject message = new JsonObject().put("user", principal).put ("charger", charger);

                vertx.eventBus().send("/api/getCharger", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }


}
