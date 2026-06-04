package com.typ.roosttraces.roost;

import java.util.Locale;

public enum RoostType {
    FIRE("fire"),
    ICE("ice"),
    LIGHTNING("lightning"),
    UNKNOWN("unknown");

    private final String id;

    RoostType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static RoostType fromId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (RoostType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        return UNKNOWN;
    }
}
