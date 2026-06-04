package com.typ.roosttraces.roost;

import java.util.Locale;

public final class RoostTypeResolver {
    private RoostTypeResolver() {}

    public static RoostType fromPiece(Object piece) {
        Class<?> current = piece.getClass();
        while (current != null && current != Object.class) {
            String name = current.getName().toLowerCase(Locale.ROOT);
            if (name.contains("firedragonroost")) return RoostType.FIRE;
            if (name.contains("icedragonroost")) return RoostType.ICE;
            if (name.contains("lightningdragonroost")) return RoostType.LIGHTNING;
            current = current.getSuperclass();
        }
        return RoostType.UNKNOWN;
    }
}
