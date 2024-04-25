// Copyright (c) Benjamin Bergman 2024.

package com.pluralsight;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.Map.*;
import java.util.function.*;
import java.util.stream.*;

@SuppressWarnings("ClassWithTooManyMethods")
final class Program implements AutoCloseable {
    private static final int MAX_PRODUCTS_PER_PAGE = 5;
    private final Collection<Product> products;
    private final Map<Product, MutableInt> shoppingCart;
    private final Scanner scanner;
    private final List<ProductFilter> filter;
    private ProductSorter sorter;

    private Program(Collection<Product> products) {
        this.products = products;
        shoppingCart = new HashMap<>();
        scanner = new Scanner(System.in);
        sorter = ProductSorter.noSort();
        filter = new ArrayList<>();
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
        do currentPage = currentPage.show();
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
            case 1 -> new StorePage(this::showProducts);
            case 2 -> new StorePage(this::showCart);
            case 3 -> new StorePage();
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield new StorePage(this::showHomePage);
            }
        };
    }

    @SuppressWarnings({"ReassignedVariable", "FeatureEnvy"})
    private StorePage showProducts() {

        var searched = products
            .stream()
            .filter(product -> filter.stream().allMatch(pred -> pred.test(product)))
            .sorted(sorter)
            .toArray(Product[]::new);

        // noinspection HardcodedFileSeparator
        System.out.printf(
            "There are %d products available. Showing %s%d based on your filters.%n",
            products.size(),
            (searched.length > MAX_PRODUCTS_PER_PAGE) ? (MAX_PRODUCTS_PER_PAGE + "/") : "",
            searched.length);

        for (int i = 0; i < Math.min(searched.length, MAX_PRODUCTS_PER_PAGE); i++) {
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
            case 1 -> showChangeSearchOptions();
            case 2 -> showAddToCart(searched);
            case 3 -> showHomePage();
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield showProducts();
            }
        });
    }

    private StorePage showAddToCart(Product[] searched) {
        if (searched.length == 0) {
            System.out.println("There's nothing to add to cart!");
            return new StorePage(this::showProducts);
        }
        System.out.printf("Which product would you like to add to cart?%n> ");
        int choice = readInt().orElse(-1);
        if ((choice < 1) || (choice > searched.length)) {
            System.out.println("Sorry, I don't understand.");
            return new StorePage(this::showProducts);
        }
        shoppingCart
            .computeIfAbsent(searched[choice - 1], x -> new MutableInt(0))
            .increment();
        printCartStatus(true);
        return new StorePage(this::showProducts);
    }

    private void printCartStatus(boolean updated) {
        int total = cartCount();
        System.out.printf("There %s%s %d item%s in your cart.%n",
            (total == 1) ? "is" : "are",
            updated ? " now" : "",
            total,
            (total == 1) ? "" : "s"
        );
    }

    private int cartCount() {
        return shoppingCart.values().stream().map(MutableInt::intValue).reduce(0, Integer::sum);
    }

    private StorePage showChangeSearchOptions() {
        System.out.printf("""
                %s
                You have %d filter%s applied.
                Choose an option:
                1 - Change sorting mode
                2 - Apply filter
                3 - Remove filter
                4 - Go Back
                >\s""",
            sorter.isDefault() ? "There is no sort applied." : ("You are sorting by " + sorter + '.'),
            filter.size(),
            (filter.size() == 1) ? "" : "s"
        );
        return new StorePage(() -> switch (readInt().orElse(-1)) {
            case 1 -> showChangeSortingMode();
            case 2 -> showAddFilters();
            case 3 -> showRemoveFilters();
            case 4 -> showProducts();
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield showChangeSearchOptions();
            }
        });
    }

    @SuppressWarnings("ReassignedVariable")
    private StorePage showRemoveFilters() {
        if (filter.isEmpty()) {
            System.out.println("There's nothing to remove!");
            return new StorePage(this::showChangeSearchOptions);
        }
        System.out.println("Which filter would you like to remove?");
        var array = filter.toArray(ProductFilter[]::new);
        for (int i = 0; i < array.length; i++) System.out.printf("%d - %s%n", i + 1, array[i].description());
        System.out.print("> ");
        int i = readInt().orElse(-1);
        if ((i < 1) || (i > array.length))
            System.out.println("Sorry, I don't understand.");
        else
            filter.remove(i - 1);
        return new StorePage(this::showChangeSearchOptions);
    }

    private StorePage showAddFilters() {
        System.out.print("""
            Filter by what?
            1 - Price (Maximum)
            2 - Price (Minimum)
            3 - Department
            4 - Name
            5 - Go Back
            >\s""");
        return switch (readInt().orElse(-1)) {
            case 1 -> new StorePage(() -> showPriceFilter(true));
            case 2 -> new StorePage(() -> showPriceFilter(false));
            case 3 -> new StorePage(this::showDepartmentFilter);
            case 4 -> new StorePage(this::showNameFilter);
            case 5 -> new StorePage(this::showChangeSearchOptions);
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield new StorePage(this::showAddFilters);
            }
        };
    }

    private StorePage showNameFilter() {
        System.out.print("""
            What are you looking for?
            >\s""");
        var search = scanner.next();
        filter.add(new ProductFilter(p -> p.productName().toLowerCase().contains(search.toLowerCase()), "Contains \"%s\"".formatted(search.toLowerCase())));
        return new StorePage(this::showAddFilters);
    }

    @SuppressWarnings("ReassignedVariable")
    private StorePage showDepartmentFilter() {
        System.out.println("Which department are you looking at?");
        var deps = products.stream().map(Product::department).distinct().toArray(String[]::new);
        for (int i = 0; i < deps.length; i++)
            System.out.printf("%d - %s%n", i + 1, deps[i]);
        System.out.print("> ");
        int choice = readInt().orElse(-1);
        if ((choice < 1) || (choice > deps.length)) {
            System.out.println("Sorry, I don't understand.");
            return new StorePage(this::showDepartmentFilter);
        }
        var dep = deps[choice - 1];
        filter.add(new ProductFilter(p -> p.department().equals(dep), "In " + dep));
        return new StorePage(this::showAddFilters);
    }

    private StorePage showPriceFilter(boolean isMax) {
        System.out.printf("""
            What's the %s price?
            >\s""", isMax ? "maximum" : "minimum");
        var command = scanner.next();
        double limit;
        try {
            limit = Double.parseDouble(command);
        } catch (NumberFormatException e) {
            System.out.println("Sorry, I don't understand.");
            return new StorePage(() -> showPriceFilter(isMax));
        }
        if (isMax)
            filter.add(new ProductFilter(p -> p.price() < limit, "Price < " + limit));
        else
            filter.add(new ProductFilter(p -> p.price() > limit, "Price > " + limit));
        return new StorePage(this::showAddFilters);
    }

    private StorePage showChangeSortingMode() {
        System.out.print("""
            1 - Reset sort
            2 - Sort by name
            3 - Sort by price
            4 - Sort by department
            5 - Go back
            >\s""");
        int command = readInt().orElse(-1);
        if (command == 5) return new StorePage(this::showChangeSearchOptions);
        sorter = switch (command) {
            case 1 -> ProductSorter.noSort();
            case 2 -> sorter.thenComparing(Comparator.comparing(Product::productName), "Name");
            case 3 -> sorter.thenComparing(Comparator.comparing(Product::price), "Price");
            case 4 -> sorter.thenComparing(Comparator.comparing(Product::department), "Department");
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield sorter;
            }
        };
        return new StorePage(this::showChangeSortingMode);
    }

    @SuppressWarnings("ReassignedVariable")
    private StorePage showCart() {
        printCartStatus(false);
        var arr = shoppingCart
            .entrySet()
            .stream()
            .sorted((e1, e2) -> sorter.compare(e1.getKey(), e2.getKey()))
            .toList();

        for (int i = 0; i < arr.size(); i++)
            System.out.printf("%d - %dx %.2f %s%n", i + 1, arr.get(i).getValue().intValue(), arr.get(i).getKey().price(), arr.get(i).getKey().productName());

        System.out.println("""
            What would you like to do?
            1 - Check out
            2 - Remove an item
            3 - Go back
            >\s""");

        return new StorePage(switch (readInt().orElse(-1)) {
            case 1 -> this::showCheckOut;
            case 2 -> () -> showRemoveItem(arr);
            case 3 -> this::showHomePage;
            default -> {
                System.out.println("Sorry, I don't understand.");
                yield this::showCart;
            }
        });
    }

    private StorePage showRemoveItem(List<? extends Entry<Product, MutableInt>> entries) {
        System.out.print("""
            Which item to remove?
            >\s""");
        int choice = readInt().orElse(-1);
        if ((choice < 1) || (choice > entries.size())) System.out.println("Sorry, I don't understand.");
        else {
            MutableInt count = entries.get(choice - 1).getValue();
            count.decrement();
            if (count.intValue() == 0)
                entries.remove(choice - 1);
        }
        printCartStatus(true);
        return new StorePage(this::showCart);
    }

    @SuppressWarnings("FeatureEnvy")
    private StorePage showCheckOut() {
        double totalPrice = shoppingCart.entrySet().stream().map(e -> e.getValue().intValue() * e.getKey().price()).reduce(0.0, Double::sum);
        System.out.printf("""
            Your total will be %.2f.
            How much cash do you have?
            >\s""", totalPrice);
        var input = scanner.next();
        double cash;
        try {
            cash = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            System.out.println("Sorry, I don't understand.");
            return new StorePage(this::showCart);
        }
        if (cash < totalPrice) {
            System.out.println("You don't have enough money!");
            return new StorePage(this::showCart);
        }
        System.out.printf("Your change is %.2f.%n", cash - totalPrice);
        System.out.printf("""
                RECEIPT:
                %Ta %<Tb %<Td, %<TY @ %<TI:%<TM %<Tp
                %s
                TOTAL: $%.2f
                PAID: $%.2f
                CHANGE: %.2f%n""",
            LocalDateTime.now(),
            shoppingCart.entrySet().stream().map(e -> "%6.2f %dx %s %s".formatted(e.getKey().price(), e.getValue().intValue(), e.getKey().productId(), e.getKey().productName())).collect(Collectors.joining(System.lineSeparator())),
            totalPrice,
            cash,
            cash - totalPrice);
        System.out.println("Thank you for shopping with us!");
        shoppingCart.clear();
        return new StorePage(this::showHomePage);
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

        @Override
        public String toString() {
            return description;
        }

        ProductSorter thenComparing(Comparator<? super Product> other, String newDescription) {
            return new ProductSorter(false, comparator.thenComparing(other), isDefault ? newDescription : (description + ", then by " + newDescription));
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

        StorePage show() {
            return (handler == null) ? new StorePage() : handler.get();
        }
    }
}
