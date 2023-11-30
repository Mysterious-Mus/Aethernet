package com.aethernet.utils.sync;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Permission {
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    private boolean permitted = false;

    public Permission(boolean permitted) {
        this.permitted = permitted;
    }

    public boolean isPermitted() {
        return permitted;
    }
    
    public void waitTillPermitted() {
        lock.lock();
        try {
            while (!permitted) {
                condition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void permit() {
        lock.lock();
        try {
            permitted = true;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

public void unpermit() {
        lock.lock();
        try {
            permitted = false;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
}