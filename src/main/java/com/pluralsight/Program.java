// Copyright (c) Benjamin Bergman 2024.

package com.pluralsight;

import java.io.*;
import java.util.*;
import java.util.function.*;

final class Program implements AutoCloseable {
    private final Collection<Product> products;
    private final Scanner scanner;

    private Program(Collection<Product> products) {
        this.products = products;
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        var products = loadProducts();
        try (var program = new Program(products)) {
            program.start();
        }
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

    @Override
    public void close() {
        scanner.close();
    }

    @SuppressWarnings("ReassignedVariable")
    private void start() {
        System.out.println("Welcome to the shop! Please take a look around.");
        StorePage currentPage = StorePage.MAIN_MENU;
        do currentPage = currentPage.show(this);
        while (currentPage != StorePage.EXIT_PROGRAM);
        System.out.println("Thank you for using our shop!");
    }

    private StorePage showHomePage() {
        return StorePage.MAIN_MENU;
    }

    private StorePage showProducts() {
        return StorePage.MAIN_MENU;
    }

    private StorePage showCart() {
        return StorePage.MAIN_MENU;
    }

    private enum StorePage {
        MAIN_MENU(Program::showHomePage),
        PRODUCTS(Program::showProducts),
        VIEW_CART(Program::showCart),
        EXIT_PROGRAM;

        private final Function<? super Program, StorePage> handler;

        StorePage() {
            //noinspection AssignmentToNull
            handler = null;
        }

        StorePage(Function<? super Program, StorePage> handler) {
            this.handler = handler;
        }

        StorePage show(Program program) {
            return (handler == null) ? EXIT_PROGRAM : handler.apply(program);
        }
    }
}
