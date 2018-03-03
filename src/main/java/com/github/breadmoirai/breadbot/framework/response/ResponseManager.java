package com.github.breadmoirai.breadbot.framework.response;

import gnu.trove.TCollections;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ResponseManager implements EventListener {

    private final TLongObjectMap<Reference<DynamicCommandResponse>> linkMap;
    private final Map<Class<?>, List<Reference<DynamicCommandResponse>>> typeMap;

    public ResponseManager() {
        linkMap = TCollections.synchronizedMap(new TLongObjectClearingHashMap<>());
        typeMap = new HashMap<>();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageDeleteEvent) {
            final long id = ((MessageDeleteEvent) event).getMessageIdLong();
            final Reference<DynamicCommandResponse> ref = linkMap.get(id);
            if (ref == null)
                return;
            final DynamicCommandResponse res = ref.get();
            if (res == null)
                return;
            res.cancel();
        } else if (event instanceof ShutdownEvent) {
            linkMap.forEachValue(ref -> {
                final DynamicCommandResponse resp = ref.get();
                if (resp != null) {
                    resp.cancel();
                }
                return true;
            });
        }
    }

    private void sendResponse(CommandResponse response) {
        if (response instanceof DynamicCommandResponse) {
            final DynamicCommandResponse cpoll = (DynamicCommandResponse) response;
            if (!cpoll.matches(null)) {
                final List<Reference<DynamicCommandResponse>> refList = typeMap.computeIfAbsent(
                        cpoll.getClass(), c -> Collections.synchronizedList(new LinkedClearingList<>()));
                refList.stream()
                       .map(Reference::get)
                       .filter(cpoll::matches)
                       .findFirst()
                       .ifPresent(DynamicCommandResponse::cancel);
            }
            response.dispatch(value -> linkMap.put(value, new WeakReference<>(cpoll)));
        } else {
            response.dispatch(value -> {
            });
        }
    }

    public EventResponseManager newEventResponseManager() {
        return new EventResponseManagerImpl();
    }

    private static class LinkedClearingList<T> extends LinkedList<Reference<T>> {

        private int n = 10;

        private int i = -5;

        @Override
        public boolean add(Reference<T> reference) {
            final boolean add = super.add(reference);
            if (i++ > n) {
                n = size() / 4;
                i = 0;
                removeIf(ref -> ref.get() == null);
            }
            return add;
        }
    }

    private static class TLongObjectClearingHashMap<T> extends TLongObjectHashMap<Reference<T>> {

        private int n = 100;

        private int i = -300;

        public TLongObjectClearingHashMap() {
        }

        @Override
        public Reference<T> put(long key, Reference<T> value) {
            final Reference<T> put = super.put(key, value);
            if (i++ > n) {
                n = size() / 4;
                i = 0;
                retainEntries((a, b) -> b.get() != null);
            }
            return put;
        }

    }

    private class EventResponseManagerImpl implements EventResponseManager {

        private final Queue<CommandResponse> queue = new ArrayDeque<>();

        @Override
        public void accept(CommandResponse response) {
            queue.add(response);
        }

        @Override
        public void complete() {
            while (!queue.isEmpty()) {
                sendResponse(queue.poll());
            }
        }

    }
}
