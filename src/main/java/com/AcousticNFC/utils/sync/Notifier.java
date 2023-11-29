package com.AcousticNFC.utils.sync;

import java.util.concurrent.Semaphore;

public class Notifier {
    
    private Semaphore taskSemaphore;

    public Notifier() {
        taskSemaphore = new Semaphore(0);
    }

    public void mWait() {
        try {
            taskSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cancelNotify() {
        taskSemaphore.drainPermits();
    }

    public void mNotify() {
        // release if no permits
        if (taskSemaphore.availablePermits() == 0) {
            taskSemaphore.release();
        }
    }

    Thread notifierThread;
    public void delayedNotify(int delay) {
        // stop the previous if exists
        if (notifierThread != null && notifierThread.isAlive()) {
            notifierThread.interrupt();
        }
        // launch the thread again to delay the notify
        notifierThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                    mNotify();
                } catch (InterruptedException e) {
                    return;
                }
            }
        };
        notifierThread.start();
    }
}
