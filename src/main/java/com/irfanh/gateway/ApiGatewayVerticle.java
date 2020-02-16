package com.irfanh.gateway;

import com.irfanh.gateway.Clients.UserClient;
import com.irfanh.gateway.Handler.ChargerHandler;
import com.irfanh.gateway.Handler.ProfileHandler;
import com.irfanh.gateway.Handler.ResponseHandler;
import io.vertx.core.*;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

import java.util.concurrent.*;

public class ApiGatewayVerticle extends AbstractVerticle {
    private JWTAuth jwtAuth;
    private MongoAuth loginAuthProvider;
    private ProfileHandler profileHandler;
    private ChargerHandler chargerHandler;
    private ApiRouter apiRouter;

  public static void main(String[] args) throws Exception {
        System.setProperty("vertx.disableFileCPResolving", "true");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ApiGatewayVerticle());



    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiGatewayVerticle.class);


    @Override
    public void start() {

        // Configure authentication with JWT
        jwtAuth = JWTAuth.create(vertx, new JsonObject().put("keyStore", new JsonObject()
                .put("type", "jceks")
                .put("path", "keystore.jceks")
                .put("password", "secret")));



       JWTAuthHandler authHandler = JWTAuthHandler.create(jwtAuth);
       authHandler.addAuthority("role:user");

        profileHandler = new ProfileHandler(vertx, jwtAuth);
        chargerHandler = new ChargerHandler(vertx, jwtAuth);
        apiRouter = new ApiRouter(vertx, jwtAuth);


        vertx.createHttpServer()
                // We pass the router accept method as request handler.
                .requestHandler(apiRouter.createRouter()::accept)
                .listen(8082);
    }








}
