package com.muhammadallee.cameldemo;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DiskFailureStore {

    public synchronized void append(FailureEvent e) {
        // write JSON + '\n'
        // fsync
    }

    public List<FailureEvent> replay() {
        // read file on startup
        return new ArrayList<>();
    }
}
