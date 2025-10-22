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
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readOrderItems(String folder) {
        try (BufferedReader br = new BufferedReader(new FileReader(folder + "/olist_order_items_dataset.csv", StandardCharsets.UTF_8))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
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
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Products product = new Products(
                        info[0],
                        info[1],
                        Integer.parseInt(info[2]),
                        Integer.parseInt(info[3]),
                        Integer.parseInt(info[4]),
                        Double.parseDouble(info[5]),
                        Double.parseDouble(info[6]),
                        Double.parseDouble(info[7]),
                        Double.parseDouble(info[8])
                );
                products.add(product);
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
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                CategoryNameTranslation t = new CategoryNameTranslation(info[0], info[1]);
                categoryNameTranslations.add(t);
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
        return Map.of();
    }

    public Map<String, List<String>> recommendedProducts() {
        return Map.of();
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