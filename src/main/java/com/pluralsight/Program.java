// Copyright (c) Benjamin Bergman 2024.

package com.pluralsight;

import java.io.*;
import java.util.*;
import java.util.function.*;

final class Program implements AutoCloseable {
    private final Collection<Product> products;
    private final Map<Product, MutableInt> shoppingCart;
    private final Scanner scanner;

    private Program(Collection<Product> products) {
        this.products = products;
        shoppingCart = new HashMap<>();
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
        StorePage currentPage = new StorePage(this::showHomePage);
        do currentPage = currentPage.show(this);
        while (!currentPage.shouldExit());
        System.out.println("Thank you for using our shop!");
    }

    private Optional<Integer> readInt() {
        var s = scanner.next();
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private StorePage showHomePage() {
        System.out.print("""
            Choose an option:
             1 - Browse Products
             2 - View Cart
             3 - Exit
            >\s""");

        return switch (readInt().orElse(0)) {
            case 1 -> new StorePage(() -> showProducts(ProductSorter.noSort(), new ArrayList<>()));
            case 2 -> new StorePage(this::showCart);
            case 3 -> new StorePage();
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield new StorePage(this::showHomePage);
            }
        };
    }

    @SuppressWarnings({"ReassignedVariable", "FeatureEnvy"})
    private StorePage showProducts(ProductSorter sort, Collection<ProductFilter> filter) {
        var searched = products
            .stream()
            .filter(product -> filter.stream().allMatch(pred -> pred.test(product)))
            .sorted(sort)
            .limit(5)
            .toArray(Product[]::new);

        System.out.printf(
            "There are %d products available. Showing %d based on your filters.%n",
            products.size(),
            searched.length);

        for (int i = 0; i < searched.length; i++) {
            var product = searched[i];
            System.out.printf("%d\t%s\t%s\t$%.2f%n", i + 1, product.department(), product.productName(), product.price());
        }

        System.out.print("""
            Choose an option:
            1 - Search
            2 - Add to cart
            3 - Go Back
            >\s""");
        return new StorePage(() -> switch (readInt().orElse(0)) {
            case 1 -> showChangeSearchOptions(sort, filter);
            case 2 -> showAddToCart(searched, sort, filter);
            case 3 -> showHomePage();
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield showProducts(sort, filter);
            }
        });
    }

    private StorePage showAddToCart(Product[] searched, ProductSorter sort, Collection<ProductFilter> filter) {
        if (searched.length == 0) {
            System.out.println("There's nothing to add to cart!");
            return new StorePage(() -> showProducts(sort, filter));
        }
        System.out.printf("Which product would you like to add to cart?%n> ");
        int choice = readInt().orElse(-1);
        if ((choice < 1) || (choice > searched.length)) {
            System.out.println("Sorry, I don't understand.");
            return new StorePage(() -> showProducts(sort, filter));
        }
        shoppingCart
            .computeIfAbsent(searched[choice - 1], x -> new MutableInt(0))
            .increment();
        printCartStatus(true);
        return new StorePage(() -> showProducts(sort, filter));
    }

    private void printCartStatus(boolean updated) {
        int total = shoppingCart.values().stream().map(MutableInt::intValue).reduce(0, Integer::sum);
        System.out.printf("There %s%s %d item%s in your cart.",
            (total == 1) ? "is" : "are",
            updated ? " now" : "",
            total,
            (total == 1) ? "" : "s"
        );
    }

    private StorePage showChangeSearchOptions(ProductSorter sort, Collection<ProductFilter> filter) {
        System.out.printf("""
                %s
                You have %d filter%s applied.
                Choose an option:
                1 - Change sorting mode
                2 - Apply filter
                3 - Remove filter
                4 - Go Back
                >\s""",
            sort.isDefault() ? "There is no sort applied." : ("You are sorting by " + sort + '.'),
            filter.size(),
            (filter.size() == 1) ? "" : "s"
        );
        return new StorePage(() -> switch (readInt().orElse(-1)) {
            case 1 -> showChangeSortingMode(sort, filter);
            case 2 -> showAddFilters(sort, filter);
            case 3 -> showRemoveFilters(sort, filter);
            case 4 -> showProducts(sort, filter);
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield showChangeSearchOptions(sort, filter);
            }
        });
    }

    private StorePage showRemoveFilters(ProductSorter sort, Collection<ProductFilter> filter) {
        throw new RuntimeException();
    }

    private StorePage showAddFilters(ProductSorter sort, Collection<ProductFilter> filter) {
        System.out.print("""
            Filter by what?
            1 - Price (Maximum)
            2 - Price (Minimum)
            3 - Department
            4 - Name
            5 - Go Back
            >\s""");
        return switch (readInt().orElse(-1)) {
            case 1 -> new StorePage(() -> showPriceFilter(sort, filter, true));
            case 2 -> new StorePage(() -> showPriceFilter(sort, filter, false));
            case 3 -> new StorePage(() -> showDepartmentFilter(sort, filter));
            case 4 -> new StorePage(() -> showNameFilter(sort, filter));
            case 5 -> new StorePage(() -> showChangeSearchOptions(sort, filter));
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield new StorePage(() -> showAddFilters(sort, filter));
            }
        };
    }

    private StorePage showNameFilter(ProductSorter sort, Collection<ProductFilter> filter) {
        System.out.print("""
            What are you looking for?
            >\s""");
        var search = scanner.next();
        filter.add(new ProductFilter(p -> p.productName().toLowerCase().contains(search.toLowerCase()), "Contains \"%s\"".formatted(search.toLowerCase())));
        return new StorePage(() -> showChangeSearchOptions(sort, filter));
    }

    @SuppressWarnings("ReassignedVariable")
    private StorePage showDepartmentFilter(ProductSorter sort, Collection<ProductFilter> filter) {
        System.out.println("Which department are you looking at?");
        var deps = products.stream().map(Product::department).toArray(String[]::new);
        for (int i = 0; i < deps.length; i++)
            System.out.printf("%d - %s%n", i + 1, deps[i]);
        System.out.print("> ");
        int choice = readInt().orElse(-1);
        if ((choice < 1) || (choice > deps.length)) {
            System.out.println("Sorry, I don't understand.");
            return new StorePage(() -> showDepartmentFilter(sort, filter));
        }
        var dep = deps[choice - 1];
        filter.add(new ProductFilter(p -> p.department().equals(dep), "In " + dep));
        return new StorePage(() -> showChangeSearchOptions(sort, filter));
    }

    private StorePage showPriceFilter(ProductSorter sort, Collection<ProductFilter> filter, boolean isMax) {
        System.out.printf("""
            What's the %s price?
            >\s""", isMax ? "maximum" : "minimum");
        var command = scanner.next();
        double limit;
        try {
            limit = Double.parseDouble(command);
        } catch (NumberFormatException e) {
            System.out.println("Sorry, I don't understand.");
            return new StorePage(() -> showPriceFilter(sort, filter, isMax));
        }
        if (isMax)
            filter.add(new ProductFilter(p -> p.price() < limit, "Price < " + limit));
        else
            filter.add(new ProductFilter(p -> p.price() > limit, "Price > " + limit));
        return new StorePage(() -> showChangeSearchOptions(sort, filter));
    }

    private StorePage showChangeSortingMode(ProductSorter sort, Collection<ProductFilter> filter) {
        System.out.print("""
            1 - Reset sort
            2 - Sort by name
            3 - Sort by price
            4 - Sort by department
            5 - Go back
            >\s""");
        int command = readInt().orElse(-1);
        if (command == 5) return new StorePage(() -> showChangeSearchOptions(sort, filter));
        return new StorePage(() -> showChangeSortingMode(switch (command) {
            case 1 -> ProductSorter.noSort();
            case 2 -> sort.thenComparing(Comparator.comparing(Product::productName), "Name");
            case 3 -> sort.thenComparing(Comparator.comparing(Product::price), "Price");
            case 4 -> sort.thenComparing(Comparator.comparing(Product::department), "Department");
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield sort;
            }
        }, filter));
    }

    private StorePage showCart() {
        throw new RuntimeException();
    }

    private record ProductSorter(boolean isDefault, Comparator<Product> comparator,
                                 String description) implements Comparator<Product> {

        static ProductSorter noSort() {
            //noinspection UnnecessarilyQualifiedStaticUsage
            return new ProductSorter(true, Comparator.naturalOrder(), "");
        }

        @Override
        public int compare(Product o1, Product o2) {
            return comparator.compare(o1, o2);
        }

        public ProductSorter thenComparing(Comparator<? super Product> other, String newDescription) {
            return new ProductSorter(false, comparator.thenComparing(other), isDefault ? newDescription : (description + ", then by " + newDescription));
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private record ProductFilter(Predicate<Product> predicate,
                                 String description) implements Predicate<Product> {
        @Override
        public boolean test(Product product) {
            return predicate.test(product);
        }
    }

    private static final class StorePage {
        private final Supplier<StorePage> handler;

        StorePage() {
            //noinspection AssignmentToNull
            handler = null;
        }

        StorePage(Supplier<StorePage> handler) {
            this.handler = handler;
        }

        boolean shouldExit() {
            return handler == null;
        }

        StorePage show(Program program) {
            return (handler == null) ? new StorePage() : handler.get();
        }
    }
}
