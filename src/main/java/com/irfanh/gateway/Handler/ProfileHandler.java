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

public class ProfileHandler {

    Vertx vertx;
    JWTAuth jwtAuth;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileHandler.class);


    public ProfileHandler(Vertx vertx, JWTAuth jwtAuth) {
      this.vertx = vertx;
      this.jwtAuth = jwtAuth;
    }

    public void getProfiles (RoutingContext ctx) {

        vertx.eventBus().send("/api/getUsers", null, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

        System.out.println("headerAuth:");


    }


    public void loginUser(RoutingContext ctx) {
        JsonObject newUser = ctx.getBodyAsJson();
        vertx.eventBus().send("/api/loginUser", newUser, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));
    }




    public void getProfile (RoutingContext ctx) {


        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String userFollowed = ctx.request().getParam("userFollowed");


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());

/*
                theUser.isAuthorised(MongoAuth.ROLE_PREFIX + "XXXXXX", authRes -> {
                    LOGGER.info("Auth: {}", authRes.result());
                    LOGGER.info(ctx.request().getHeader("Authorization"));
                });*/


                theUser.isAuthorized("role:developer", resX -> {
                    if (resX.succeeded() && resX.result()) {
                        LOGGER.info("Auth: {}", resX.result());
                        LOGGER.info(ctx.request().getHeader("Authorization"));
                    }
                });

                // JsonObject message = new JsonObject().put("user", principal).put ("userFollowed", userFollowed);

                JsonObject query = new JsonObject();
                final String username = ctx.request().getParam("username");
                query.put("username", username);

                JsonObject message = new JsonObject().put("user", principal).put("username", username);

                vertx.eventBus().send("/api/getProfile", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }


    public void postUser(RoutingContext ctx) {
        JsonObject newUser = ctx.getBodyAsJson();
        vertx.eventBus().send("/api/users-post", newUser, (Handler<AsyncResult<Message<JsonObject>>>) responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));
    }





    public void getCurrentUser(RoutingContext routingContext) {

    /*    routingContext.user().isAuthorised(MongoAuth.ROLE_PREFIX + "user", authRes -> {
            LOGGER.info("Auth: {}", authRes.result());
            LOGGER.info(routingContext.request().getHeader("Authorization"));
        });*/



        String headerAuth = routingContext.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());


                vertx.eventBus().send("/api/getUser", principal, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(routingContext, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });

    }



    public void updateUser (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);
        JsonObject newUserValuesJson = ctx.getBodyAsJson();


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());

                JsonObject message = new JsonObject().put("user", principal).put("fields", newUserValuesJson);

                vertx.eventBus().send("api/updateUser", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }





    public void followUser (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String userFollowed = ctx.request().getParam("userFollowed");



        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());



                JsonObject message = new JsonObject().put("user", principal).put ("userFollowed", userFollowed);

                vertx.eventBus().send("/api/followUser", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }





    public void unfollowUser (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String userFollowed = ctx.request().getParam("usernameUnfollowed");



        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());



                JsonObject message = new JsonObject().put("user", principal).put ("usernameUnfollowed", userFollowed);

                vertx.eventBus().send("/api/unfollowUser", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }



    public void getUserFollowers (RoutingContext ctx) {

        JsonObject query = new JsonObject();
        final String groupId = ctx.request().getParam("userId");
        query.put("userId", groupId);

        vertx.eventBus().send("/api/getUserFollowers", query, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

        System.out.println("headerAuth:");


    }

}
