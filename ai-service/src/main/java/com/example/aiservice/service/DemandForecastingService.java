package com.example.aiservice.service;

import com.example.aiservice.dto.DemandForecastResponse;
import com.example.aiservice.dto.ProductDemandForecastResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandForecastingService {

    private final WebClient.Builder webClientBuilder;
    private final GeminiService geminiService;

    @Value("${api.gateway.url:http://api-gateway:8080}")
    private String apiGatewayUrl;

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * Dự đoán nhu cầu nhập hàng dựa trên lịch sử nhập - xuất - bán
     */
    public DemandForecastResponse forecastDemand(String token) {
        try {
            // Lấy dữ liệu 90 ngày gần nhất để phân tích
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(90);

            List<Map<String, Object>> stocks = fetchStocks(token);
            List<Map<String, Object>> products = fetchProducts(token);
            List<Map<String, Object>> imports = fetchImports(token, startDate, endDate);
            List<Map<String, Object>> exports = fetchExports(token, startDate, endDate);

            // Tạo map productId -> product info
            Map<Long, Map<String, Object>> productMap = products.stream()
                    .collect(Collectors.toMap(
                            p -> Long.valueOf(p.get("id").toString()),
                            p -> p,
                            (a, b) -> a));

            // Tính toán dự đoán cho từng sản phẩm
            List<DemandForecastResponse.ForecastItem> forecasts = new ArrayList<>();

            for (Map<String, Object> stock : stocks) {
                Long productId = Long.valueOf(stock.get("productId").toString());
                Map<String, Object> product = productMap.get(productId);
                if (product == null)
                    continue;

                Integer currentStock = getIntegerValue(stock, "quantity", 0);

                // Tính tốc độ bán trung bình
                Double avgDailySales = calculateAverageDailySales(exports, productId);

                // Tính tốc độ nhập trung bình
                Double avgDailyImport = calculateAverageDailyImport(imports, productId);

                // Dự đoán số ngày còn lại
                Integer daysUntilReorder = null;
                Integer recommendedQuantity = null;
                Integer optimalStock = null;
                Double confidence = 0.5;
                String reasoning = "";

                if (avgDailySales > 0) {
                    // Số ngày dự đoán cần nhập lại
                    int daysRemaining = (int) Math.ceil(currentStock / avgDailySales);
                    daysUntilReorder = Math.max(0, daysRemaining - 7); // Trừ 7 ngày buffer

                    // Số lượng nhập đề xuất = tốc độ bán * số ngày muốn duy trì (14 ngày)
                    recommendedQuantity = (int) Math.ceil(avgDailySales * 14);

                    // Mức tồn tối ưu = tốc độ bán * 21 ngày (3 tuần)
                    optimalStock = (int) Math.ceil(avgDailySales * 21);

                    // Độ tin cậy dựa trên số lượng dữ liệu
                    int dataPoints = countDataPoints(exports, imports, productId);
                    confidence = Math.min(0.95, 0.5 + (dataPoints * 0.05));

                    reasoning = String.format(
                            "Dựa trên tốc độ bán trung bình %.1f sản phẩm/ngày trong 90 ngày qua. " +
                                    "Tồn kho hiện tại: %d. Dự đoán cần nhập lại sau %d ngày.",
                            avgDailySales, currentStock, daysUntilReorder);
                } else if (currentStock == 0) {
                    // Sản phẩm hết hàng
                    daysUntilReorder = 0;
                    recommendedQuantity = calculateRecommendedQuantityFromHistory(imports, productId);
                    optimalStock = recommendedQuantity;
                    confidence = 0.7;
                    reasoning = "Sản phẩm đã hết hàng. Đề xuất nhập lại dựa trên lịch sử nhập hàng trước đó.";
                }

                if (daysUntilReorder != null && daysUntilReorder <= 30) {
                    DemandForecastResponse.ForecastItem forecast = new DemandForecastResponse.ForecastItem();
                    forecast.setProductId(productId);
                    forecast.setProductCode(String.valueOf(product.getOrDefault("code", "")));
                    forecast.setProductName(String.valueOf(product.getOrDefault("name", "")));
                    forecast.setCurrentStock(currentStock);
                    forecast.setPredictedDaysUntilReorder(daysUntilReorder);
                    forecast.setRecommendedQuantity(recommendedQuantity);
                    forecast.setOptimalStockLevel(optimalStock);
                    forecast.setConfidence(confidence);
                    forecast.setReasoning(reasoning);
                    forecasts.add(forecast);
                }
            }

            // Sắp xếp theo độ ưu tiên (ngày cần nhập gần nhất trước)
            forecasts.sort(Comparator.comparing(DemandForecastResponse.ForecastItem::getPredictedDaysUntilReorder));

            // Tạo summary bằng AI
            String summary = generateForecastSummary(forecasts);
            String analysis = generateForecastAnalysis(forecasts, products.size());

            return new DemandForecastResponse(forecasts, summary, analysis);

        } catch (Exception e) {
            log.error("Error forecasting demand", e);
            throw new RuntimeException("Không thể dự đoán nhu cầu nhập hàng: " + e.getMessage());
        }
    }

    private Double calculateAverageDailySales(List<Map<String, Object>> exports, Long productId) {
        int totalSold = 0;
        Set<String> dates = new HashSet<>();

        for (Map<String, Object> export : exports) {
            // Chỉ tính các phiếu xuất đã được xác nhận (status = "EXPORTED")
            Object status = export.get("status");
            if (status == null || !"EXPORTED".equals(status.toString())) {
                continue; // Bỏ qua các phiếu chờ duyệt, đã hủy, v.v.
            }

            Object exportDate = export.get("exportDate");
            if (exportDate != null) {
                dates.add(exportDate.toString());
            }

            Object items = export.get("items");
            if (items instanceof List) {
                for (Object item : (List<?>) items) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        if (Objects.equals(getLongValue(itemMap, "productId"), productId)) {
                            totalSold += getIntegerValue(itemMap, "quantity", 0);
                        }
                    }
                }
            }
        }

        int days = Math.max(1, dates.size());
        return totalSold / (double) days;
    }

    private Double calculateAverageDailyImport(List<Map<String, Object>> imports, Long productId) {
        int totalImported = 0;
        Set<String> dates = new HashSet<>();

        for (Map<String, Object> importOrder : imports) {
            Object importDate = importOrder.get("importDate");
            if (importDate != null) {
                dates.add(importDate.toString());
            }

            Object items = importOrder.get("items");
            if (items instanceof List) {
                for (Object item : (List<?>) items) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        if (Objects.equals(getLongValue(itemMap, "productId"), productId)) {
                            totalImported += getIntegerValue(itemMap, "quantity", 0);
                        }
                    }
                }
            }
        }

        int days = Math.max(1, dates.size());
        return totalImported / (double) days;
    }

    private Integer calculateRecommendedQuantityFromHistory(List<Map<String, Object>> imports, Long productId) {
        List<Integer> quantities = new ArrayList<>();

        for (Map<String, Object> importOrder : imports) {
            Object items = importOrder.get("items");
            if (items instanceof List) {
                for (Object item : (List<?>) items) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        if (Objects.equals(getLongValue(itemMap, "productId"), productId)) {
                            quantities.add(getIntegerValue(itemMap, "quantity", 0));
                        }
                    }
                }
            }
        }

        if (quantities.isEmpty()) {
            return 100; // Default
        }

        // Lấy trung bình của 3 lần nhập gần nhất
        quantities.sort(Collections.reverseOrder());
        int sum = quantities.stream().limit(3).mapToInt(Integer::intValue).sum();
        return sum / Math.min(3, quantities.size());
    }

    private int countDataPoints(List<Map<String, Object>> exports, List<Map<String, Object>> imports, Long productId) {
        int count = 0;
        for (Map<String, Object> export : exports) {
            Object items = export.get("items");
            if (items instanceof List) {
                for (Object item : (List<?>) items) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        if (Objects.equals(getLongValue(itemMap, "productId"), productId)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private String generateForecastSummary(List<DemandForecastResponse.ForecastItem> forecasts) {
        if (forecasts.isEmpty()) {
            return "Không có sản phẩm nào cần nhập hàng trong 30 ngày tới.";
        }

        long urgent = forecasts.stream().filter(f -> f.getPredictedDaysUntilReorder() <= 5).count();
        long soon = forecasts.stream()
                .filter(f -> f.getPredictedDaysUntilReorder() > 5 && f.getPredictedDaysUntilReorder() <= 14).count();
        long later = forecasts.stream().filter(f -> f.getPredictedDaysUntilReorder() > 14).count();

        return String.format(
                "Dự đoán: %d sản phẩm cần nhập hàng trong 30 ngày tới (%d khẩn cấp, %d sớm, %d sau).",
                forecasts.size(), urgent, soon, later);
    }

    private String generateForecastAnalysis(List<DemandForecastResponse.ForecastItem> forecasts, int totalProducts) {
        if (forecasts.isEmpty()) {
            return "Tất cả sản phẩm đều có tồn kho đủ trong 30 ngày tới.";
        }

        StringBuilder context = new StringBuilder("Phân tích dự đoán nhu cầu nhập hàng:\n");
        context.append(String.format("- Tổng số sản phẩm: %d\n", totalProducts));
        context.append(String.format("- Số sản phẩm cần nhập: %d\n", forecasts.size()));

        context.append("- Top 5 sản phẩm cần nhập sớm nhất:\n");
        forecasts.stream().limit(5).forEach(f -> {
            context.append(String.format("  + %s: Cần nhập sau %d ngày, đề xuất %d sản phẩm\n",
                    f.getProductName(), f.getPredictedDaysUntilReorder(), f.getRecommendedQuantity()));
        });

        String prompt = "Bạn là chuyên gia phân tích chuỗi cung ứng. " +
                "Hãy phân tích dữ liệu dự đoán nhu cầu nhập hàng sau và đưa ra nhận định tổng quan:\n\n" +
                context.toString() +
                "\nHãy đưa ra phân tích ngắn gọn (2-3 câu) về tình hình tồn kho và đề xuất hành động.";

        String analysis = geminiService.invokeGemini(prompt);
        if (analysis == null || analysis.isBlank()) {
            throw new RuntimeException("Gemini không trả về phân tích dự đoán nhu cầu.");
        }
        return analysis.trim();
    }

    private List<Map<String, Object>> fetchStocks(String token) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(apiGatewayUrl).build();
            Map<String, Object> response = webClient.get()
                    .uri("/api/stocks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block(TIMEOUT);

            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching stocks", e);
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> fetchProducts(String token) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(apiGatewayUrl).build();
            Map<String, Object> response = webClient.get()
                    .uri("/api/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block(TIMEOUT);

            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching products", e);
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> fetchImports(String token, LocalDate from, LocalDate to) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(apiGatewayUrl).build();
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/imports")
                            .queryParam("from", from.toString())
                            .queryParam("to", to.toString())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block(TIMEOUT);

            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching imports", e);
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> fetchExports(String token, LocalDate from, LocalDate to) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(apiGatewayUrl).build();
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/exports")
                            .queryParam("from", from.toString())
                            .queryParam("to", to.toString())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block(TIMEOUT);

            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching exports", e);
            return new ArrayList<>();
        }
    }

    private Integer getIntegerValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * Dự đoán nhu cầu cho một sản phẩm cụ thể
     */
    public ProductDemandForecastResponse forecastProductDemand(String token, Long productId, Integer days) {
        try {
            // Lấy dữ liệu 90 ngày gần nhất để phân tích
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(90);

            List<Map<String, Object>> stocks = fetchStocks(token);
            List<Map<String, Object>> products = fetchProducts(token);
            List<Map<String, Object>> exports = fetchExports(token, startDate, endDate);

            // Tìm sản phẩm
            Map<String, Object> product = products.stream()
                    .filter(p -> Objects.equals(getLongValue(p, "id"), productId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

            // Tìm tồn kho hiện tại
            Integer currentStock = stocks.stream()
                    .filter(s -> Objects.equals(getLongValue(s, "productId"), productId))
                    .mapToInt(s -> getIntegerValue(s, "quantity", 0))
                    .sum();

            // Tính tốc độ bán trung bình
            Double avgDailySales = calculateAverageDailySales(exports, productId);

            // Tính số ngày dự đoán sẽ hết hàng
            Integer predictedDaysUntilStockOut = null;
            if (avgDailySales > 0) {
                predictedDaysUntilStockOut = (int) Math.ceil(currentStock / avgDailySales);
            } else if (currentStock == 0) {
                predictedDaysUntilStockOut = 0;
            }

            // Tính số lượng nhập đề xuất
            Integer recommendedReorderQuantity = null;
            if (avgDailySales > 0) {
                recommendedReorderQuantity = (int) Math.ceil(avgDailySales * 14); // Đủ cho 14 ngày
            }

            // Mức tồn tối ưu
            Integer optimalStockLevel = null;
            if (avgDailySales > 0) {
                optimalStockLevel = (int) Math.ceil(avgDailySales * 21); // 3 tuần
            }

            // Độ tin cậy
            int dataPoints = countDataPoints(exports, new ArrayList<>(), productId);
            Double confidence = Math.min(0.95, 0.5 + (dataPoints * 0.05));

            // Tạo dự đoán theo từng ngày
            List<ProductDemandForecastResponse.DailyForecast> dailyForecasts = new ArrayList<>();
            if (avgDailySales != null && avgDailySales > 0 && predictedDaysUntilStockOut != null) {
                int forecastDays = Math.min(days, Math.max(predictedDaysUntilStockOut + 7, 7));
                double remainingStock = currentStock;
                int consecutiveZeroStockDays = 0; // Đếm số ngày liên tiếp hết hàng

                for (int day = 1; day <= forecastDays; day++) {
                    // Tính số lượng bán dự đoán cho ngày này
                    // Nếu còn hàng thì bán theo tốc độ bán trung bình, nhưng không vượt quá tồn kho
                    // còn lại
                    int predictedSales = 0;
                    if (remainingStock > 0) {
                        predictedSales = (int) Math.min(Math.round(avgDailySales), remainingStock);
                    }

                    // Tồn kho sau khi bán = tồn kho trước đó - số lượng bán
                    remainingStock = Math.max(0, remainingStock - predictedSales);
                    int predictedStock = (int) Math.round(remainingStock);

                    // Đếm số ngày liên tiếp hết hàng
                    if (predictedStock == 0) {
                        consecutiveZeroStockDays++;
                    } else {
                        consecutiveZeroStockDays = 0; // Reset nếu có hàng lại
                    }

                    LocalDate forecastDate = LocalDate.now().plusDays(day);

                    ProductDemandForecastResponse.DailyForecast forecast = new ProductDemandForecastResponse.DailyForecast(
                            day, predictedStock, predictedSales, forecastDate.toString());
                    dailyForecasts.add(forecast);

                    // Dừng nếu đã hết hàng quá 2 ngày liên tiếp (sau khi đã thêm ngày thứ 2)
                    if (consecutiveZeroStockDays > 2) {
                        break;
                    }
                }
            } else if (currentStock == 0) {
                // Nếu đã hết hàng, chỉ hiển thị 2 ngày (ngày hiện tại + 1 ngày sau)
                for (int day = 1; day <= 2; day++) {
                    LocalDate forecastDate = LocalDate.now().plusDays(day);
                    ProductDemandForecastResponse.DailyForecast forecast = new ProductDemandForecastResponse.DailyForecast(
                            day, 0, 0, forecastDate.toString());
                    dailyForecasts.add(forecast);
                }
            }

            // Tạo phân tích chi tiết bằng AI
            String detailedAnalysis = generateProductAnalysis(
                    product, currentStock, avgDailySales, predictedDaysUntilStockOut, exports);
            String recommendations = generateProductRecommendations(
                    currentStock, avgDailySales, predictedDaysUntilStockOut, recommendedReorderQuantity);

            ProductDemandForecastResponse response = new ProductDemandForecastResponse();
            response.setProductId(productId);
            response.setProductCode(String.valueOf(product.getOrDefault("code", "")));
            response.setProductName(String.valueOf(product.getOrDefault("name", "")));
            response.setCurrentStock(currentStock);
            response.setAvgDailySales(avgDailySales != null ? avgDailySales : 0.0);
            response.setPredictedDaysUntilStockOut(predictedDaysUntilStockOut);
            response.setRecommendedReorderQuantity(recommendedReorderQuantity);
            response.setOptimalStockLevel(optimalStockLevel);
            response.setConfidence(confidence);
            response.setDetailedAnalysis(detailedAnalysis);
            response.setRecommendations(recommendations);
            response.setDailyForecasts(dailyForecasts);

            return response;

        } catch (Exception e) {
            log.error("Error forecasting product demand", e);
            throw new RuntimeException("Không thể dự đoán nhu cầu cho sản phẩm: " + e.getMessage());
        }
    }

    private String generateProductAnalysis(Map<String, Object> product, Integer currentStock,
            Double avgDailySales, Integer predictedDaysUntilStockOut, List<Map<String, Object>> exports) {
        StringBuilder context = new StringBuilder("Phân tích dự báo nhu cầu cho sản phẩm:\n");
        context.append(String.format("- Mã sản phẩm: %s\n", product.getOrDefault("code", "")));
        context.append(String.format("- Tên sản phẩm: %s\n", product.getOrDefault("name", "")));
        context.append(String.format("- Tồn kho hiện tại: %d\n", currentStock));

        if (avgDailySales != null && avgDailySales > 0) {
            context.append(String.format("- Tốc độ bán trung bình: %.2f sản phẩm/ngày\n", avgDailySales));
        } else {
            context.append("- Tốc độ bán: Không có dữ liệu bán hàng trong 90 ngày qua\n");
        }

        if (predictedDaysUntilStockOut != null) {
            if (predictedDaysUntilStockOut == 0) {
                context.append("- Tình trạng: Đã hết hàng\n");
            } else if (predictedDaysUntilStockOut <= 7) {
                context.append(String.format("- Cảnh báo: Sẽ hết hàng sau %d ngày (KHẨN CẤP)\n",
                        predictedDaysUntilStockOut));
            } else if (predictedDaysUntilStockOut <= 14) {
                context.append(String.format("- Cảnh báo: Sẽ hết hàng sau %d ngày (CẦN CHÚ Ý)\n",
                        predictedDaysUntilStockOut));
            } else {
                context.append(String.format("- Dự đoán: Sẽ hết hàng sau %d ngày\n", predictedDaysUntilStockOut));
            }
        }

        String prompt = "Bạn là chuyên gia phân tích dự báo nhu cầu và quản trị kho hàng. " +
                "Hãy phân tích chi tiết dữ liệu sau và đưa ra nhận định về tình hình tồn kho và dự báo nhu cầu:\n\n"
                +
                context.toString() +
                "\nHãy đưa ra phân tích chi tiết (3-4 câu) về:\n" +
                "1. Tình hình tồn kho hiện tại\n" +
                "2. Xu hướng bán hàng\n" +
                "3. Rủi ro hết hàng\n" +
                "4. Khuyến nghị hành động cụ thể";

        String result = geminiService.invokeGemini(prompt);
        if (result == null || result.isBlank()) {
            throw new RuntimeException("Gemini không trả về phân tích chi tiết sản phẩm.");
        }
        return result.trim();
    }

    private String generateProductRecommendations(Integer currentStock, Double avgDailySales,
            Integer predictedDaysUntilStockOut, Integer recommendedReorderQuantity) {
        StringBuilder recommendations = new StringBuilder();

        if (currentStock == 0) {
            recommendations.append("🔴 KHẨN CẤP: Sản phẩm đã hết hàng. Cần nhập lại ngay lập tức.\n");
            if (recommendedReorderQuantity != null) {
                recommendations
                        .append(String.format("💡 Đề xuất nhập: %d sản phẩm để đảm bảo đủ hàng cho ít nhất 14 ngày.\n",
                                recommendedReorderQuantity));
            }
        } else if (predictedDaysUntilStockOut != null && predictedDaysUntilStockOut <= 7) {
            recommendations.append("⚠️ CẢNH BÁO: Sản phẩm sẽ hết hàng trong vòng 7 ngày tới.\n");
            recommendations.append("💡 Hành động: Liên hệ nhà cung cấp ngay để đặt hàng.\n");
            if (recommendedReorderQuantity != null) {
                recommendations.append(String.format("💡 Đề xuất nhập: %d sản phẩm.\n", recommendedReorderQuantity));
            }
        } else if (predictedDaysUntilStockOut != null && predictedDaysUntilStockOut <= 14) {
            recommendations.append("⚠️ CHÚ Ý: Sản phẩm sẽ hết hàng trong vòng 14 ngày tới.\n");
            recommendations.append("💡 Hành động: Lên kế hoạch nhập hàng trong tuần này.\n");
            if (recommendedReorderQuantity != null) {
                recommendations.append(String.format("💡 Đề xuất nhập: %d sản phẩm.\n", recommendedReorderQuantity));
            }
        } else if (predictedDaysUntilStockOut != null && predictedDaysUntilStockOut > 14) {
            recommendations.append("✅ Tình trạng: Tồn kho đủ cho ít nhất 14 ngày.\n");
            recommendations.append("💡 Hành động: Theo dõi định kỳ, nhập hàng khi còn 14 ngày.\n");
        } else {
            recommendations.append("ℹ️ Không có đủ dữ liệu để dự đoán. Cần theo dõi thêm.\n");
        }

        if (avgDailySales != null && avgDailySales > 0) {
            recommendations.append(String.format("📊 Tốc độ bán: %.2f sản phẩm/ngày.\n", avgDailySales));
        }

        return recommendations.toString();
    }
}
