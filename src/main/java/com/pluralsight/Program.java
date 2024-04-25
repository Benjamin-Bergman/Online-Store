// Copyright (c) Benjamin Bergman 2024.

package com.pluralsight;

import java.io.*;
import java.util.*;

final class Program {
    public static void main(String[] args) {
        var products = loadProducts();
    }

    private static Collection<Product> loadProducts() {
        try (InputStream resource = Program.class.getResourceAsStream("/products.csv")
        ) {
            if (resource == null) return Collections.emptyList();

            //noinspection NestedTryStatement
            try (InputStreamReader isr = new InputStreamReader(resource);
                 BufferedReader br = new BufferedReader(isr)) {
                Collection<Product> products = new ArrayList<>();
                br.lines().map(Program::parseLine).forEach(products::add);
                return products;
            }

        } catch (IOException ignored) {
        }

        return Collections.emptyList();
    }

    private static Product parseLine(String s) {
        String[] parts = s.split("\\|");
        return new Product(parts[0], parts[1], Double.parseDouble(parts[2]), parts[3]);
    }
}
