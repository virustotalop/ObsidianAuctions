package com.gmail.virustotalop.obsidianauctions.exception;

public class InvalidJsonException extends Exception {

    public InvalidJsonException(String json) {
        super("Invalid json '" + json + "'");
    }
}
