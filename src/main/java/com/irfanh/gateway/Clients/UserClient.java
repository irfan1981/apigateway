package com.irfanh.gateway.Clients;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class UserClient {

    private Vertx vertx;

    public UserClient(Vertx vertx) {
        this.vertx = vertx;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(UserClient.class);

    public UserClient findCustomerAccounts (RoutingContext routingContext, Handler<AsyncResult<Buffer>> resultHandler) {


        Buffer buffer = routingContext.getBody();

        WebClient client = WebClient.create(vertx);

        int initialOffset = 4; // length of `/api`

        String path = routingContext.request().uri();

        String newPath = (path.substring(initialOffset));


        if (routingContext.request().method() == HttpMethod.POST) {
            client
                    .post(8080, "localhost", newPath)
                    .sendBuffer(buffer, ar -> {
                        if (ar.succeeded()) {
                            // Ok
                            resultHandler.handle(Future.succeededFuture(ar.result().body()));
                        }
                    });
        }



        return this;
    }
}
