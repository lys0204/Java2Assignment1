import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OlistAnalyzer (V11 - 修正版)
 * * 最终统一逻辑，以匹配用户提供的 Q1.txt - Q5.txt 文件:
 * 1. Q4 (analyzeSellerPerformance):
 * - avgScore 使用 "所有评论条目的平均分" (逻辑 A - 匹配 Q4.txt)。
 * 2. Q5 (recommendedProducts):
 * - 过滤器: "评论条目总数" >= 5
 * - reviewCountScore (0.3): 基于 "评论条目总数"
 * - avgRatingScore (0.2): 基于 "所有评论条目的平均分"
 * - 包含所有类别，即使列表为空。
 * * 此版本旨在生成与用户提供的所有 .txt 文件一致的结果。
 */
public class OlistAnalyzer {
    // 存储从CSV读取的数据
    private List<Order> orders = new ArrayList<>();
    private List<OrderItems> orderItems = new ArrayList<>();
    private List<OrderReviews> orderReviews = new ArrayList<>();
    private List<Products> products = new ArrayList<>();
    private List<CategoryNameTranslation> categoryNameTranslations = new ArrayList<>();

    // 统一的日期时间格式化器
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数，读取指定文件夹路径下的所有CSV文件。
     * @param datasetFolderPath 包含CSV文件的文件夹路径。
     */
    public OlistAnalyzer(String datasetFolderPath) {
        readOrders(datasetFolderPath + "/olist_orders_dataset.csv");
        readOrderItems(datasetFolderPath + "/olist_order_items_dataset.csv");
        readOrderReviews(datasetFolderPath + "/olist_order_reviews_dataset.csv");
        readProducts(datasetFolderPath + "/olist_products_dataset.csv");
        readCategoryNameTranslations(datasetFolderPath + "/product_category_name_translation.csv");
    }

    // --- (CSV Reading methods - Unchanged) ---

    private void readOrders(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            br.readLine(); // 跳过表头
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                    if (info.length >= 8) {
                        Order order = new Order(
                                info[0], // order_id
                                info[1], // customer_id
                                info[2], // order_status
                                info[3].isEmpty() ? null : LocalDateTime.parse(info[3], formatter), // order_purchase_timestamp
                                info[4].isEmpty() ? null : LocalDateTime.parse(info[4], formatter), // order_approved_at
                                info[5].isEmpty() ? null : LocalDateTime.parse(info[5], formatter), // order_delivered_carrier_date
                                info[6].isEmpty() ? null : LocalDateTime.parse(info[6], formatter), // order_delivered_customer_date
                                info[7].isEmpty() ? null : LocalDateTime.parse(info[7], formatter)  // order_estimated_delivery_date
                        );
                        orders.add(order);
                    }
                } catch (Exception e) {
                    System.err.println("解析订单时跳过此行: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readOrderItems(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                    if (info.length >= 7) {
                        // 确保关键数值不为空
                        if (!info[1].isEmpty() && !info[5].isEmpty() && !info[6].isEmpty()) {
                            OrderItems item = new OrderItems(
                                    info[0], // order_id
                                    Integer.parseInt(info[1]), // order_item_id
                                    info[2], // product_id
                                    info[3], // seller_id
                                    info[4].isEmpty() ? null : LocalDateTime.parse(info[4], formatter), // shipping_limit_date
                                    Double.parseDouble(info[5]), // price
                                    Double.parseDouble(info[6])  // freight_value
                            );
                            orderItems.add(item);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("解析订单项时跳过此行: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readOrderReviews(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                if (info.length >= 7) {
                    try {
                        OrderReviews review = new OrderReviews(
                                info[0], // review_id
                                info[1], // order_id
                                Integer.parseInt(info[2]), // review_score
                                info[3], // review_comment_title
                                info[4], // review_comment_message
                                info[5].isEmpty() ? null : LocalDateTime.parse(info[5], formatter), // review_creation_date
                                info[6].isEmpty() ? null : LocalDateTime.parse(info[6], formatter)  // review_answer_timestamp
                        );
                        orderReviews.add(review);
                    } catch (Exception e) {
                        System.err.println("解析评论时跳过此行: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readProducts(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (info.length >= 9) {
                    try {
                        Products product = new Products(
                                info[0], // product_id
                                info[1], // product_category_name
                                info[2].isEmpty() ? 0 : Integer.parseInt(info[2]), // product_name_lenght
                                info[3].isEmpty() ? 0 : Integer.parseInt(info[3]), // product_description_lenght
                                info[4].isEmpty() ? 0 : Integer.parseInt(info[4]), // product_photos_qty
                                info[5].isEmpty() ? 0.0 : Double.parseDouble(info[5]), // product_weight_g
                                info[6].isEmpty() ? 0.0 : Double.parseDouble(info[6]), // product_length_cm
                                info[7].isEmpty() ? 0.0 : Double.parseDouble(info[7]), // product_height_cm
                                info[8].isEmpty() ? 0.0 : Double.parseDouble(info[8])  // product_width_cm
                        );
                        products.add(product);
                    } catch (Exception e) {
                        System.err.println("解析产品时跳过此行: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCategoryNameTranslations(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                    if (info.length >= 2) {
                        CategoryNameTranslation t = new CategoryNameTranslation(
                                info[0], // product_category_name
                                info[1]  // product_category_name_english
                        );
                        categoryNameTranslations.add(t);
                    }
                } catch (Exception e) {
                    System.err.println("解析翻译时跳过此行: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 1. Top 10 Best-Selling Product Categories (Unchanged)
     */
    public Map<String, Integer> topSellingCategories() {
        Map<String, String> translate = categoryNameTranslations.stream()
                .collect(Collectors.toMap(
                        CategoryNameTranslation::getProductCategoryName,
                        CategoryNameTranslation::getProductCategoryNameEnglish,
                        (e1, e2) -> e1
                ));

        Map<String, String> productToCategory = products.stream()
                .filter(p -> p.getProductCategoryName() != null && !p.getProductCategoryName().isEmpty() && translate.containsKey(p.getProductCategoryName()))
                .collect(Collectors.toMap(
                        Products::getProductId,
                        p -> translate.get(p.getProductCategoryName()),
                        (e1, e2) -> e1
                ));

        Map<String, Integer> categorySales = orderItems.stream()
                .filter(item -> productToCategory.containsKey(item.getProductId()))
                .collect(Collectors.groupingBy(
                        item -> productToCategory.get(item.getProductId()),
                        Collectors.summingInt(item -> 1)
                ));

        return categorySales.entrySet().stream()
                .sorted((e1, e2) -> {
                    int salesCompare = Integer.compare(e2.getValue(), e1.getValue());
                    if (salesCompare != 0) return salesCompare;
                    return e1.getKey().compareTo(e2.getKey());
                })
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 2. Customer Purchase Patterns by Hour (Unchanged)
     */
    public Map<String, Long> getPurchasePatternByHour() {
        Map<Integer, Long> hourCounts = orders.stream()
                .filter(o -> o.getOrderPurchaseTimestamp() != null)
                .collect(Collectors.groupingBy(
                        order -> order.getOrderPurchaseTimestamp().getHour(),
                        Collectors.counting()
                ));

        Map<String, Long> result = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            String hourStr = String.format("%02d:00", hour);
            result.put(hourStr, hourCounts.getOrDefault(hour, 0L));
        }

        return result;
    }

    /**
     * 3. Price Range Distribution by Category (Unchanged)
     */
    public Map<String, Map<String, Long>> getPriceRangeDistribution() {
        Map<String, String> translate = categoryNameTranslations.stream()
                .collect(Collectors.toMap(
                        CategoryNameTranslation::getProductCategoryName,
                        CategoryNameTranslation::getProductCategoryNameEnglish,
                        (e1, e2) -> e1
                ));

        Map<String, String> productToCategory = products.stream()
                .filter(p -> p.getProductCategoryName() != null && !p.getProductCategoryName().isEmpty() && translate.containsKey(p.getProductCategoryName()))
                .collect(Collectors.toMap(
                        Products::getProductId,
                        p -> translate.get(p.getProductCategoryName()),
                        (e1, e2) -> e1
                ));

        Map<String, Double> productAvgPrice = orderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItems::getProductId,
                        Collectors.averagingDouble(OrderItems::getPrice)
                ));

        Map<String, Map<String, Long>> result = productToCategory.entrySet().stream()
                .filter(entry -> productAvgPrice.containsKey(entry.getKey()))
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue(),
                        Collectors.groupingBy(
                                entry -> getPriceRange(productAvgPrice.get(entry.getKey())),
                                Collectors.counting()
                        )
                ));

        String[] ranges = {"(0,50]", "(50,100]", "(100,200]", "(200,500]", "(500,)"};
        Map<String, Map<String, Long>> sortedResult = new TreeMap<>();

        result.forEach((category, rangeMap) -> {
            Map<String, Long> sortedRanges = new LinkedHashMap<>();
            for (String range : ranges) {
                sortedRanges.put(range, rangeMap.getOrDefault(range, 0L));
            }
            sortedResult.put(category, sortedRanges);
        });

        return sortedResult;
    }

    // Q3 的辅助方法
    public String getPriceRange(double price) {
        if (price <= 50) return "(0,50]";
        if (price <= 100) return "(50,100]";
        if (price <= 200) return "(100,200]";
        if (price <= 500) return "(200,500]";
        return "(500,)";
    }

    /**
     * 4. Seller Performance Analysis (V11 - Logic A for avgScore to match Q4.txt)
     */
    public Map<String, List<Double>> analyzeSellerPerformance() {
        Map<String, Order> orderMap = orders.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Order::getOrderId, order -> order, (e1, e2) -> e1));

        Map<String, List<Integer>> orderToAllScores = orderReviews.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(OrderReviews::getOrderId,
                        Collectors.mapping(OrderReviews::getReviewScore, Collectors.toList())));
        Set<String> reviewedOrderIds = orderToAllScores.keySet();

        Map<String, Set<String>> sellerAllOrders = orderItems.stream()
                .filter(item -> item != null && item.getSellerId() != null && !item.getSellerId().isEmpty() && orderMap.containsKey(item.getOrderId()))
                .collect(Collectors.groupingBy(OrderItems::getSellerId,
                        Collectors.mapping(OrderItems::getOrderId, Collectors.toSet())));

        Set<String> eligibleSellers = sellerAllOrders.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().size() >= 50)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<String, List<OrderItems>> sellerAllItemsMap = orderItems.stream()
                .filter(item -> item != null && item.getSellerId() != null && eligibleSellers.contains(item.getSellerId()))
                .collect(Collectors.groupingBy(OrderItems::getSellerId));

        Map<String, List<Double>> result = new HashMap<>();

        for (Map.Entry<String, List<OrderItems>> entry : sellerAllItemsMap.entrySet()) {
            String sellerId = entry.getKey();
            List<OrderItems> allSellerItems = entry.getValue();
            Set<String> uniqueAllOrderIds = sellerAllOrders.getOrDefault(sellerId, Collections.emptySet());
            double totalOrderCount = (double) uniqueAllOrderIds.size();

            // --- 指标 1 & 2 & 3 (Unchanged) ---
            double totalSalesOnlyPrice = allSellerItems.stream()
                    .mapToDouble(OrderItems::getPrice)
                    .sum();
            double roundedTotalSalesOnlyPrice = Math.round(totalSalesOnlyPrice * 100.0) / 100.0;
            double avgOrderValue = (totalOrderCount > 0) ? (roundedTotalSalesOnlyPrice / totalOrderCount) : 0.0;
            double roundedAvgOrderValue = Math.round(avgOrderValue * 100.0) / 100.0;
            double uniqueProductsCount = (double) allSellerItems.stream()
                    .map(OrderItems::getProductId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            // --- 指标 4: Average review score (V11 - "Average of Entries" - Logic A) ---
            Set<String> uniqueReviewedOrderIdsForSeller = uniqueAllOrderIds.stream()
                    .filter(reviewedOrderIds::contains)
                    .collect(Collectors.toSet());

            // 【V11 修正】: 获取这些订单的 *所有* 评论分数
            List<Integer> allScoresForSeller = uniqueReviewedOrderIdsForSeller.stream()
                    .flatMap(orderId -> orderToAllScores.getOrDefault(orderId, Collections.emptyList()).stream())
                    .collect(Collectors.toList());

            // 【V11 修正】: 对 *所有评论条目* 取平均
            double avgScore = allScoresForSeller.stream()
                    .mapToInt(Integer::intValue).average().orElse(0.0);
            double roundedAvgScore = Math.round(avgScore * 100.0) / 100.0;

            // --- 指标 5: On-time delivery rate (Unchanged) ---
            double onTimeDeliveryRate = 0.0;
            long deliverableOrders = uniqueAllOrderIds.stream()
                    .map(orderMap::get)
                    .filter(order -> order != null &&
                            "delivered".equals(order.getOrderStatus()) &&
                            order.getOrderDeliveredCustomerDate() != null &&
                            order.getOrderEstimatedDeliveryDate() != null)
                    .count();
            long deliveredOnTime = uniqueAllOrderIds.stream()
                    .map(orderMap::get)
                    .filter(order -> order != null &&
                            "delivered".equals(order.getOrderStatus()) &&
                            order.getOrderDeliveredCustomerDate() != null &&
                            order.getOrderEstimatedDeliveryDate() != null &&
                            !order.getOrderDeliveredCustomerDate().isAfter(order.getOrderEstimatedDeliveryDate()))
                    .count();
            if (deliverableOrders > 0) {
                onTimeDeliveryRate = (double) deliveredOnTime / deliverableOrders;
            }
            double roundedOnTimeRate = Math.round(onTimeDeliveryRate * 100.0) / 100.0;

            // --- 存储结果 ---
            result.put(sellerId, Arrays.asList(
                    roundedTotalSalesOnlyPrice,
                    roundedAvgOrderValue,
                    uniqueProductsCount,
                    roundedAvgScore,
                    roundedOnTimeRate
            ));
        }

        // --- 最终排序 ---
        return result.entrySet().stream()
                .sorted((e1, e2) -> {
                    double salesValue1 = e1.getValue().get(0);
                    double salesValue2 = e2.getValue().get(0);
                    int salesCompare = Double.compare(salesValue2, salesValue1);
                    if (salesCompare != 0) {
                        return salesCompare;
                    } else {
                        return e1.getKey().compareTo(e2.getKey());
                    }
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 5. Top-10 Recommended Products by Category (V11 - Logic A for reviewCount and avgRating)
     */
    public Map<String, List<String>> recommendedProducts() {
        // --- 预计算 ---
        Map<String, String> translate = categoryNameTranslations.stream()
                .collect(Collectors.toMap(
                        CategoryNameTranslation::getProductCategoryName,
                        CategoryNameTranslation::getProductCategoryNameEnglish,
                        (e1, e2) -> e1
                ));
        Map<String, String> productToEnglishCategory = products.stream()
                .filter(p -> p.getProductCategoryName() != null && !p.getProductCategoryName().isEmpty() && translate.containsKey(p.getProductCategoryName()))
                .collect(Collectors.toMap(
                        Products::getProductId,
                        p -> translate.get(p.getProductCategoryName()),
                        (e1, e2) -> e1
                ));
        Map<String, List<Integer>> orderToAllScores = orderReviews.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        OrderReviews::getOrderId,
                        Collectors.mapping(OrderReviews::getReviewScore, Collectors.toList())
                ));
        Set<String> reviewedOrderIds = orderToAllScores.keySet();

        // --- 计算每个产品的基础指标 ---
        Map<String, List<OrderItems>> itemsByProduct = orderItems.stream()
                .filter(item -> item != null && item.getProductId() != null && productToEnglishCategory.containsKey(item.getProductId()))
                .collect(Collectors.groupingBy(OrderItems::getProductId));

        Map<String, ProductMetrics> productMetricsMap = new HashMap<>();
        for (Map.Entry<String, List<OrderItems>> entry : itemsByProduct.entrySet()) {
            String productId = entry.getKey();
            List<OrderItems> items = entry.getValue();

            // 指标: 销量
            int salesCount = items.size();

            Set<String> distinctOrderIds = items.stream()
                    .map(OrderItems::getOrderId)
                    .collect(Collectors.toSet());
            Set<String> distinctReviewedOrdersForProduct = distinctOrderIds.stream()
                    .filter(reviewedOrderIds::contains)
                    .collect(Collectors.toSet());

            // 获取这些订单的 *所有* 评论条目
            List<Integer> allScoresForProduct = distinctReviewedOrdersForProduct.stream()
                    .flatMap(orderId -> orderToAllScores.getOrDefault(orderId, Collections.emptyList()).stream())
                    .collect(Collectors.toList());

            // 【V11 修正 1】: 指标: 评论数 (基于 *评论条目总数*)
            long reviewCount = allScoresForProduct.size();

            // 【V11 修正 2】: 指标: 平均评分 (基于 *评论条目平均分*)
            double avgRating = allScoresForProduct.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

            // 【V11 修正 3】: 过滤器 (基于 *评论条目总数*)
            if (salesCount >= 10 && reviewCount >= 5) {
                // 存储 Logic A 的 reviewCount 和 avgRating
                productMetricsMap.put(productId, new ProductMetrics(salesCount, reviewCount, avgRating));
            }
        }

        // --- 按类别对过滤后的产品进行分组 ---
        Map<String, List<ProductWithMetrics>> categoryProductMetrics = new HashMap<>();
        for (Map.Entry<String, ProductMetrics> entry : productMetricsMap.entrySet()) {
            String productId = entry.getKey();
            ProductMetrics metrics = entry.getValue();
            String category = productToEnglishCategory.get(productId);
            categoryProductMetrics
                    .computeIfAbsent(category, k -> new ArrayList<>())
                    .add(new ProductWithMetrics(productId, metrics));
        }

        // --- 在每个类别内部计算分数并推荐 ---
        // 【V11 修正 4】: 初始化Map以包含所有类别 (包括空类别)
        Map<String, List<String>> finalRecommendations = new TreeMap<>();
        translate.values().stream().distinct().forEach(englishCategoryName -> {
            finalRecommendations.put(englishCategoryName, new ArrayList<>());
        });

        for (Map.Entry<String, List<ProductWithMetrics>> categoryEntry : categoryProductMetrics.entrySet()) {
            String category = categoryEntry.getKey();
            List<ProductWithMetrics> productsInCategory = categoryEntry.getValue();

            // V11: 这里需要检查，因为可能没有产品符合条件了
            if (productsInCategory.isEmpty()) {
                continue; // 跳过没有合格产品的类别
            }

            // 1. 找到该类别的 Min/Max 用于归一化
            double minSales = productsInCategory.stream().mapToDouble(p -> p.metrics.salesCount).min().orElse(0.0);
            double maxSales = productsInCategory.stream().mapToDouble(p -> p.metrics.salesCount).max().orElse(minSales);
            double minReviews = productsInCategory.stream().mapToDouble(p -> p.metrics.reviewCount).min().orElse(0.0); // V11: 基于条目数
            double maxReviews = productsInCategory.stream().mapToDouble(p -> p.metrics.reviewCount).max().orElse(minReviews); // V11: 基于条目数
            double minRating = productsInCategory.stream().mapToDouble(p -> p.metrics.avgRating).min().orElse(0.0); // V11: 基于条目平均分
            double maxRating = productsInCategory.stream().mapToDouble(p -> p.metrics.avgRating).max().orElse(minRating); // V11: 基于条目平均分

            final double salesRange = (maxSales - minSales);
            final double reviewRange = (maxReviews - minReviews);
            final double ratingRange = (maxRating - minRating);

            // 2. 计算每个产品的最终分数
            List<ProductWithScore> scoredProducts = productsInCategory.stream()
                    .map(p -> {
                        double salesScore = (salesRange == 0) ? 1.0 : (p.metrics.salesCount - minSales) / salesRange;
                        double reviewScore = (reviewRange == 0) ? 1.0 : (p.metrics.reviewCount - minReviews) / reviewRange; // V11
                        double ratingScore = (ratingRange == 0) ? 1.0 : (p.metrics.avgRating - minRating) / ratingRange; // V11

                        double finalScore = 0.5 * salesScore + 0.3 * reviewScore + 0.2 * ratingScore;
                        return new ProductWithScore(p.productId, finalScore);
                    })
                    .collect(Collectors.toList());

            // 3. 排序和截取
            List<String> top10Products = scoredProducts.stream()
                    .sorted((p1, p2) -> {
                        int scoreCompare = Double.compare(p2.score, p1.score);
                        if (scoreCompare != 0) return scoreCompare;
                        return p1.productId.compareTo(p2.productId);
                    })
                    .limit(10)
                    .map(p -> p.productId)
                    .collect(Collectors.toList());

            finalRecommendations.put(category, top10Products);
        }

        return finalRecommendations;
    }

    // --- (Q5 的辅助内部类) ---
    private static class ProductMetrics {
        int salesCount;
        long reviewCount;  // 【V11 修正】: 评论条目总数
        double avgRating;  // 【V11 修正】: 评论条目平均分

        ProductMetrics(int salesCount, long reviewCount, double avgRating) {
            this.salesCount = salesCount;
            this.reviewCount = reviewCount;
            this.avgRating = avgRating;
        }
    }
    private static class ProductWithMetrics {
        String productId;
        ProductMetrics metrics;
        ProductWithMetrics(String productId, ProductMetrics metrics) {
            this.productId = productId;
            this.metrics = metrics;
        }
    }
    private static class ProductWithScore {
        String productId;
        double score;
        ProductWithScore(String productId, double score) {
            this.productId = productId;
            this.score = score;
        }
    }
}


// --- (数据模型类 - Unchanged) ---
class Order {
    String orderId;
    String customerId;
    String orderStatus;
    LocalDateTime orderPurchaseTimestamp;
    LocalDateTime orderApprovedAt;
    LocalDateTime orderDeliveredCarrierDate;
    LocalDateTime orderDeliveredCustomerDate;
    LocalDateTime orderEstimatedDeliveryDate;
    public Order(String orderId, String customerId, String orderStatus,
                 LocalDateTime orderPurchaseTimestamp, LocalDateTime orderApprovedAt,
                 LocalDateTime orderDeliveredCarrierDate, LocalDateTime orderDeliveredCustomerDate,
                 LocalDateTime orderEstimatedDeliveryDate) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderStatus = orderStatus;
        this.orderPurchaseTimestamp = orderPurchaseTimestamp;
        this.orderApprovedAt = orderApprovedAt;
        this.orderDeliveredCarrierDate = orderDeliveredCarrierDate;
        this.orderDeliveredCustomerDate = orderDeliveredCustomerDate;
        this.orderEstimatedDeliveryDate = orderEstimatedDeliveryDate;
    }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getOrderStatus() { return orderStatus; }
    public LocalDateTime getOrderPurchaseTimestamp() { return orderPurchaseTimestamp; }
    public LocalDateTime getOrderApprovedAt() { return orderApprovedAt; }
    public LocalDateTime getOrderDeliveredCarrierDate() { return orderDeliveredCarrierDate; }
    public LocalDateTime getOrderDeliveredCustomerDate() { return orderDeliveredCustomerDate; }
    public LocalDateTime getOrderEstimatedDeliveryDate() { return orderEstimatedDeliveryDate; }
}

class OrderItems {
    String orderId;
    int orderItemId;
    String productId;
    String sellerId;
    LocalDateTime shippingLimitDate;
    double price;
    double freightValue;
    public OrderItems(String orderId, int orderItemId,
                      String productId, String sellerId,
                      LocalDateTime shippingLimitDate,
                      double price, double freightValue) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.sellerId = sellerId;
        this.shippingLimitDate = shippingLimitDate;
        this.price = price;
        this.freightValue = freightValue;
    }
    public String getOrderId() { return orderId; }
    public int getOrderItemId() { return orderItemId; }
    public String getProductId() { return productId; }
    public String getSellerId() { return sellerId; }
    public LocalDateTime getShippingLimitDate() { return shippingLimitDate; }
    public double getPrice() { return price; }
    public double getFreightValue() { return freightValue; }
}

class OrderReviews {
    String reviewId;
    String orderId;
    int reviewScore;
    String reviewCommentTitle;
    String reviewCommentMessage;
    LocalDateTime reviewCreationDate;
    LocalDateTime reviewAnswerTimestamp;
    public OrderReviews(String reviewId, String orderId,
                        int reviewScore, String reviewCommentTitle,
                        String reviewCommentMessage, LocalDateTime reviewCreationDate,
                        LocalDateTime reviewAnswerTimestamp) {
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.reviewScore = reviewScore;
        this.reviewCommentTitle = reviewCommentTitle;
        this.reviewCommentMessage = reviewCommentMessage;
        this.reviewCreationDate = reviewCreationDate;
        this.reviewAnswerTimestamp = reviewAnswerTimestamp;
    }
    public String getReviewId() { return reviewId; }
    public String getOrderId() { return orderId; }
    public int getReviewScore() { return reviewScore; }
    public String getReviewCommentTitle() { return reviewCommentTitle; }
    public String getReviewCommentMessage() { return reviewCommentMessage; }
    public LocalDateTime getReviewCreationDate() { return reviewCreationDate; }
    public LocalDateTime getReviewAnswerTimestamp() { return reviewAnswerTimestamp; }
}

class Products {
    String productId;
    String productCategoryName;
    int productNameLenght;
    int productDescriptionLenght;
    int productPhotosQty;
    double productWeightG;
    double productLengthCm;
    double productHeightCm;
    double productWidthCm;
    public Products(String productId,
                    String productCategoryName,
                    int productNameLenght,
                    int productDescriptionLenght,
                    int productPhotosQty,
                    double productWeightG,
                    double productLengthCm,
                    double productHeightCm,
                    double productWidthCm) {
        this.productId = productId;
        this.productCategoryName = productCategoryName;
        this.productNameLenght = productNameLenght;
        this.productDescriptionLenght = productDescriptionLenght;
        this.productPhotosQty = productPhotosQty;
        this.productWeightG = productWeightG;
        this.productLengthCm = productLengthCm;
        this.productHeightCm = productHeightCm;
        this.productWidthCm = productWidthCm;
    }
    public String getProductId() { return productId; }
    public String getProductCategoryName() { return productCategoryName; }
    public int getProductNameLenght() { return productNameLenght; }
    public int getProductDescriptionLenght() { return productDescriptionLenght; }
    public int getProductPhotosQty() { return productPhotosQty; }
    public double getProductWeightG() { return productWeightG; }
    public double getProductLengthCm() { return productLengthCm; }
    public double getProductHeightCm() { return productHeightCm; }
    public double getProductWidthCm() { return productWidthCm; }
}

class CategoryNameTranslation {
    String productCategoryName;
    String productCategoryNameEnglish;
    public CategoryNameTranslation(String productCategoryName,
                                   String productCategoryNameEnglish) {
        this.productCategoryName = productCategoryName;
        this.productCategoryNameEnglish = productCategoryNameEnglish;
    }
    public String getProductCategoryName() { return productCategoryName; }
    public String getProductCategoryNameEnglish() { return productCategoryNameEnglish; }
}