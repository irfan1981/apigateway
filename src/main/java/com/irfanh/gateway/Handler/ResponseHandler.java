package com.irfanh.gateway.Handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ResponseHandler  {

    public static void defaultResponse (RoutingContext ctx, AsyncResult<Message<JsonObject>> responseHandler) {
        if (responseHandler.failed()) {
            ctx.fail(500);
        } else {
            //  final Message<String> result = responseHandler.result();
            JsonObject a = responseHandler.result().body();

            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            ctx.response().end(Json.encodePrettily(a));
        }
    }


}
