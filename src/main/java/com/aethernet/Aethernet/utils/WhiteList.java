package com.aethernet.Aethernet.utils;

import java.net.Inet4Address;
import java.util.*;

public class WhiteList<T> {
    Set<T> whiteList = new HashSet<T>();

    public WhiteList(T[] initialEntries) {
        for (T entry : initialEntries) {
            whiteList.add(entry);
        }
    }

    public boolean contains(T x) {
        for (T entry : whiteList) {
            if (entry.equals(x)) {
                return true;
            }
        }
        return false;
    }
}
