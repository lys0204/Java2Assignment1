import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class OlistAnalyzer {
    private List<Order> orders = new ArrayList<>();
    private List<OrderItems> orderItems = new ArrayList<>();
    private List<OrderReviews> orderReviews = new ArrayList<>();
    private List<Products> products = new ArrayList<>();
    private List<CategoryNameTranslation> categoryNameTranslations = new ArrayList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OlistAnalyzer(String datasetFolderPath) {
        readOrders(datasetFolderPath);
        readOrderItems(datasetFolderPath);
        readOrderReviews(datasetFolderPath);
        readProducts(datasetFolderPath);
        readCategoryNameTranslations(datasetFolderPath);
    }

    private void readOrders(String folder) {
        try (BufferedReader br = new BufferedReader(new FileReader(folder + "/olist_orders_dataset.csv", StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                    // 检查数组长度是否足够
                    if (info.length >= 8) {
                        Order order = new Order(
                                info[0],
                                info[1],
                                info[2],
                                LocalDateTime.parse(info[3], formatter),
                                info[4].isEmpty() ? null : LocalDateTime.parse(info[4], formatter),
                                info[5].isEmpty() ? null : LocalDateTime.parse(info[5], formatter),
                                info[6].isEmpty() ? null : LocalDateTime.parse(info[6], formatter),
                                LocalDateTime.parse(info[7], formatter)
                        );
                        orders.add(order);
                    }
                } catch (Exception e) {
                    // 跳过格式错误的行
                    System.out.println("解析错误，跳过此行: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readOrderItems(String folder) {
        try (BufferedReader br = new BufferedReader(new FileReader(folder + "/olist_order_items_dataset.csv", StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                    // 检查数组长度和数值格式
                    if (info.length >= 7) {
                        // 验证数值字段
                        if (!info[1].isEmpty() && !info[5].isEmpty() && !info[6].isEmpty()) {
                            OrderItems item = new OrderItems(
                                    info[0],
                                    Integer.parseInt(info[1]),
                                    info[2],
                                    info[3],
                                    LocalDateTime.parse(info[4], formatter),
                                    Double.parseDouble(info[5]),
                                    Double.parseDouble(info[6])
                            );
                            orderItems.add(item);
                        }
                    }
                } catch (Exception e) {
                    // 跳过格式错误的行
                    System.out.println("解析错误，跳过此行: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readOrderReviews(String folder) {
        try (BufferedReader br = new BufferedReader(new FileReader(folder + "/olist_order_reviews_dataset.csv", StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                // 检查数组长度，避免越界
                if (info.length >= 7) {
                    try {
                        OrderReviews review = new OrderReviews(
                                info[0],
                                info[1],
                                Integer.parseInt(info[2]),
                                info[3],
                                info[4],
                                LocalDateTime.parse(info[5], formatter),
                                LocalDateTime.parse(info[6], formatter)
                        );
                        orderReviews.add(review);
                    } catch (NumberFormatException e) {
                        // 处理可能的解析异常
                        System.err.println("解析错误，跳过此行: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readProducts(String folder) {
        try (BufferedReader br = new BufferedReader(new FileReader(folder + "/olist_products_dataset.csv", StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                // 检查数组长度，避免越界
                if (info.length >= 9) {
                    try {
                        // 对可能为空的数值字段进行处理
                        int productCategoryNameLength = info[2].isEmpty() ? 0 : Integer.parseInt(info[2]);
                        int productNameLength = info[3].isEmpty() ? 0 : Integer.parseInt(info[3]);
                        int productDescriptionLength = info[4].isEmpty() ? 0 : Integer.parseInt(info[4]);
                        double productWeight = info[5].isEmpty() ? 0.0 : Double.parseDouble(info[5]);
                        double productLength = info[6].isEmpty() ? 0.0 : Double.parseDouble(info[6]);
                        double productHeight = info[7].isEmpty() ? 0.0 : Double.parseDouble(info[7]);
                        double productWidth = info[8].isEmpty() ? 0.0 : Double.parseDouble(info[8]);
                        
                        Products product = new Products(
                                info[0],
                                info[1],
                                productCategoryNameLength,
                                productNameLength,
                                productDescriptionLength,
                                productWeight,
                                productLength,
                                productHeight,
                                productWidth
                        );
                        products.add(product);
                    } catch (NumberFormatException e) {
                        // 处理可能的解析异常
                        System.err.println("解析错误，跳过此行: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCategoryNameTranslations(String folder) {
        try (BufferedReader br = new BufferedReader(new FileReader(folder + "/product_category_name_translation.csv", StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                    // 检查数组长度
                    if (info.length >= 2) {
                        CategoryNameTranslation t = new CategoryNameTranslation(info[0], info[1]);
                        categoryNameTranslations.add(t);
                    }
                } catch (Exception e) {
                    // 跳过格式错误的行
                    System.out.println("解析错误，跳过此行: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> topSellingCategories() {
        Map<String,String> translate = categoryNameTranslations.stream().collect(Collectors.toMap(
                CategoryNameTranslation::getProductCategoryName,
                CategoryNameTranslation::getProductCategoryNameEnglish
        ));
        Map<String,String> idAndName = new HashMap<>();
        for(Products product : products) {
            if(product.getProductCategoryName() != null && !product.getProductCategoryName().isEmpty()){
                String englishName = translate.get(product.getProductCategoryName());
                idAndName.put(product.getProductId(), englishName);
            }
        }

        Map<String, Integer> categorySales = orderItems.stream()
                .filter(item -> idAndName.containsKey(item.getProductId()))
                .collect(Collectors.groupingBy(
                        item -> idAndName.get(item.getProductId()),
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

    public Map<String, Long> getPurchasePatternByHour() {
        Map<Integer, Long> hourCounts = orders.stream()
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

    public Map<String, Map<String, Long>> getPriceRangeDistribution() {
        Map<String,String> translate = categoryNameTranslations.stream().collect(Collectors.toMap(
                CategoryNameTranslation::getProductCategoryName,
                CategoryNameTranslation::getProductCategoryNameEnglish
        ));
        Map<String,String> idAndName = new HashMap<>();
        for(Products product : products) {
            if(product.getProductCategoryName() != null && !product.getProductCategoryName().isEmpty()){
                String englishName = translate.get(product.getProductCategoryName());
                idAndName.put(product.getProductId(), englishName);
            }
        }

        Map<String, Map<String, Long>> result = orderItems.stream()
                .filter(item -> idAndName.containsKey(item.getProductId()))
                .collect(Collectors.groupingBy(
                        item -> idAndName.get(item.getProductId()),
                        Collectors.groupingBy(
                                item -> getPriceRange(item.getPrice()),
                                Collectors.counting()
                        )
                ));

        for (String category : result.keySet()) {
            Map<String, Long> categoryRanges = result.get(category);
            String[] ranges = {"(0,50]", "(50,100]", "(100,200]", "(200,500]", "(500,)"};
            for (String range : ranges) {
                categoryRanges.putIfAbsent(range, 0L);
            }
        }

        return result.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    
    public String getPriceRange(double price) {
        if (price <= 50) return "(0,50]";
        if (price <= 100) return "(50,100]";
        if (price <= 200) return "(100,200]";
        if (price <= 500) return "(200,500]";
        return "(500,)";
    }

    public Map<String, List<Double>> analyzeSellerPerformance() {
        // 创建订单ID到订单的映射
        Map<String, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(
                        Order::getOrderId, 
                        order -> order,
                        (existing, replacement) -> existing // 处理重复的订单ID
                ));
        
        // 创建订单ID到评分的映射
        Map<String, Integer> orderToScore = orderReviews.stream()
                .collect(Collectors.toMap(
                        OrderReviews::getOrderId,
                        OrderReviews::getReviewScore,
                        (existing, replacement) -> existing // 处理重复的订单ID
                ));
        
        // 先统计每个卖家的订单数，过滤掉订单数少于50的卖家（计算唯一订单）
        Map<String, Long> sellerOrderCounts = orderItems.stream()
                .filter(item -> orderMap.containsKey(item.getOrderId()))
                .collect(Collectors.groupingBy(
                        OrderItems::getSellerId,
                        Collectors.mapping(OrderItems::getOrderId, Collectors.toSet())
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size()
                ));
        
        // 只处理订单数>=50的卖家
        Map<String, List<Double>> result = orderItems.stream()
                .filter(item -> item != null && item.getSellerId() != null)
                .filter(item -> orderMap.containsKey(item.getOrderId()))
                .filter(item -> sellerOrderCounts.getOrDefault(item.getSellerId(), 0L) >= 50)
                .collect(Collectors.groupingBy(
                        OrderItems::getSellerId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    // 获取唯一订单ID集合用于后续计算
                                    Set<String> uniqueOrderIds = items.stream()
                                            .map(OrderItems::getOrderId)
                                            .distinct()
                                            .collect(Collectors.toSet());
                                    
                                    // 1. 总销售额
                                    double totalSales = items.stream()
                                            .mapToDouble(item -> item.getPrice() + item.getFreightValue())
                                            .sum();
                                    
                                    // 2. 平均订单价值
                                    double avgOrderValue = 0.0;
                                    if (!uniqueOrderIds.isEmpty()) {
                                        Map<String, Double> orderValues = new HashMap<>();
                                        for (OrderItems item : items) {
                                            String orderId = item.getOrderId();
                                            double itemValue = item.getPrice() + item.getFreightValue();
                                            orderValues.put(orderId, orderValues.getOrDefault(orderId, 0.0) + itemValue);
                                        }
                                        avgOrderValue = orderValues.values().stream()
                                                .mapToDouble(Double::doubleValue)
                                                .average()
                                                .orElse(0.0);
                                    }
                                    
                                    // 3. 销售的唯一产品数量
                                    double uniqueProducts = items.stream()
                                            .map(OrderItems::getProductId)
                                            .distinct()
                                            .filter(id -> id != null && !id.isEmpty())
                                            .count();
                                    
                                    // 4. 平均评分（只计算有评分的订单）
                                    double avgScore = 0.0;
                                    List<Integer> scores = new ArrayList<>();
                                    for (String orderId : uniqueOrderIds) {
                                        if (orderToScore.containsKey(orderId)) {
                                            scores.add(orderToScore.get(orderId));
                                        }
                                    }
                                    if (!scores.isEmpty()) {
                                        avgScore = scores.stream()
                                                .mapToInt(Integer::intValue)
                                                .average()
                                                .orElse(0.0);
                                    }
                                    
                                    // 5. 准时配送率
                                    double onTimeDeliveryRate = 0.0;
                                    int deliveredOnTime = 0;
                                    int deliverableOrders = 0;
                                    
                                    for (String orderId : uniqueOrderIds) {
                                        Order order = orderMap.get(orderId);
                                        if (order != null && 
                                            order.getOrderDeliveredCustomerDate() != null && 
                                            order.getOrderEstimatedDeliveryDate() != null) {
                                            deliverableOrders++;
                                            if (order.getOrderDeliveredCustomerDate().isBefore(order.getOrderEstimatedDeliveryDate()) ||
                                                order.getOrderDeliveredCustomerDate().isEqual(order.getOrderEstimatedDeliveryDate())) {
                                                deliveredOnTime++;
                                            }
                                        }
                                    }
                                    
                                    if (deliverableOrders > 0) {
                                        onTimeDeliveryRate = (double) deliveredOnTime / deliverableOrders;
                                    }
                                    
                                    // 四舍五入保留两位小数
                                    return Arrays.asList(
                                            Math.round(totalSales * 100.0) / 100.0,
                                            Math.round(avgOrderValue * 100.0) / 100.0,
                                            uniqueProducts,
                                            Math.round(avgScore * 100.0) / 100.0,
                                            Math.round(onTimeDeliveryRate * 100.0) / 100.0
                                    );
                                }
                        )
                ));
        
        // 按总销售额降序排序，相同时按卖家ID升序排序
        return result.entrySet().stream()
                .sorted((e1, e2) -> {
                    double sales1 = e1.getValue().get(0);
                    double sales2 = e2.getValue().get(0);
                    int salesCompare = Double.compare(sales2, sales1);
                    if (salesCompare != 0) return salesCompare;
                    return e1.getKey().compareTo(e2.getKey());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public Map<String, List<String>> recommendedProducts() {
        // 建立产品ID到英文类别的映射
        Map<String, String> translate = categoryNameTranslations.stream().collect(Collectors.toMap(
                CategoryNameTranslation::getProductCategoryName,
                CategoryNameTranslation::getProductCategoryNameEnglish
        ));
        Map<String, String> productToEnglishCategory = new HashMap<>();
        for(Products product : products) {
            if(product.getProductCategoryName() != null && !product.getProductCategoryName().isEmpty()){
                String englishName = translate.get(product.getProductCategoryName());
                if(englishName != null) {
                    productToEnglishCategory.put(product.getProductId(), englishName);
                }
            }
        }
        
        // 建立订单ID到评分的映射
        Map<String, Integer> orderToScore = orderReviews.stream()
                .collect(Collectors.toMap(
                        OrderReviews::getOrderId,
                        OrderReviews::getReviewScore,
                        (existing, replacement) -> existing // 处理重复的订单ID
                ));
        
        // 计算每个产品的销量、评价数量和平均评分
        Map<String, ProductMetrics> productMetrics = orderItems.stream()
                .filter(item -> productToEnglishCategory.containsKey(item.getProductId()))
                .collect(Collectors.groupingBy(
                        OrderItems::getProductId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    // 销量（订单项数量）
                                    int salesCount = items.size();
                                    
                                    // 评价数量（有评价的订单数量）
                                    long reviewCount = items.stream()
                                            .map(OrderItems::getOrderId)
                                            .distinct()
                                            .filter(orderToScore::containsKey)
                                            .count();
                                    
                                    // 平均评分
                                    double avgRating = items.stream()
                                            .map(OrderItems::getOrderId)
                                            .distinct()
                                            .filter(orderToScore::containsKey)
                                            .mapToInt(orderToScore::get)
                                            .average()
                                            .orElse(0.0);
                                    
                                    return new ProductMetrics(salesCount, reviewCount, avgRating);
                                }
                        )
                ));
        
        // 按类别分组产品
        Map<String, List<String>> categoryProducts = productToEnglishCategory.entrySet().stream()
                .filter(entry -> {
                    String productId = entry.getKey();
                    ProductMetrics metrics = productMetrics.get(productId);
                    return metrics != null && 
                           metrics.salesCount >= 10 && 
                           metrics.reviewCount >= 5;
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
        
        // 为每个类别计算推荐产品
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : categoryProducts.entrySet()) {
            String category = entry.getKey();
            List<String> productIds = entry.getValue();
            
            if (productIds.isEmpty()) continue;
            
            // 获取该类别的所有产品指标
            List<ProductWithMetrics> productsWithMetrics = productIds.stream()
                    .map(productId -> {
                        ProductMetrics metrics = productMetrics.get(productId);
                        return new ProductWithMetrics(productId, metrics);
                    })
                    .collect(Collectors.toList());
            
            // 计算该类别的最大值和最小值用于归一化
            double maxSales = productsWithMetrics.stream()
                    .mapToDouble(p -> p.metrics.salesCount)
                    .max().orElse(1.0);
            double minSales = productsWithMetrics.stream()
                    .mapToDouble(p -> p.metrics.salesCount)
                    .min().orElse(0.0);
            
            double maxReviews = productsWithMetrics.stream()
                    .mapToDouble(p -> p.metrics.reviewCount)
                    .max().orElse(1.0);
            double minReviews = productsWithMetrics.stream()
                    .mapToDouble(p -> p.metrics.reviewCount)
                    .min().orElse(0.0);
            
            double maxRating = productsWithMetrics.stream()
                    .mapToDouble(p -> p.metrics.avgRating)
                    .max().orElse(1.0);
            double minRating = productsWithMetrics.stream()
                    .mapToDouble(p -> p.metrics.avgRating)
                    .min().orElse(0.0);
            
            // 计算加权评分并排序
            List<String> recommendedProducts = productsWithMetrics.stream()
                    .map(p -> {
                        // 归一化评分
                        double salesScore = (maxSales == minSales) ? 1.0 : 
                                (p.metrics.salesCount - minSales) / (maxSales - minSales);
                        double reviewScore = (maxReviews == minReviews) ? 1.0 : 
                                (p.metrics.reviewCount - minReviews) / (maxReviews - minReviews);
                        double ratingScore = (maxRating == minRating) ? 1.0 : 
                                (p.metrics.avgRating - minRating) / (maxRating - minRating);
                        
                        // 加权评分
                        double finalScore = 0.5 * salesScore + 0.3 * reviewScore + 0.2 * ratingScore;
                        
                        return new ProductWithScore(p.productId, finalScore);
                    })
                    .sorted((p1, p2) -> {
                        int scoreCompare = Double.compare(p2.score, p1.score);
                        if (scoreCompare != 0) return scoreCompare;
                        return p1.productId.compareTo(p2.productId);
                    })
                    .limit(10)
                    .map(p -> p.productId)
                    .collect(Collectors.toList());
            
            result.put(category, recommendedProducts);
        }
        
        return result;
    }
    
    // 辅助类：产品指标
    private static class ProductMetrics {
        int salesCount;
        long reviewCount;
        double avgRating;
        
        ProductMetrics(int salesCount, long reviewCount, double avgRating) {
            this.salesCount = salesCount;
            this.reviewCount = reviewCount;
            this.avgRating = avgRating;
        }
    }
    
    // 辅助类：带指标的产品
    private static class ProductWithMetrics {
        String productId;
        ProductMetrics metrics;
        
        ProductWithMetrics(String productId, ProductMetrics metrics) {
            this.productId = productId;
            this.metrics = metrics;
        }
    }
    
    // 辅助类：带评分的产品
    private static class ProductWithScore {
        String productId;
        double score;
        
        ProductWithScore(String productId, double score) {
            this.productId = productId;
            this.score = score;
        }
    }
}

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

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public LocalDateTime getOrderPurchaseTimestamp() {
        return orderPurchaseTimestamp;
    }

    public LocalDateTime getOrderApprovedAt() {
        return orderApprovedAt;
    }

    public LocalDateTime getOrderDeliveredCarrierDate() {
        return orderDeliveredCarrierDate;
    }

    public LocalDateTime getOrderDeliveredCustomerDate() {
        return orderDeliveredCustomerDate;
    }

    public LocalDateTime getOrderEstimatedDeliveryDate() {
        return orderEstimatedDeliveryDate;
    }
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

    public String getOrderId() {
        return orderId;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public String getProductId() {
        return productId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public LocalDateTime getShippingLimitDate() {
        return shippingLimitDate;
    }

    public double getPrice() {
        return price;
    }

    public double getFreightValue() {
        return freightValue;
    }
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

    public String getReviewId() {
        return reviewId;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getReviewScore() {
        return reviewScore;
    }

    public String getReviewCommentTitle() {
        return reviewCommentTitle;
    }

    public String getReviewCommentMessage() {
        return reviewCommentMessage;
    }

    public LocalDateTime getReviewCreationDate() {
        return reviewCreationDate;
    }

    public LocalDateTime getReviewAnswerTimestamp() {
        return reviewAnswerTimestamp;
    }
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

    public String getProductId() {
        return productId;
    }

    public String getProductCategoryName() {
        return productCategoryName;
    }

    public int getProductNameLenght() {
        return productNameLenght;
    }

    public int getProductDescriptionLenght() {
        return productDescriptionLenght;
    }

    public int getProductPhotosQty() {
        return productPhotosQty;
    }

    public double getProductWeightG() {
        return productWeightG;
    }

    public double getProductLengthCm() {
        return productLengthCm;
    }

    public double getProductHeightCm() {
        return productHeightCm;
    }

    public double getProductWidthCm() {
        return productWidthCm;
    }
}

class CategoryNameTranslation {
    String productCategoryName; 
    String productCategoryNameEnglish;

    public CategoryNameTranslation(String productCategoryName,
                                   String productCategoryNameEnglish) {
        this.productCategoryName = productCategoryName;
        this.productCategoryNameEnglish = productCategoryNameEnglish;
    }

    public String getProductCategoryName() {
        return productCategoryName;
    }

    public String getProductCategoryNameEnglish() {
        return productCategoryNameEnglish;
    }
}