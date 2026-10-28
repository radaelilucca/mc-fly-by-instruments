package com.radaeli.aeronauticsflybyinstruments.wire;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Wire nodes loaded on the client, for the Wiring View and the wire picker. Common code, no client classes. */
public final class ClientWireIndex {
    private static final Set<WireNodeBlockEntity> NODES = Collections.newSetFromMap(new WeakHashMap<>());

    private ClientWireIndex() {}

    static synchronized void add(WireNodeBlockEntity node) {
        NODES.add(node);
    }

    static synchronized void remove(WireNodeBlockEntity node) {
        NODES.remove(node);
    }

    /** Snapshot of the live nodes whose level is still the given one. */
    public static synchronized List<WireNodeBlockEntity> nodes(Object level) {
        return NODES.stream().filter(node -> node.getLevel() == level && !node.isRemoved()).toList();
    }

    public static synchronized void clear() {
        NODES.clear();
    }
}
