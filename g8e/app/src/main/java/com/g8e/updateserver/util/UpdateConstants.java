package com.g8e.updateserver.util;

import io.github.cdimascio.dotenv.Dotenv;

public class UpdateConstants {
    static Dotenv dotenv = Dotenv.load();

    public static final int UPDATE_SERVER_PORT = Integer.parseInt(dotenv.get("UPDATE_SERVER_PORT"));
    public static final int CACHE_VERSION = 992;
    public static final int UPDATE_REQUEST_CHECK_FOR_UPDATES = 1;
    public static final int UPDATE_RESPONSE_CACHE_UP_TO_DATE = 1;
    public static final int UPDATE_RESPONSE_UPDATE_AVAILABLE = 2;

    // Prevent instantiation
    private UpdateConstants() {
    }

}
