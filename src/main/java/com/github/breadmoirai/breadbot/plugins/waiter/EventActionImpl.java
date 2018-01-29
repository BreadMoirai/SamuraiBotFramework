package com.github.breadmoirai.breadbot.plugins.waiter;

import net.dv8tion.jda.core.events.Event;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class EventActionImpl<E extends Event, V> implements EventAction<E, V> {
    private final Class<E> eventClass;
    private final Predicate<E> condition;
    private final Consumer<E> action;
    private final ObjectIntPredicate<E> stopper;
    private final Function<E, V> finisher;
    protected final EventActionFutureImpl<V> future;
    protected final EventWaiter waiter;

    private volatile boolean isWaiting = true;
    private boolean running = false;
    private int runCount = 0;

    public EventActionImpl(Class<E> eventClass, Predicate<E> condition, Consumer<E> action, ObjectIntPredicate<E> stopper, Function<E, V> finisher, EventWaiter waiter) {
        this.eventClass = eventClass;
        this.condition = condition;
        this.action = action;
        this.stopper = stopper;
        this.finisher = finisher;
        this.waiter = waiter;
        this.future = new EventActionFutureImpl<>(this);
    }

    @Override
    public boolean accept(Event event) {
        if (!isWaiting) return true;
        @SuppressWarnings("unchecked") final E e = (E) event;
        if (condition.test(e)) {
            if (!isWaiting) return true;
            running = true;
            waiter.removeAction(eventClass, this);
            isWaiting = false;
            action.accept(e);
            runCount++;
            if (stopper.test(e, runCount)) {
                final V result = finisher.apply(e);
                future.complete(result);
                return true;
            }
        }
        return false;
    }

    @Override
    public EventActionFuture<V> getFuture() {
        return future;
    }

    @Override
    public boolean cancel() {
        isWaiting = false;
        waiter.removeAction(eventClass, this);
        return !running;
    }


}