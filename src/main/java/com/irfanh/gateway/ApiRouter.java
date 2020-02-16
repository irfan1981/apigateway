package com.irfanh.gateway;

import com.irfanh.gateway.Clients.UserClient;
import com.irfanh.gateway.Handler.ChargerHandler;
import com.irfanh.gateway.Handler.ProfileHandler;
import com.irfanh.gateway.Handler.ResponseHandler;
import io.netty.util.concurrent.SucceededFuture;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApiRouter {
    Vertx vertx;
    JWTAuth jwtAuth;
    private ProfileHandler profileHandler;
    private ChargerHandler chargerHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRouter.class);

    public ApiRouter(Vertx vertx, JWTAuth jwtAuth) {
        this.vertx = vertx;
        this.jwtAuth = jwtAuth;

        profileHandler = new ProfileHandler(vertx, jwtAuth);
        chargerHandler = new ChargerHandler(vertx, jwtAuth);
    }



    public Router createRouter () {
        // create a apiRouter to handle the API
        Router baseRouter = Router.router(vertx);


        baseRouter.mountSubRouter("/api", apiRouter ());

        return baseRouter;
    }


    private Router apiRouter () {
        Router apiRouter = Router.router(vertx);


        apiRouter.route().handler(CorsHandler.create(".*.") //
                 .allowCredentials(true)
                .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                .allowedMethod(io.vertx.core.http.HttpMethod.POST)
                .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.DELETE)
                .allowedHeader("ENCTYPE")
                .allowedHeader("Access-Control-Request-Method")
                .allowedHeader("Access-Control-Allow-Credentials")
                .allowedHeader("Access-Control-Allow-Origin")
                .allowedHeader("Access-Control-Allow-Headers")
                .allowedHeader("Authorization")
                .allowedHeader("Content-Type"));


        // User/Profile API
        // _____________________________

        apiRouter.route("/user*").handler(BodyHandler.create());
        apiRouter.route("/profiles/:userToFollow/follow").handler(BodyHandler.create());
        apiRouter.get("/user").handler(profileHandler::getCurrentUser);
        apiRouter.put("/user").handler(profileHandler::updateUser);
        apiRouter.post("/users").handler(profileHandler::postUser);
        apiRouter.post("/users/login").handler(profileHandler::loginUser);
        apiRouter.get("/profiles").handler(profileHandler::getProfiles);
        apiRouter.get("/profiles/:username").handler(profileHandler::getProfile);
        apiRouter.post("/profiles/:userFollowed/follow").handler(profileHandler::followUser);
        apiRouter.delete("/profiles/:usernameUnfollowed/follow").handler(profileHandler::unfollowUser);
        apiRouter.get("/users/:userId/followers").handler(profileHandler::getUserFollowers);

        // ________________________________




        apiRouter.route("/articles").handler(BodyHandler.create());
        apiRouter.route("/articles/:article/comments").handler(BodyHandler.create());
        // apiRouter.route("/images/upload").handler(BodyHandler.create());

        // apiRouter.get("/user").handler(authHandler).handler(this::getCurrentUser);

        // apiRouter.put("/user").handler(this::updateUser);








        apiRouter.post("/articles/:article/comments").handler(this::createComment);
        apiRouter.post("/articles/:article/favorite").handler(this::favorite);
        apiRouter.delete("/articles/:article/favorite").handler(this::unfavorite);
        apiRouter.post("/articles/:article").handler(this::favorite);


        apiRouter.delete("/articles/:article").handler(this::deleteArticle);
        apiRouter.delete("/articles/:article/comments/:comment").handler(this::deleteComment);

        apiRouter.post("/articles").handler(this::createArticle);
        apiRouter.get("/articles/:article").handler(this::getArticle);

        apiRouter.get("/articles/:article/comments").handler(this::getArticleComments);
        apiRouter.get("/articles").handler(this::getArticles);
        apiRouter.get("/newsFeed").handler(this::getNewsFeed);


        // GROUPS
        apiRouter.route("/groups").handler(BodyHandler.create());
        apiRouter.post("/groups").handler(this::createGroup);
        apiRouter.get("/groups").handler(this::getGroups);
        apiRouter.get("/groups/:group").handler(this::getGroup);
        apiRouter.put("/groups").handler(this::updateGroup);
        apiRouter.post("/groups/:groupId/follow").handler(this::followGroup);
        apiRouter.get("/groups/:groupId/followers").handler(this::getGroupFollowers);




        // PAGES
        apiRouter.route("/pages").handler(BodyHandler.create());
        apiRouter.post("/pages").handler(this::createPage);
        apiRouter.get("/pages").handler(this::getPages);
        apiRouter.get("/pages/:page").handler(this::getPage);
        apiRouter.put("/pages").handler(this::updatePage);
        apiRouter.post("/pages/:page/follow").handler(this::followPage);
        apiRouter.delete("/pages/:page/follow").handler(this::unfollowPage);
        apiRouter.get("/pages/:page/followers").handler(this::getPageFollowers);
        apiRouter.put("/pages").handler(this::updateGroup);


        // chargers
        apiRouter.route("/chargers").handler(BodyHandler.create());
        apiRouter.get("/chargers").handler(chargerHandler::getChargers);
        apiRouter.get("/chargers/:charger").handler(chargerHandler::getCharger);
        apiRouter.post("/chargers").handler(this::createCharger);


        apiRouter.get("/search").handler(this::search);


        apiRouter.get("/test").handler(rc -> {
            rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            rc.response().end("ok");
        });


        // Allow outbound traffic to the vtoons addresses
        BridgeOptions options = new BridgeOptions()

                // all outbound messages are permitted
                .addInboundPermitted(new PermittedOptions())
                .addOutboundPermitted(new PermittedOptions());

        // apiRouter.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));

        apiRouter.route("/eventbus/*").handler(eventBusHandler());

        return  apiRouter;

    }




    private SockJSHandler eventBusHandler() {
        SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);

        BridgeOptions boptions = new BridgeOptions()

                // all outbound messages are permitted
                .addInboundPermitted(new PermittedOptions())
                .addOutboundPermitted(new PermittedOptions());

        return SockJSHandler.create(vertx, options).bridge(boptions, event -> {
            if (event.type() == BridgeEventType.SOCKET_CREATED) {
                System.out.println("A socket was created!");
            } else if (event.type() == BridgeEventType.SOCKET_CLOSED){
                System.out.println("A socket was closed!");
            } else if (event.type() == BridgeEventType.UNREGISTER || event.type() == BridgeEventType.REGISTER){
                System.out.println(event.getRawMessage().encode());
            }
            event.complete(true);
        });
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




    private void registerUser(RoutingContext routingContext) {

        LOGGER.debug("insert successful:");
        new UserClient(vertx).findCustomerAccounts(routingContext, res2 -> {
            LOGGER.info("aaa");

            JsonObject a = res2.result().toJsonObject();


            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("Content-Type", "application/json; charset=utf-8")
                    //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
                    .end(Json.encodePrettily(a));

        });

    }



    private void registerUserMessage(RoutingContext routingContext) {

        JsonObject message = new JsonObject()
                .put("action", "action.register")
                .put("user", routingContext.getBodyAsJson().getJsonObject("user"));


        System.out.println(message.getJsonObject("user"));
        vertx.eventBus().send("address.login", message, ar -> {
            if (ar.succeeded()) {
                JsonObject a = (JsonObject) ar.result().body();


                routingContext.response()
                        .setStatusCode(201)
                        .putHeader("Content-Type", "application/json; charset=utf-8")
                        //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
                        .end(Json.encodePrettily(a));
            } else {

            }
        });

    }





    private void getArticles (RoutingContext ctx) {


        String headerAuth = ctx.request().getHeader("Authorization");
        String[] values = headerAuth.split(" ");

        JsonObject query = new JsonObject();
        final String author = ctx.request().getParam("author");
        final String page = ctx.request().getParam("page");
        final String type = ctx.request().getParam("type");
        final String groupId = ctx.request().getParam("groupId");
        final String objectId = ctx.request().getParam("objectId");

        if (author!= null) {
            query.put("author.username", author);
        }

        if (page!= null) {
            query.put("articles.page", page);
        }

        if (type!= null) {
            query.put("articles.type", type);
        }

        if (groupId!= null) {
            query.put("articles.groupId", groupId);
        }

        if (objectId!= null) {
            query.put("articles.objectId", objectId);
        }

        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();

                query.put("user", principal);

                vertx.eventBus().send("/api/getArticles", query, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));


            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });


    }




    private void getNewsFeed (RoutingContext ctx) {

        String headerAuth = null;
        String[] values = null;

        if (ctx.request().getHeader("Authorization") != null)  {
            headerAuth = ctx.request().getHeader("Authorization");
            values = headerAuth.split(" ");
        }



        JsonObject query = new JsonObject();

        final String page = ctx.request().getParam("page");
        final String type = ctx.request().getParam("type");

        if (page!= null) {
            query.put("articles.page", page);
        }

        if (type!= null) {
            query.put("articles.type", type);
        }



        if (headerAuth != null) {
            jwtAuth.authenticate(new JsonObject()
                    .put("jwt", values[1]), res -> {
                if (res.succeeded()) {
                    io.vertx.ext.auth.User theUser = res.result();
                    JsonObject principal = theUser.principal();

                    query.put("user", principal);

                    vertx.eventBus().send("/api/getNewsFeed", query, (Handler<AsyncResult<Message<JsonObject>>>)
                            responseHandler -> defaultResponse(ctx, responseHandler));


                } else {
                    //failed!
                    System.out.println("authentication failed ");
                }

            });
        } else {

            vertx.eventBus().send("/api/getNewsFeed", query, (Handler<AsyncResult<Message<JsonObject>>>)
                    responseHandler -> defaultResponse(ctx, responseHandler));

        }



    }



    private void getGroups (RoutingContext ctx) {

        JsonObject query = new JsonObject();

        final String page = ctx.request().getParam("page");

        final String searchQuery = ctx.request().getParam("query");

        if (page!= null) {
            query.put("groups.page", page);
        }


        if (searchQuery!= null) {
            query.put("groups.query", searchQuery);
        }

        vertx.eventBus().send("/api/getGroups", query, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));
    }




    private void getArticleComments (RoutingContext ctx) {

        JsonObject query = new JsonObject();
        final String article = ctx.request().getParam("article");
        query.put("article", article);

        vertx.eventBus().send("/api/getArticleComments", query, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));

        System.out.println("headerAuth:");


    }


    private void createComment (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        String[] values = headerAuth.split(" ");


        final String article = ctx.request().getParam("article");
        JsonObject comment = ctx.getBodyAsJson();


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                JsonObject message = new JsonObject().put("user", principal).put ("article", article).put("comment", comment);

                vertx.eventBus().send("/api/createArticleComment", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }



    private void favorite (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String article = ctx.request().getParam("article");


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());



                JsonObject message = new JsonObject().put("user", principal).put ("article", article).put("favorited", true);

                vertx.eventBus().send("/api/articleFavorite", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }




    private void unfavorite (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String article = ctx.request().getParam("article");


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());


                JsonObject message = new JsonObject().put("user", principal).put ("article", article);

                vertx.eventBus().send("/api/articleUnFavorite", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));


            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }





    private void getArticle (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String article = ctx.request().getParam("article");


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());


                JsonObject message = new JsonObject().put("user", principal).put ("article", article);

                vertx.eventBus().send("/api/getArticle", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }



    private void getGroup (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String group = ctx.request().getParam("group");


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());


                JsonObject message = new JsonObject().put("user", principal).put ("group", group);

                vertx.eventBus().send("/api/getGroup", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }





    private void deleteComment (RoutingContext ctx) {
        // String headerAuth = ctx.request().getHeader("Authorization");
        // System.out.println("headerAuth: " + headerAuth);

        // String[] values = headerAuth.split(" ");
        // System.out.println("values[1]: " + values[1]);

        final String comment = ctx.request().getParam("comment");
        // JsonObject comment = ctx.getBodyAsJson();


        JsonObject message = new JsonObject().put("comment", comment);

        vertx.eventBus().send("/api/deleteComment", message, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));
    }



    private void deleteArticle (RoutingContext ctx) {
        // String headerAuth = ctx.request().getHeader("Authorization");
        // System.out.println("headerAuth: " + headerAuth);

        // String[] values = headerAuth.split(" ");
        // System.out.println("values[1]: " + values[1]);

        final String article = ctx.request().getParam("article");
        // JsonObject comment = ctx.getBodyAsJson();


        JsonObject message = new JsonObject().put("article", article);

        vertx.eventBus().send("/api/deleteArticle", message, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));
    }



    private void createArticle (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        JsonObject article = ctx.getBodyAsJson();


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());



                JsonObject message = new JsonObject().put("user", principal).put("article", article);

                vertx.eventBus().send("/api/createArticle", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponseWithEventBus(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }


    private void createGroup (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        JsonObject group = ctx.getBodyAsJson();


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());



                JsonObject message = new JsonObject().put("user", principal).put("group", group);

                vertx.eventBus().send("/api/createGroup", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }









    public void updateGroup (RoutingContext ctx) {
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

                JsonObject message = new JsonObject().put("fields", newUserValuesJson);

                vertx.eventBus().send("/api/updateGroup", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }


    private void defaultResponse(RoutingContext ctx, AsyncResult<Message<JsonObject>> responseHandler) {
        if (responseHandler.failed()) {
            ctx.fail(500);
        } else {
            //  final Message<String> result = responseHandler.result();
            JsonObject a = responseHandler.result().body();

            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            ctx.response().end(Json.encodePrettily(a));
        }
    }



    private void defaultResponseWithEventBus(RoutingContext ctx, AsyncResult<Message<JsonObject>> responseHandler) {
        if (responseHandler.failed()) {
            ctx.fail(500);
        } else {
            //  final Message<String> result = responseHandler.result();
            JsonObject response = responseHandler.result().body();


            vertx.eventBus().publish("public", response);

            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            ctx.response().end(Json.encodePrettily(response));
        }
    }



    public void followGroup (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String groupId = ctx.request().getParam("groupId");



        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());



                JsonObject message = new JsonObject().put("user", principal).put ("groupId", groupId);

                vertx.eventBus().send("/api/followGroup", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }


    private void getGroupFollowers (RoutingContext ctx) {

        JsonObject query = new JsonObject();
        final String groupId = ctx.request().getParam("groupId");
        query.put("groupId", groupId);

        vertx.eventBus().send("/api/getGroupFollowers", query, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));

        System.out.println("headerAuth:");


    }




    private void createPage (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        JsonObject page = ctx.getBodyAsJson();


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());

                JsonObject message = new JsonObject().put("user", principal).put("page", page);

                vertx.eventBus().send("/api/createPage", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }









    private void getPages (RoutingContext ctx) {

        JsonObject query = new JsonObject();

        final String page = ctx.request().getParam("page");

        final String searchQuery = ctx.request().getParam("query");

        if (page!= null) {
            query.put("pages.page", page);
        }


        if (searchQuery!= null) {
            query.put("pages.query", searchQuery);
        }

        vertx.eventBus().send("/api/getPages", query, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));
    }




    private void getPage (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String page = ctx.request().getParam("page");


        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());


                JsonObject message = new JsonObject().put("user", principal).put ("page", page);

                vertx.eventBus().send("/api/getPage", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }




    public void updatePage (RoutingContext ctx) {
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

                JsonObject message = new JsonObject().put("fields", newUserValuesJson);

                vertx.eventBus().send("/api/updatePage", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }





    public void followPage (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String page = ctx.request().getParam("page");



        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());

                JsonObject message = new JsonObject().put("user", principal).put ("page", page);

                vertx.eventBus().send("/api/followPage", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }





    public void unfollowPage (RoutingContext ctx) {
        String headerAuth = ctx.request().getHeader("Authorization");
        System.out.println("headerAuth: " + headerAuth);

        String[] values = headerAuth.split(" ");
        System.out.println("values[1]: " + values[1]);

        final String page = ctx.request().getParam("page");



        jwtAuth.authenticate(new JsonObject()
                .put("jwt", values[1]), res -> {
            if (res.succeeded()) {
                io.vertx.ext.auth.User theUser = res.result();
                JsonObject principal = theUser.principal();
                System.out.println("theUser: " + theUser.principal().encodePrettily());

                JsonObject message = new JsonObject().put("user", principal).put ("page", page);

                vertx.eventBus().send("/api/unfollowPage", message, (Handler<AsyncResult<Message<JsonObject>>>)
                        responseHandler -> ResponseHandler.defaultResponse(ctx, responseHandler));

            } else {
                //failed!
                System.out.println("authentication failed ");
            }

        });
    }



    private void getPageFollowers (RoutingContext ctx) {

        JsonObject query = new JsonObject();
        final String page = ctx.request().getParam("page");
        query.put("page", page);

        vertx.eventBus().send("/api/getPageFollowers", query, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));

        System.out.println("headerAuth:");


    }



    private void search (RoutingContext ctx) {

        JsonObject query = new JsonObject();

        final String searchQuery = ctx.request().getParam("query");


        final String page = ctx.request().getParam("page");

        if (searchQuery!= null) {
            query.put("pages.query", searchQuery);
        }

        if (searchQuery!= null) {
            query.put("groups.query", searchQuery);
        }

        if (page!= null) {
            query.put("groups.page", page);
        }

        if (page!= null) {
            query.put("pages.page", page);
        }



        List<String> urls = Arrays.asList(
                "/api/getPages",
                "/api/getGroups");

        List<Future> futures = new ArrayList<>();


        urls.forEach(url -> {
            Future<Object> future = Future.future();
            vertx.eventBus().send(url, query, result-> {
                LOGGER.info(result);
                future.complete(result);
            });
            futures.add(future);
        });

        long time = System.currentTimeMillis();
        CompositeFuture.all(futures).setHandler( (AsyncResult<CompositeFuture> composedResult)-> {

            JsonArray subResults = new JsonArray();


            for (Object singleResult : composedResult.result().list()) {
                AsyncResult<Message> asyncResult = (AsyncResult<Message>) singleResult;
                subResults.add (asyncResult.result().body());
            }


            JsonObject result = new JsonObject();
            result.put("result", subResults);

            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            ctx.response().end(Json.encodePrettily(result));


            System.out.println("Complete in " + (System.currentTimeMillis() - time));
        });


    }





    private void createCharger (RoutingContext ctx) {


        JsonObject charger = ctx.getBodyAsJson();


        JsonObject message = new JsonObject().put("charger", charger);

        vertx.eventBus().send("/api/createCharger", message, (Handler<AsyncResult<Message<JsonObject>>>)
                responseHandler -> defaultResponse(ctx, responseHandler));

    }



}
