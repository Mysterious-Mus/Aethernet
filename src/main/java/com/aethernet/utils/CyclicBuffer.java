package com.aethernet.utils;

import java.util.ArrayList;

/**
 * A ring buffer that contains FIRSTINDEXWANTED(FIW)<p>
 * everything before FIRSTINDEXWANTED is discarded<p>
 * also tells the buffer feeder which index to start feeding,
 * since the FIW can be set > last index in the buffer<p>
 * 
 * cyclic manner, okay if tail - FIW <= buffer size
 */
public class CyclicBuffer<T> {
    
    private ArrayList<T> buffer;
    public int FIW; // inclusive
    private int lastIdx; // exclusive

    public CyclicBuffer(int size) {
        buffer = new ArrayList<T>();
        // push initial contents
        for (int i = 0; i < size; i++) {
            buffer.add(null);
        }
        FIW = 0;
        lastIdx = 0;
    }

    private int getBufferIdx(int idx) {
        return idx % buffer.size();
    }

    public boolean full() {
        return lastIdx - FIW >= buffer.size();
    }

    public void push(T t) {
        // if buffer is full, terminate
        if (full()) {
            System.out.println("Buffer is full, can't push more. Too much not handled or FIW not set properly.");
            System.exit(0);
        }

        buffer.set(getBufferIdx(lastIdx), t);
        lastIdx++;
    }

    /**
     * if buffer is full, discard the first element in the buffer
     * then push the new element
     */
    public void pushAndDiscard(T t) {
        if (full()) popFront();
        push(t);
    }

    public void pusharr(ArrayList<T> arr) {
        for (T t : arr) {
            push(t);
        }
    }

    public T get(int i) {
        return buffer.get(getBufferIdx(i));
    }

    /** 
     * index of the elem behind the last item(doesn't exist yet but next to feed)
     */
    public int tailIdx() {
        return lastIdx; // this should be the index of the next element to be fed
    }

    public void setFIW(int new_FIW) {
        // can't decrease
        FIW = Math.max(FIW, new_FIW);
        
        // FIW can now be larger than lastIdx, because we can sometimes skip some indices
        if (FIW > lastIdx) {
            lastIdx = FIW;
        }
    }

    public int size() {
        return lastIdx - FIW;
    }

    public T popFront() {
        if (size() == 0) {
            System.out.println("Buffer is empty, can't pop.");
            System.exit(0);
        }

        T t = buffer.get(getBufferIdx(FIW));
        FIW++;
        return t;
    }
}
