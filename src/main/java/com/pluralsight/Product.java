// Copyright (c) Benjamin Bergman 2024.

package com.pluralsight;

record Product(String productId, String productName, double price, String department) implements Comparable<Product> {
    @Override
    public int compareTo(Product o) {
        return productId.compareTo(o.productId());
    }

    @Override
    public String toString() {
        return productId;
    }
}
