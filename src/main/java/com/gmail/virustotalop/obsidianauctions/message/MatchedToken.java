package com.gmail.virustotalop.obsidianauctions.message;

public class MatchedToken {

    private final String key;
    private final boolean not;
    private final int start;
    private final int end;

    public MatchedToken(String key, int start, int end) {
        this.key = this.processTokenString(key);
        this.not = this.processTokenBoolean(key);
        this.start = start;
        this.end = end;
    }

    public String getKey() {
        return this.key;
    }

    public boolean getNot() {
        return this.not;
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }

    private String processTokenString(String token) {
        return token.replace("{", "")
                .replace("}", "")
                .replace("!", "");
    }

    public boolean processTokenBoolean(String token) {
        return token.contains("!");
    }
}
