package com.kanven.leech.watcher;

import lombok.Data;

import java.nio.file.Path;

@Data
public class Event {

    private Path parent;

    private Path child;

    private Path old;

    private EventType type;

    public Event() {

    }

    public Event(EventType type, Path parent, Path child, Path old) {
        this(type, parent, child);
        this.old = old;
    }

    public Event(EventType type, Path parent, Path child) {
        this.type = type;
        this.parent = parent;
        this.child = child;
    }

    public enum EventType {
        NEW,
        MODIFY,
        RENAME,
        DELETED;
    }

    @Override
    public String toString() {
        return "Event{" +
                "parent=" + parent +
                ", child=" + child +
                ", old=" + old +
                ", type=" + type +
                '}';
    }
}