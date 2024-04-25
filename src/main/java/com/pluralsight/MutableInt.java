// Copyright (c) Benjamin Bergman 2024.

package com.pluralsight;

final class MutableInt {
    private int value;

    MutableInt(int value) {
        this.value = value;
    }

    void increment() {
        value++;
    }

    void decrement() {
        value--;
    }

    int intValue() {
        return value;
    }
}
