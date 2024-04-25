// Copyright (c) Benjamin Bergman 2024.

package com.pluralsight;

final class Product {
    private final String productId;
    private final String productName;
    private final String department;
    private final double price;

    Product(String productId, String productName, double price, String department) {
        this.productId = productId;
        this.productName = productName;
        this.department = department;
        this.price = price;
    }

    String getProductId() {
        return productId;
    }

    String getProductName() {
        return productName;
    }

    String getDepartment() {
        return department;
    }

    double getPrice() {
        return price;
    }
}
