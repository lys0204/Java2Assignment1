import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OlistAnalyzer 类 (V11 - 用户最终版)
 * 用于读取 Olist 数据集并执行5项核心分析任务。
 * 1. Q4 (analyzeSellerPerformance) 使用您提供的、匹配 Q4.txt 的逻辑。
 * 2. Q5 (recommendedProducts) 使用匹配 Q5.txt 的逻辑。
 * 3. readOrderReviews 包含跨行解析逻辑。
 */
public class OlistAnalyzer {
    // 存储从CSV读取的数据列表
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

    // --- (CSV 读取方法) ---

    private void readOrders(String filePath) {
        // 使用 try-with-resources 自动关闭 BufferedReader
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            br.readLine(); // 跳过表头
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    // 正则表达式：按逗号分割，但忽略引号内的逗号
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
                    // 如果某一行解析失败，打印错误并跳过，继续解析下一行
                    System.err.println("Skipping this line due to error while parsing orders: " + line);
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
                    System.err.println("Skipping this line due to error while parsing order items: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取评论文件，此方法包含处理跨行 CSV 记录的逻辑。
     * (例如，一个评论消息 "review_comment_message" 可能包含换行符)
     */
    private void readOrderReviews(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            br.readLine(); // 跳过表头
            String line;
            // csvRowBuffer 用于累积可能跨行的 CSV 记录
            StringBuilder csvRowBuffer = new StringBuilder();

            while ((line = br.readLine()) != null) {
                csvRowBuffer.append(line); // 将当前读取的行追加到缓冲区

                // 检查缓冲区中总共的引号数量
                int quoteCount = 0;
                for (int i = 0; i < csvRowBuffer.length(); i++) {
                    if (csvRowBuffer.charAt(i) == '"') {
                        quoteCount++;
                    }
                }

                // 核心逻辑：如果引号数量是偶数，说明所有被打开的引号都已关闭
                // 这标志着一个完整的 CSV 记录（行）已经读取完毕
                if (quoteCount % 2 == 0) {
                    String completeRow = csvRowBuffer.toString();
                    csvRowBuffer.setLength(0); // 清空缓冲区，准备处理下一条记录

                    String[] info = completeRow.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                    if (info.length >= 7) {
                        try {
                            // 清理因跨行和 split 导致的、字段两端多余的引号
                            String reviewTitle = info[3];
                            if (reviewTitle.startsWith("\"") && reviewTitle.endsWith("\"") && reviewTitle.length() > 1) {
                                reviewTitle = reviewTitle.substring(1, reviewTitle.length() - 1);
                            }

                            String reviewMessage = info[4];
                            if (reviewMessage.startsWith("\"") && reviewMessage.endsWith("\"") && reviewMessage.length() > 1) {
                                reviewMessage = reviewMessage.substring(1, reviewMessage.length() - 1);
                            }

                            OrderReviews review = new OrderReviews(
                                    info[0], // review_id
                                    info[1], // order_id
                                    Integer.parseInt(info[2]), // review_score
                                    reviewTitle,   // info[3] (清理后)
                                    reviewMessage, // info[4] (清理后)
                                    info[5].isEmpty() ? null : LocalDateTime.parse(info[5], formatter), // review_creation_date
                                    info[6].isEmpty() ? null : LocalDateTime.parse(info[6], formatter)  // review_answer_timestamp
                            );
                            orderReviews.add(review);
                        } catch (Exception e) {
                            System.err.println("Skipping this line due to error while parsing reviews: " + completeRow);
                        }
                    }
                } else {
                    // 引号是奇数，意味着我们正处于一个跨行字段的中间
                    // 我们需要把 br.readLine() 消耗掉的那个换行符加回来
                    csvRowBuffer.append("\n");
                    // 然后继续循环，读取下一行并追加到缓冲区
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
            // 产品的 split 逻辑与 review 不同，因为它不需要处理换行，但处理方式类似
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
                        System.err.println("Skipping this line due to error while parsing products: " + line);
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
                    System.err.println("Skipping this line due to error while parsing translations: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 1. Top 10 Best-Selling Product Categories
     * 找出销量最高的10个产品类别。
     */
    public Map<String, Integer> topSellingCategories() {
        // 步骤 1: 创建一个 葡萄牙语类别名 -> 英语类别名 的翻译映射
        Map<String, String> translate = categoryNameTranslations.stream()
                .collect(Collectors.toMap(
                        CategoryNameTranslation::getProductCategoryName,
                        CategoryNameTranslation::getProductCategoryNameEnglish,
                        (e1, e2) -> e1 // 如果有重复的葡语名，保留第一个
                ));

        // 步骤 2: 创建一个 Product ID -> 英语类别名 的映射
        Map<String, String> productToCategory = products.stream()
                // 过滤掉没有类别名或类别名无法翻译的产品
                .filter(p -> p.getProductCategoryName() != null && !p.getProductCategoryName().isEmpty() && translate.containsKey(p.getProductCategoryName()))
                .collect(Collectors.toMap(
                        Products::getProductId,
                        p -> translate.get(p.getProductCategoryName()),
                        (e1, e2) -> e1 // 如果有重复的 Product ID，保留第一个
                ));

        // 步骤 3: 计算销量
        // 遍历所有订单项(orderItems)
        Map<String, Integer> categorySales = orderItems.stream()
                // 只保留那些在步骤2中成功映射了类别的产品
                .filter(item -> productToCategory.containsKey(item.getProductId()))
                // 按英语类别名分组，并计算每个组中的项目数 (summingInt(item -> 1) 等同于 counting())
                .collect(Collectors.groupingBy(
                        item -> productToCategory.get(item.getProductId()),
                        Collectors.summingInt(item -> 1)
                ));

        // 步骤 4: 排序并截取
        return categorySales.entrySet().stream()
                .sorted((e1, e2) -> {
                    // 首先，按销量(value)降序排列
                    int salesCompare = Integer.compare(e2.getValue(), e1.getValue());
                    if (salesCompare != 0) return salesCompare;
                    // 如果销量相同，则按类别名(key)字母序升序排列
                    return e1.getKey().compareTo(e2.getKey());
                })
                .limit(10) // 只取前10个
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // 使用 LinkedHashMap 来保持排序后的顺序
                ));
    }

    /**
     * 2. Customer Purchase Patterns by Hour
     * 按小时统计订单数量。
     */
    public Map<String, Long> getPurchasePatternByHour() {
        // 步骤 1: 按小时(0-23)分组并统计订单数
        Map<Integer, Long> hourCounts = orders.stream()
                // 确保订单有购买时间戳
                .filter(o -> o.getOrderPurchaseTimestamp() != null)
                .collect(Collectors.groupingBy(
                        // 提取小时 (0-23)
                        order -> order.getOrderPurchaseTimestamp().getHour(),
                        Collectors.counting() // 统计每个小时的订单数
                ));

        // 步骤 2: 创建并填充一个包含所有24小时的有序 Map
        Map<String, Long> result = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            // 格式化 key 为 "HH:00"
            String hourStr = String.format("%02d:00", hour);
            // 从 hourCounts 中获取该小时的订单数，如果不存在则默认为 0L
            result.put(hourStr, hourCounts.getOrDefault(hour, 0L));
        }

        return result;
    }

    /**
     * 3. Price Range Distribution by Category
     * 按类别统计 *产品* 的 *平均价格* 分布。
     */
    public Map<String, Map<String, Long>> getPriceRangeDistribution() {
        // 步骤 1 & 2: 创建翻译映射 和 Product ID -> 英语类别 映射 (同 Q1)
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

        // 步骤 3: 【关键】计算 *每个产品* 的 *平均* 售价
        // (注意: 这不是按 item 计价, 而是按 product_id 计价)
        Map<String, Double> productAvgPrice = orderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItems::getProductId, // 按产品ID分组
                        Collectors.averagingDouble(OrderItems::getPrice) // 计算该产品所有 item 的平均价格
                ));

        // 步骤 4: 统计每个类别中，产品落在各个价格区间的数量
        Map<String, Map<String, Long>> result = productToCategory.entrySet().stream()
                // 过滤掉那些没有价格信息的产品 (即不在 productAvgPrice 映射中的)
                .filter(entry -> productAvgPrice.containsKey(entry.getKey()))
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue(), // 按英语类别名 (value) 分组 (外层Map)
                        Collectors.groupingBy(
                                // 按产品的平均价格所属的区间 (getPriceRange) 分组 (内层Map)
                                entry -> getPriceRange(productAvgPrice.get(entry.getKey())),
                                Collectors.counting() // 统计每个区间内的产品数量
                        )
                ));

        // 步骤 5: 定义价格区间顺序
        String[] ranges = {"(0,50]", "(50,100]", "(100,200]", "(200,500]", "(500,)"};
        // 步骤 6: 格式化输出，确保按字母序且包含所有区间（即使为0）
        Map<String, Map<String, Long>> sortedResult = new TreeMap<>(); // 使用 TreeMap 自动按类别名(Key)排序

        result.forEach((category, rangeMap) -> {
            Map<String, Long> sortedRanges = new LinkedHashMap<>(); // 使用 LinkedHashMap 保证区间顺序
            for (String range : ranges) {
                // 填充0
                sortedRanges.put(range, rangeMap.getOrDefault(range, 0L));
            }
            sortedResult.put(category, sortedRanges);
        });

        return sortedResult;
    }

    /**
     * Q3 的辅助方法，根据价格返回对应的区间字符串
     */
    public String getPriceRange(double price) {
        if (price <= 50) return "(0,50]";
        if (price <= 100) return "(50,100]";
        if (price <= 200) return "(100,200]";
        if (price <= 500) return "(200,500]";
        return "(500,)";
    }

    /**
     * 4. Seller Performance Analysis
     * (注意: 此方法完全基于您提供的代码片段实现，以匹配 Q4.txt)
     */
    public Map<String, List<Double>> analyzeSellerPerformance(){
        // --- 预计算 步骤 ---
        // 1. 计算每个 order_id 对应的 *分数总和*
        Map<String,Double> TransOrderIdToAverageReviewScore = orderReviews.stream()
                .collect(Collectors.groupingBy(
                        orderReview -> orderReview.getOrderId(),
                        Collectors.summingDouble(orderReview -> orderReview.getReviewScore())
                ));

        // 2. 计算每个 order_id 对应的 *评论条目数*
        Map<String,Long> TransOrderIdToNumberOfReview = orderReviews.stream()
                .collect(Collectors.groupingBy(
                        orderReview -> orderReview.getOrderId(),
                        Collectors.counting()
                ));

        // 3. 计算每个 order_id 是否按时交付
        Map<String,Boolean> TransOrderIdToOntimeDeliver = orders.stream()
                // 过滤掉没有送达日期的订单
                .filter(order -> order.getOrderDeliveredCustomerDate() != null && order.getOrderEstimatedDeliveryDate() != null)
                .collect(Collectors.toMap(
                        order -> order.getOrderId(),
                        // 准时(true)的条件是：实际送达日期 *不晚于* 预计送达日期
                        order -> !(order.getOrderDeliveredCustomerDate().isAfter(order.getOrderEstimatedDeliveryDate())),
                        (o1, o2) -> o1 // 处理重复的 order_id (保留第一个)
                ));

        // 4. 标记每个 order_id 是否具有计算“准时率”所需的两个日期
        Map<String, Boolean> orderIdToHasDeliveryDate = orders.stream()
                .collect(Collectors.toMap(
                        order -> order.getOrderId(),
                        order -> order.getOrderDeliveredCustomerDate() != null && order.getOrderEstimatedDeliveryDate() != null,
                        (o1, o2) -> o1
                ));

        // --- 核心计算 步骤 ---
        Map<String,List<Double>> result = orderItems.stream()
                .collect(Collectors.groupingBy(
                        orderItem -> orderItem.getSellerId(), // 1. 按 sellerId 分组
                        Collectors.collectingAndThen( // 2. 对每个组执行以下操作
                                Collectors.toList(), // 2a. 先把该卖家的所有 item 收集到一个 List (sellerItem)
                                sellerItem -> { // 2b. 然后对这个 List (sellerItem) 执行此 lambda

                                    // 3. 计算不重复的 order ID 集合 (用于计算 AvgOrderValue, AvgScore, OnTimeRate 的基数)
                                    Set<String> orderIds = sellerItem.stream()
                                            .map(item -> item.getOrderId())
                                            .collect(Collectors.toSet());

                                    // 4. 筛选出那些有交付日期的不重复订单 (用于 OnTimeRate)
                                    List<String> ordersWithDeliveryDate = orderIds.stream()
                                            .filter(orderId -> orderIdToHasDeliveryDate.getOrDefault(orderId,false))
                                            .collect(Collectors.toList());

                                    // 5. 资格检查：订单总数（不重复）必须 >= 50
                                    if (orderIds.size() < 50) {
                                        return null; // 返回 null，后续会被 filter 掉
                                    }

                                    // 6. 计算指标 4 (平均分) 的分母：
                                    // 遍历所有不重复的 orderId，从预计算的 Map 中查找它们的评论条数，并求和
                                    long NumberOfReview = orderIds.stream()
                                            .mapToLong(orderId -> TransOrderIdToNumberOfReview.getOrDefault(orderId, 0L))
                                            .sum(); // (这是 逻辑 A：评论条目总数)

                                    // 7. 计算指标 1 (总销售额)：
                                    // 遍历 *所有* item (sellerItem 列表)，并对 price 求和
                                    Double sumOfSell = sellerItem.stream()
                                            .mapToDouble(item -> (item.getPrice()))
                                            .sum();

                                    // 8. 计算指标 3 (唯一产品数)：
                                    // 遍历 *所有* item (sellerItem 列表)，对 productId 去重并计数
                                    Long uniqueProduct = sellerItem.stream()
                                            .map(item -> item.getProductId())
                                            .distinct()
                                            .count();

                                    // 9. 计算指标 4 (平均分) 的分子：
                                    // 遍历所有不重复的 orderId，从预计算的 Map 中查找它们的分数总和，并求和
                                    double totalReviewScore = orderIds.stream()
                                            .map(orderid -> TransOrderIdToAverageReviewScore.get(orderid))
                                            .filter(Objects::nonNull) // 过滤掉没有评论的订单 (其在Map中为null)
                                            .mapToDouble(Double::doubleValue)
                                            .sum(); // (这是 逻辑 A：评论分数总和)

                                    // 10. 计算指标 5 (准时率)
                                    long totalOrders = ordersWithDeliveryDate.size(); // 分母：有日期的订单
                                    long OntimeOrders = ordersWithDeliveryDate.stream()
                                            .filter(orderId -> TransOrderIdToOntimeDeliver.getOrDefault(orderId, false)) // 分子：准时的订单
                                            .count();
                                    double OntimeRate = totalOrders > 0 ? (double) OntimeOrders / totalOrders : 0.00;

                                    // 11. 计算指标 4 (平均分)：
                                    double avgScore = 0.0;
                                    if (NumberOfReview > 0) { // 防止除以 0
                                        avgScore = Math.round(totalReviewScore / NumberOfReview * 100.0) / 100.0; // (逻辑 A)
                                    }

                                    // 12. 按顺序返回所有指标
                                    return Arrays.asList(
                                            Math.round(sumOfSell * 100.0) / 100.0, // 1. 总销售额
                                            Math.round(sumOfSell / orderIds.size() * 100.0) / 100.0, // 2. 平均订单额 (使用不重复订单数为分母)
                                            (double) uniqueProduct, // 3. 唯一产品数
                                            avgScore, // 4. 平均评分
                                            Math.round(OntimeRate * 100.0) / 100.0 // 5. 准时率
                                    );
                                }
                        )
                ));
        // --- 最终处理 步骤 ---
        return result.entrySet().stream()
                .filter(entry -> entry.getValue() != null) // 过滤掉不满足 50 单的卖家
                .sorted(Comparator
                        .comparing((Map.Entry<String, List<Double>> e) -> e.getValue().get(0),Comparator.reverseOrder()) // 按总销售额(index 0)降序
                        .thenComparing(e -> e.getKey())) // 按 sellerId 字母序
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // 保持排序顺序
                ));
    }

    /**
     * 5. Top-10 Recommended Products by Category
     * (注意: 此方法使用 逻辑 A 来匹配 Q5.txt 文件)
     */
    public Map<String, List<String>> recommendedProducts() {
        // --- 预计算 步骤 ---
        // 1. 创建 葡语 -> 英语 翻译映射
        Map<String, String> translate = categoryNameTranslations.stream()
                .collect(Collectors.toMap(
                        CategoryNameTranslation::getProductCategoryName,
                        CategoryNameTranslation::getProductCategoryNameEnglish,
                        (e1, e2) -> e1
                ));
        // 2. 创建 Product ID -> 英语类别 映射
        Map<String, String> productToEnglishCategory = products.stream()
                .filter(p -> p.getProductCategoryName() != null && !p.getProductCategoryName().isEmpty() && translate.containsKey(p.getProductCategoryName()))
                .collect(Collectors.toMap(
                        Products::getProductId,
                        p -> translate.get(p.getProductCategoryName()),
                        (e1, e2) -> e1
                ));
        // 3. 创建 Order ID -> 该订单所有评论分数(List) 的映射
        Map<String, List<Integer>> orderToAllScores = orderReviews.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        OrderReviews::getOrderId,
                        Collectors.mapping(OrderReviews::getReviewScore, Collectors.toList())
                ));
        Set<String> reviewedOrderIds = orderToAllScores.keySet();

        // --- 计算产品指标 步骤 ---
        // 1. 按 Product ID 分组所有 item
        Map<String, List<OrderItems>> itemsByProduct = orderItems.stream()
                .filter(item -> item != null && item.getProductId() != null && productToEnglishCategory.containsKey(item.getProductId()))
                .collect(Collectors.groupingBy(OrderItems::getProductId));

        Map<String, ProductMetrics> productMetricsMap = new HashMap<>();
        for (Map.Entry<String, List<OrderItems>> entry : itemsByProduct.entrySet()) {
            String productId = entry.getKey();
            List<OrderItems> items = entry.getValue();

            // 指标 1: 销量 (即 item 数量)
            int salesCount = items.size();

            // 找到该产品关联的、有评论的订单
            Set<String> distinctOrderIds = items.stream()
                    .map(OrderItems::getOrderId)
                    .collect(Collectors.toSet());
            Set<String> distinctReviewedOrdersForProduct = distinctOrderIds.stream()
                    .filter(reviewedOrderIds::contains)
                    .collect(Collectors.toSet());

            // 逻辑 A: 获取这些订单的 *所有* 评论条目
            List<Integer> allScoresForProduct = distinctReviewedOrdersForProduct.stream()
                    .flatMap(orderId -> orderToAllScores.getOrDefault(orderId, Collections.emptyList()).stream())
                    .collect(Collectors.toList());

            // 逻辑 A: 指标 2: 评论数 (基于 *评论条目总数*)
            long reviewCount = allScoresForProduct.size();

            // 逻辑 A: 指标 3: 平均评分 (基于 *评论条目平均分*)
            double avgRating = allScoresForProduct.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

            // 逻辑 A: 过滤器 (基于 *评论条目总数*)
            if (salesCount >= 10 && reviewCount >= 5) {
                productMetricsMap.put(productId, new ProductMetrics(salesCount, reviewCount, avgRating));
            }
        }

        // --- 分组 步骤 ---
        // 1. 将合格的产品按类别分组
        Map<String, List<ProductWithMetrics>> categoryProductMetrics = new HashMap<>();
        for (Map.Entry<String, ProductMetrics> entry : productMetricsMap.entrySet()) {
            String productId = entry.getKey();
            ProductMetrics metrics = entry.getValue();
            String category = productToEnglishCategory.get(productId);
            categoryProductMetrics
                    .computeIfAbsent(category, k -> new ArrayList<>())
                    .add(new ProductWithMetrics(productId, metrics));
        }

        // --- 计分和排序 步骤 ---
        // 1. 初始化最终 Map，使用 TreeMap 保证类别按字母序
        // 并且预先填充所有类别，以确保空列表 `[]` 也被输出
        Map<String, List<String>> finalRecommendations = new TreeMap<>();
        translate.values().stream().distinct().forEach(englishCategoryName -> {
            finalRecommendations.put(englishCategoryName, new ArrayList<>());
        });

        // 2. 遍历所有 *有合格产品* 的类别
        for (Map.Entry<String, List<ProductWithMetrics>> categoryEntry : categoryProductMetrics.entrySet()) {
            String category = categoryEntry.getKey();
            List<ProductWithMetrics> productsInCategory = categoryEntry.getValue();

            if (productsInCategory.isEmpty()) {
                continue;
            }

            // 3. 找到该类别中各项指标的 Min/Max 值，用于归一化
            double minSales = productsInCategory.stream().mapToDouble(p -> p.metrics.salesCount).min().orElse(0.0);
            double maxSales = productsInCategory.stream().mapToDouble(p -> p.metrics.salesCount).max().orElse(minSales);
            double minReviews = productsInCategory.stream().mapToDouble(p -> p.metrics.reviewCount).min().orElse(0.0);
            double maxReviews = productsInCategory.stream().mapToDouble(p -> p.metrics.reviewCount).max().orElse(minReviews);
            double minRating = productsInCategory.stream().mapToDouble(p -> p.metrics.avgRating).min().orElse(0.0);
            double maxRating = productsInCategory.stream().mapToDouble(p -> p.metrics.avgRating).max().orElse(minRating);

            // 4. 计算归一化范围
            final double salesRange = (maxSales - minSales);
            final double reviewRange = (maxReviews - minReviews);
            final double ratingRange = (maxRating - minRating);

            // 5. 计算每个产品的最终分数
            List<ProductWithScore> scoredProducts = productsInCategory.stream()
                    .map(p -> {
                        // 归一化 (Min-Max Scaling)
                        double salesScore = (salesRange == 0) ? 1.0 : (p.metrics.salesCount - minSales) / salesRange;
                        double reviewScore = (reviewRange == 0) ? 1.0 : (p.metrics.reviewCount - minReviews) / reviewRange;
                        double ratingScore = (ratingRange == 0) ? 1.0 : (p.metrics.avgRating - minRating) / ratingRange;

                        // 计算加权总分
                        double finalScore = 0.5 * salesScore + 0.3 * reviewScore + 0.2 * ratingScore;
                        return new ProductWithScore(p.productId, finalScore);
                    })
                    .collect(Collectors.toList());

            // 6. 排序和截取
            List<String> top10Products = scoredProducts.stream()
                    .sorted((p1, p2) -> {
                        // 按分数降序
                        int scoreCompare = Double.compare(p2.score, p1.score);
                        if (scoreCompare != 0) return scoreCompare;
                        // 分数相同，按 Product ID 升序
                        return p1.productId.compareTo(p2.productId);
                    })
                    .limit(10) // 取前10
                    .map(p -> p.productId)
                    .collect(Collectors.toList());

            // 7. 将结果放入最终的 Map 中
            finalRecommendations.put(category, top10Products);
        }

        return finalRecommendations;
    }

    // --- Q5 的辅助内部类 ---

    /** 存储产品的基础指标 (用于Q5) */
    private static class ProductMetrics {
        int salesCount;
        long reviewCount;  // 评论条目总数 (逻辑 A)
        double avgRating;  // 评论条目平均分 (逻辑 A)

        ProductMetrics(int salesCount, long reviewCount, double avgRating) {
            this.salesCount = salesCount;
            this.reviewCount = reviewCount;
            this.avgRating = avgRating;
        }
    }

    /** 关联 Product ID 和其指标 */
    private static class ProductWithMetrics {
        String productId;
        ProductMetrics metrics;
        ProductWithMetrics(String productId, ProductMetrics metrics) {
            this.productId = productId;
            this.metrics = metrics;
        }
    }

    /** 关联 Product ID 和其最终分数 */
    private static class ProductWithScore {
        String productId;
        double score;
        ProductWithScore(String productId, double score) {
            this.productId = productId;
            this.score = score;
        }
    }
}

// --- (数据模型类) ---

/** 代表 olist_orders_dataset.csv 中的一行 */
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

    // Getters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getOrderStatus() { return orderStatus; }
    public LocalDateTime getOrderPurchaseTimestamp() { return orderPurchaseTimestamp; }
    public LocalDateTime getOrderApprovedAt() { return orderApprovedAt; }
    public LocalDateTime getOrderDeliveredCarrierDate() { return orderDeliveredCarrierDate; }
    public LocalDateTime getOrderDeliveredCustomerDate() { return orderDeliveredCustomerDate; }
    public LocalDateTime getOrderEstimatedDeliveryDate() { return orderEstimatedDeliveryDate; }
}

/** 代表 olist_order_items_dataset.csv 中的一行 */
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

    // Getters
    public String getOrderId() { return orderId; }
    public int getOrderItemId() { return orderItemId; }
    public String getProductId() { return productId; }
    public String getSellerId() { return sellerId; }
    public LocalDateTime getShippingLimitDate() { return shippingLimitDate; }
    public double getPrice() { return price; }
    public double getFreightValue() { return freightValue; }
}

/** 代表 olist_order_reviews_dataset.csv 中的一行 */
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

    // Getters
    public String getReviewId() { return reviewId; }
    public String getOrderId() { return orderId; }
    public int getReviewScore() { return reviewScore; }
    public String getReviewCommentTitle() { return reviewCommentTitle; }
    public String getReviewCommentMessage() { return reviewCommentMessage; }
    public LocalDateTime getReviewCreationDate() { return reviewCreationDate; }
    public LocalDateTime getReviewAnswerTimestamp() { return reviewAnswerTimestamp; }
}

/** 代表 olist_products_dataset.csv 中的一行 */
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

    // Getters
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

/** 代表 product_category_name_translation.csv 中的一行 */
class CategoryNameTranslation {
    String productCategoryName;
    String productCategoryNameEnglish;

    public CategoryNameTranslation(String productCategoryName,
                                   String productCategoryNameEnglish) {
        this.productCategoryName = productCategoryName;
        this.productCategoryNameEnglish = productCategoryNameEnglish;
    }

    // Getters
    public String getProductCategoryName() { return productCategoryName; }
    public String getProductCategoryNameEnglish() { return productCategoryNameEnglish; }
}