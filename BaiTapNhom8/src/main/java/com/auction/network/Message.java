package com.auction.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;

public class Message {

    private static final Gson GSON = new Gson();

    private MessageType type;
    private JsonElement payload;
    private boolean success;
    private String errorMessage;

    public Message(MessageType type) {
        this.type = type;
        this.success = true;
    }

    public <T> Message(MessageType type, T payloadObject) {
        this.type = type;
        this.payload = GSON.toJsonTree(payloadObject);
        this.success = true;
    }

    public static <T> Message success(MessageType type, T data) {
        Message m = new Message(type, data);
        m.success = true;
        return m;
    }

    public static Message error(MessageType type, String errorMsg) {
        Message m = new Message(type);
        m.success = false;
        m.errorMessage = errorMsg;
        return m;
    }

    public <T> T getPayload(Class<T> clazz) {
        if (payload == null) return null;
        return GSON.fromJson(payload, clazz);
    }

    public <T> T getPayload(Type typeOfT) {
        if (payload == null) return null;
        return GSON.fromJson(payload, typeOfT);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    public MessageType getType()       { return type; }
    public boolean isSuccess()         { return success; }
    public String getErrorMessage()    { return errorMessage; }
}