package com.auction.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;

/**
 * Lớp bọc chung cho mọi tin nhắn gửi qua Socket.
 * Mỗi tin nhắn = 1 dòng JSON (kết thúc bằng \n).
 *
 * Ví dụ JSON:
 * {"type":"LOGIN","payload":{"username":"bidder1","password":"bidder123"}}
 */
public class Message {

    private static final Gson GSON = new Gson();

    private MessageType type;
    private JsonElement payload;   // Dữ liệu đính kèm (có thể null)
    private boolean success;       // Dùng cho response
    private String errorMessage;   // Dùng khi success = false

    // ── Constructors ─────────────────────────────────────────────────────────

    public Message() {}

    /** Tạo message không có payload (VD: GET_AUCTIONS) */
    public Message(MessageType type) {
        this.type = type;
        this.success = true;
    }

    /** Tạo message có payload (serialize object thành JSON) */
    public <T> Message(MessageType type, T payloadObject) {
        this.type = type;
        this.payload = GSON.toJsonTree(payloadObject);
        this.success = true;
    }

    /** Tạo response thành công */
    public static <T> Message success(MessageType type, T data) {
        Message m = new Message(type, data);
        m.success = true;
        return m;
    }

    /** Tạo response lỗi */
    public static Message error(MessageType type, String errorMsg) {
        Message m = new Message(type);
        m.success = false;
        m.errorMessage = errorMsg;
        return m;
    }

    /** Giải mã payload thành object (Dùng cho Class thông thường) */
    public <T> T getPayload(Class<T> clazz) {
        if (payload == null) return null;
        return GSON.fromJson(payload, clazz);
    }

    /** Giải mã payload thành object cho các kiểu Generic phức tạp (như List) */
    public <T> T getPayload(Type typeOfT) {
        if (payload == null) return null;
        return GSON.fromJson(payload, typeOfT);
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    /** Chuyển thành chuỗi JSON để gửi qua mạng */
    public String toJson() {
        return GSON.toJson(this);
    }

    /** Đọc từ chuỗi JSON nhận được */
    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public MessageType getType()       { return type; }
    public boolean isSuccess()         { return success; }
    public String getErrorMessage()    { return errorMessage; }
}