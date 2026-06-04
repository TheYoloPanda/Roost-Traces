package com.typ.roosttraces.pool;

import java.util.List;

import com.typ.roosttraces.roost.RoostType;

public record NodeSelectorConfig(
        boolean inheritDefault,
        List<String> defaultSelectors,
        List<String> fireSelectors,
        List<String> iceSelectors,
        List<String> lightningSelectors) {

    public static NodeSelectorConfig defaults() {
        return new NodeSelectorConfig(
                true,
                List.of("#roosttraces:create_reautomated_nodes", "#roosttraces:custom_nodes"),
                List.of("#roosttraces:fire_roost_nodes"),
                List.of("#roosttraces:ice_roost_nodes"),
                List.of("#roosttraces:lightning_roost_nodes"));
    }

    public List<String> selectorsFor(RoostType type) {
        return switch (type) {
            case FIRE -> fireSelectors;
            case ICE -> iceSelectors;
            case LIGHTNING -> lightningSelectors;
            case UNKNOWN -> defaultSelectors;
        };
    }
}
