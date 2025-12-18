package com.example.aiservice.controller;

import com.example.aiservice.common.ApiResponse;
import com.example.aiservice.dto.*;
import com.example.aiservice.exception.AiServiceException;
import com.example.aiservice.service.DataService;
import com.example.aiservice.service.GeminiService;
import com.example.aiservice.service.ProductDescriptionParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final GeminiService geminiService;
    private final ProductDescriptionParser descriptionParser;
    private final DataService dataService;
    private final ObjectMapper objectMapper;

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("Received chat request: {}", request.getMessage());
        try {
            // Lấy token từ header
            String token = authHeader != null && authHeader.startsWith("Bearer ") 
                ? authHeader.substring(7) 
                : null;

            String userMessage = request.getMessage().toLowerCase(Locale.ROOT);
            StringBuilder contextData = new StringBuilder();

            // Phân tích câu hỏi và lấy dữ liệu liên quan
            if (token != null) {
                boolean needProducts = false;
                boolean needInventory = false;
                boolean needOrders = false;
                
                // Nhận diện câu hỏi về số lượng, tổng số sản phẩm
                if (userMessage.contains("có bao nhiêu") || userMessage.contains("bao nhiêu sản phẩm") ||
                    userMessage.contains("tổng số") || userMessage.contains("số lượng") ||
                    userMessage.contains("hiện tại") && userMessage.contains("sản phẩm")) {
                    needProducts = true;
                    needInventory = true; // Cần cả thống kê tồn kho
                }
                
                // Nhận diện câu hỏi về tồn kho
                if (userMessage.contains("tồn kho") || userMessage.contains("inventory") || 
                    userMessage.contains("hết hàng") || userMessage.contains("sắp hết") ||
                    userMessage.contains("cảnh báo") || userMessage.contains("warning") ||
                    userMessage.contains("trong kho")) {
                    needInventory = true;
                }
                
                // Nhận diện tìm kiếm sản phẩm cụ thể
                if (userMessage.contains("tìm") || userMessage.contains("search")) {
                    String keyword = extractKeyword(userMessage);
                    if (!keyword.isEmpty() && keyword.length() > 2) {
                        String productData = dataService.searchProducts(keyword, token);
                        contextData.append("DỮ LIỆU SẢN PHẨM:\n").append(productData).append("\n\n");
                    } else {
                        needProducts = true;
                    }
                }
                
                // Nhận diện câu hỏi về sản phẩm (không phải tìm kiếm)
                if (userMessage.contains("sản phẩm") || userMessage.contains("product")) {
                    if (!userMessage.contains("tìm") && !userMessage.contains("search")) {
                        needProducts = true;
                    }
                }
                
                // Nhận diện câu hỏi về đơn hàng, doanh thu
                if (userMessage.contains("đơn hàng") || userMessage.contains("order") || 
                    userMessage.contains("doanh thu") || userMessage.contains("revenue") ||
                    userMessage.contains("bán hàng") || userMessage.contains("sales")) {
                    needOrders = true;
                }
                
                // Lấy dữ liệu theo nhu cầu
                if (needProducts && !contextData.toString().contains("DỮ LIỆU SẢN PHẨM")) {
                    String productsSummary = dataService.getProductsSummary(token);
                    contextData.append("DỮ LIỆU SẢN PHẨM:\n").append(productsSummary).append("\n\n");
                }
                
                if (needInventory && !contextData.toString().contains("DỮ LIỆU TỒN KHO")) {
                    String inventoryData = dataService.getInventorySummary(token);
                    contextData.append("DỮ LIỆU TỒN KHO:\n").append(inventoryData).append("\n\n");
                }
                
                if (needOrders && !contextData.toString().contains("DỮ LIỆU ĐƠN HÀNG")) {
                    String ordersData = dataService.getOrdersSummary(token);
                    contextData.append("DỮ LIỆU ĐƠN HÀNG:\n").append(ordersData).append("\n\n");
                }
                
                // Nếu không có keyword cụ thể và câu hỏi ngắn, lấy tổng quan
                if (contextData.isEmpty() && (
                    userMessage.contains("tổng quan") || userMessage.contains("overview") ||
                    userMessage.contains("thống kê") || userMessage.contains("statistics") ||
                    userMessage.length() < 30 // Câu hỏi ngắn, có thể cần tổng quan
                )) {
                    String inventoryData = dataService.getInventorySummary(token);
                    String ordersData = dataService.getOrdersSummary(token);
                    contextData.append("DỮ LIỆU TỒN KHO:\n").append(inventoryData).append("\n\n");
                    contextData.append("DỮ LIỆU ĐƠN HÀNG:\n").append(ordersData).append("\n\n");
                }
            } else {
                log.warn("No authentication token provided, cannot fetch database data");
            }

            String systemPrompt = """
                    Bạn là trợ lý AI thông minh trong hệ thống quản lý kho hàng (CMS).
                    Bạn có quyền truy cập dữ liệu THỰC TẾ từ database của hệ thống.
                    
                    QUY TẮC QUAN TRỌNG:
                    1. LUÔN sử dụng dữ liệu thực tế được cung cấp trong phần "DỮ LIỆU" bên dưới
                    2. Trả lời dựa trên SỐ LIỆU CỤ THỂ từ database, KHÔNG đoán mò
                    3. Nếu có dữ liệu, hãy trích dẫn số liệu chính xác
                    4. Nếu không có dữ liệu, hãy nói rõ "Không có dữ liệu trong hệ thống"
                    5. KHÔNG đưa ra hướng dẫn chung chung nếu đã có dữ liệu thực tế
                    
                    Nhiệm vụ của bạn:
                    
                    1. TRẢ LỜI VỀ SỐ LƯỢNG SẢN PHẨM:
                       - Nếu được hỏi "có bao nhiêu sản phẩm", hãy đọc số liệu từ "DỮ LIỆU TỒN KHO"
                       - Trả lời: "Hiện tại trong kho có [X] sản phẩm" (dựa trên số liệu thực tế)
                       - Liệt kê chi tiết nếu cần
                    
                    2. TÌM SẢN PHẨM: Sử dụng dữ liệu sản phẩm thực tế
                       - Liệt kê các sản phẩm phù hợp với từ khóa
                       - Hiển thị mã, tên, tồn kho, giá từ dữ liệu
                    
                    3. PHÂN TÍCH TỒN KHO: Sử dụng dữ liệu tồn kho thực tế
                       - Báo cáo tình trạng tồn kho hiện tại với số liệu cụ thể
                       - Cảnh báo sản phẩm sắp hết, hết hàng dựa trên dữ liệu
                       - Phân tích rủi ro và đề xuất giải pháp
                    
                    4. PHÂN TÍCH DOANH THU: Sử dụng dữ liệu đơn hàng thực tế
                       - Thống kê doanh thu, số đơn hàng với số liệu cụ thể
                       - Phân tích xu hướng bán hàng
                    
                    5. GỢI Ý MARKETING: Dựa trên dữ liệu thực tế
                       - Sản phẩm tồn kho cao → gợi ý giảm giá
                       - Sản phẩm bán chạy → gợi ý đẩy mạnh quảng cáo
                    
                    VÍ DỤ:
                    - Câu hỏi: "Có bao nhiêu sản phẩm trong kho?"
                      → Đọc "DỮ LIỆU TỒN KHO" → Trả lời: "Hiện tại trong kho có [X] sản phẩm (theo dữ liệu: Tổng số sản phẩm: X)"
                    
                    - Câu hỏi: "Thống kê tồn kho"
                      → Đọc "DỮ LIỆU TỒN KHO" → Trả lời với số liệu cụ thể: "Tổng số sản phẩm: X, Hết hàng: Y, Sắp hết: Z..."
                    
                    Trả lời ngắn gọn, rõ ràng, dùng tiếng Việt, LUÔN dựa trên dữ liệu thực tế.
                    """;

            String fullPrompt = systemPrompt;
            if (!contextData.isEmpty()) {
                fullPrompt += "\n\n" + contextData.toString();
            }
            fullPrompt += "\n\nCâu hỏi của người dùng: " + request.getMessage();
            
            log.info("Calling Gemini with prompt length: {}", fullPrompt.length());
            String text = geminiService.invokeGemini(fullPrompt);
            log.info("Gemini response received, length: {}", text != null ? text.length() : 0);
            return ApiResponse.ok(new AiChatResponse(text));
        } catch (Exception e) {
            log.error("Error in chat endpoint", e);
            throw e;
        }
    }

    /**
     * Trích xuất từ khóa tìm kiếm từ câu hỏi
     */
    private String extractKeyword(String message) {
        // Loại bỏ các từ không cần thiết
        String[] stopWords = {"tìm", "search", "sản phẩm", "product", "cho", "tôi", "bạn", 
                             "có", "nào", "là", "gì", "ở", "đâu", "?", "!", ".", ","};
        String cleaned = message;
        for (String stop : stopWords) {
            cleaned = cleaned.replaceAll("\\b" + stop + "\\b", " ").trim();
        }
        // Lấy từ dài nhất (có thể là tên sản phẩm)
        String[] words = cleaned.split("\\s+");
        String keyword = "";
        for (String word : words) {
            if (word.length() > keyword.length() && word.length() > 2) {
                keyword = word;
            }
        }
        return keyword;
    }

    @PostMapping("/product-description")
    public ApiResponse<ProductDescriptionResponse> generateDescription(
            @Valid @RequestBody ProductDescriptionRequest request) {
        String prompt = """
                Bạn là chuyên gia viết nội dung thương mại điện tử chuyên nghiệp, sử dụng tiếng Việt tự nhiên, dễ đọc.
                Hãy viết mô tả chi tiết cho sản phẩm có tên: "%s".
                
                Yêu cầu nội dung:
                1. "short": Mô tả ngắn gọn (80-150 từ)
                   - Nêu được nhóm sản phẩm, đối tượng sử dụng, 2–3 lợi ích chính.
                2. "seo": Mô tả chuẩn SEO (180-280 từ)
                   - Có chèn tự nhiên 2–3 lần tên sản phẩm
                   - Nhắc tới các từ khóa gợi ý như: chất lượng, chính hãng, bảo hành, giao hàng, giá tốt (nếu phù hợp)
                   - Đoạn văn mạch lạc, không liệt kê khô khan.
                3. "long": Mô tả chi tiết (300-600 từ)
                   - Chia nội dung thành các đoạn rõ ý: tổng quan, tính năng nổi bật, thông số/đặc điểm, trải nghiệm sử dụng, bảo hành – hậu mãi.
                   - Tập trung vào lợi ích thực tế cho người dùng trong bối cảnh kho hàng/bán lẻ.
                   - Viết dạng văn bản thuần, không dùng markdown, không bullet.
                4. "attributes": Mảng các thuộc tính gợi ý, dạng chuỗi ngắn, ví dụ:
                   - "chất liệu: ..."
                   - "kích thước: ..."
                   - "màu sắc: ..."
                   - "bảo hành: ..."
                   - "xuất xứ: ..."
                   - "trọng lượng: ..."
                
                Trả về JSON với format:
                {
                  "short": "...",
                  "seo": "...",
                  "long": "...",
                  "attributes": ["chất liệu: ...", "kích thước: ...", "màu sắc: ...", "bảo hành: ...", "xuất xứ: ..."]
                }
                
                Chỉ trả về JSON thuần túy, không thêm lời giải thích, không thêm markdown.
                """.formatted(request.getName());
        try {
            String text = geminiService.invokeGemini(prompt);
            return ApiResponse.ok(descriptionParser.parse(text));
        } catch (AiServiceException ex) {
            // Fallback mô tả đơn giản khi Gemini quá tải / hết quota
            String name = request.getName();
            String shortDesc =
                    "Sản phẩm " + name +
                            " là lựa chọn phù hợp cho nhu cầu sử dụng thực tế trong kho hàng và cửa hàng bán lẻ. " +
                            "Thiết kế đơn giản, dễ bố trí, giúp tối ưu quy trình nhập – xuất – lưu trữ, đồng thời mang lại trải nghiệm sử dụng thuận tiện cho nhân viên.";

            String seoDesc =
                    "Sản phẩm " + name +
                            " được thiết kế hướng tới sự bền bỉ, ổn định và dễ thao tác trong môi trường làm việc hàng ngày. " +
                            "Người dùng có thể ứng dụng trong nhiều kịch bản khác nhau như trưng bày, lưu kho, đóng gói hoặc giao nhận. " +
                            "Khi bổ sung đầy đủ thông tin về chất liệu, kích thước, tải trọng, bảo hành và xuất xứ, mô tả sản phẩm sẽ giúp khách hàng dễ dàng so sánh, lựa chọn và tin tưởng hơn khi đặt mua trực tuyến.";

            String longDesc =
                    "Sản phẩm " + name +
                            " phù hợp sử dụng trong hệ thống quản lý kho hàng, siêu thị mini, cửa hàng bán lẻ hoặc các đơn vị phân phối cần chuẩn hóa danh mục hàng hóa. " +
                            "Tùy vào cấu hình thực tế, sản phẩm có thể được làm từ nhiều loại chất liệu khác nhau (kim loại, nhựa cứng, gỗ, v.v.) nhằm đáp ứng các yêu cầu về độ bền và tính thẩm mỹ.\n\n" +
                            "Khi mô tả chi tiết, bạn nên bổ sung thêm các thông tin như: kích thước tổng thể, trọng lượng, màu sắc, tải trọng tối đa, tiêu chuẩn an toàn, phụ kiện đi kèm và điều kiện bảo quản. " +
                            "Ngoài ra, việc nêu rõ chính sách bảo hành, thời gian đổi trả, đơn vị cung cấp và xuất xứ sẽ giúp khách hàng yên tâm hơn trong quá trình lựa chọn.\n\n" +
                            "Để tăng hiệu quả bán hàng, có thể gợi ý thêm các tình huống sử dụng thực tế (ví dụ: kết hợp với các loại kệ, thùng, thiết bị khác trong kho) và lợi ích cụ thể mà sản phẩm mang lại cho người vận hành. " +
                            "Những thông tin này không chỉ hỗ trợ đội ngũ bán hàng tư vấn tốt hơn mà còn giúp hệ thống quản lý kho xây dựng dữ liệu sản phẩm rõ ràng, dễ tra cứu.";

            ProductDescriptionResponse fallback = ProductDescriptionResponse.builder()
                    .shortDescription(shortDesc)
                    .seoDescription(seoDesc)
                    .longDescription(longDesc)
                    .attributes(List.of(
                            "chất liệu: cập nhật theo thực tế",
                            "kích thước: cập nhật theo thực tế",
                            "bảo hành: cập nhật theo chính sách",
                            "xuất xứ: cập nhật theo thực tế"))
                    .build();

            return ApiResponse.ok(fallback);
        }
    }

    @PostMapping("/inventory-forecast")
    public ApiResponse<InventoryForecastResponse> forecast(
            @Valid @RequestBody InventoryForecastRequest request) {
        List<InventoryForecastRequest.ItemSummary> items = request.getItems();

        // Xây dựng danh sách sản phẩm với thông tin chi tiết để gửi cho Gemini
        StringBuilder itemsData = new StringBuilder();
        for (InventoryForecastRequest.ItemSummary item : items) {
            itemsData.append(String.format("- SKU: %s | Tên: %s | Tồn kho: %d",
                    item.getCode(), item.getName(), item.getQuantity()));
            if (item.getAvgDailySales() != null && item.getAvgDailySales() > 0) {
                double daysRemaining = item.getQuantity() / item.getAvgDailySales();
                itemsData.append(String.format(" | Bán TB/ngày: %.2f | Còn đủ: %.1f ngày",
                        item.getAvgDailySales(), daysRemaining));
            }
            itemsData.append("\n");
        }

        String prompt = """
                Bạn là chuyên gia quản trị kho và dự báo time-series chuyên nghiệp.
                Phân tích dữ liệu tồn kho và dự báo nhu cầu trong 7 ngày tới.

                Dữ liệu sản phẩm:
                %s

                Nhiệm vụ và FORMAT BẮT BUỘC:

                1. Xác định các SKU có nguy cơ thiếu hàng (itemsAtRisk):
                   - SKU có tồn kho ước tính < 7 ngày bán hoặc đã hết hàng.
                   - Với mỗi SKU, tính:
                     - daysRemaining: số ngày ước tính còn đủ hàng (có thể là số thực, ví dụ 3.5)
                     - recommendedPurchaseQty: số lượng đề xuất nhập thêm để đủ hàng ~21 ngày.

                2. Xác định các SKU tồn kho cao / dư hàng (overstockItems):
                   - SKU có tồn kho ước tính > 30 ngày bán.
                   - Với mỗi SKU, tính:
                     - daysOfStock: số ngày ước tính có thể bán hết lượng tồn.
                     - recommendation: gợi ý hành động (ví dụ: "Giảm giá nhẹ 10-15%% và đẩy khuyến mãi",
                       "Cân nhắc luân chuyển sang kho bán tốt hơn", ...).

                3. Tạo phần summary (tóm tắt):
                   - Tối đa 2-3 đoạn ngắn.
                   - Nêu tổng quan số SKU có nguy cơ thiếu, số SKU tồn cao, và hành động nên ưu tiên.

                HÃY TRẢ VỀ JSON THUẦN với format CHÍNH XÁC như sau
                (không thêm bất kỳ text nào bên ngoài JSON, không dùng markdown, không giải thích thêm):

                {
                  "itemsAtRisk": [
                    {
                      "code": "SKU001",
                      "name": "Tên sản phẩm",
                      "quantity": 10,
                      "daysRemaining": 3.5,
                      "recommendedPurchaseQty": 50
                    }
                  ],
                  "overstockItems": [
                    {
                      "code": "SKU002",
                      "name": "Tên sản phẩm B",
                      "quantity": 300,
                      "daysOfStock": 45.0,
                      "recommendation": "Giảm giá nhẹ 10-15%% và đẩy khuyến mãi trong 2 tuần tới."
                    }
                  ],
                  "summary": "Đoạn tóm tắt tiếng Việt rõ ràng, dễ hiểu về tình hình tồn kho."
                }

                NHẮC LẠI: Chỉ trả về JSON đúng format trên, không thêm bất kỳ nội dung nào khác.
                """.formatted(itemsData.toString());

        try {
            String text = geminiService.invokeGemini(prompt);
            // Parse JSON trả về từ Gemini về cấu trúc InventoryForecastResponse
            InventoryForecastResponse parsed = objectMapper.readValue(text, InventoryForecastResponse.class);
            return ApiResponse.ok(parsed);
        } catch (Exception ex) {
            // Bất kỳ lỗi nào (Gemini, JSON parse, ...) đều dùng fallback rule-based
            log.warn("Gemini inventory forecast failed, returning fallback structured response: {}", ex.getMessage());
            InventoryForecastResponse fallback = buildFallbackInventoryRecommendation(items);
            return ApiResponse.ok(fallback);
        }
    }

    private InventoryForecastResponse buildFallbackInventoryRecommendation(List<InventoryForecastRequest.ItemSummary> items) {
        if (items == null || items.isEmpty()) {
            return InventoryForecastResponse.builder()
                    .itemsAtRisk(List.of())
                    .overstockItems(List.of())
                    .summary("Không có dữ liệu tồn kho để phân tích.")
                    .build();
        }

        var outOfStock = items.stream()
                .filter(i -> i.getQuantity() != null && i.getQuantity() <= 0)
                .limit(10)
                .collect(Collectors.toList());

        var withSales = items.stream()
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0 && i.getAvgDailySales() != null && i.getAvgDailySales() > 0)
                .collect(Collectors.toList());

        var lowByDays = withSales.stream()
                .filter(i -> (i.getQuantity() / i.getAvgDailySales()) < 7.0)
                .sorted((a, b) -> Double.compare(a.getQuantity() / a.getAvgDailySales(), b.getQuantity() / b.getAvgDailySales()))
                .limit(10)
                .collect(Collectors.toList());

        var overStock = withSales.stream()
                .filter(i -> (i.getQuantity() / i.getAvgDailySales()) > 30.0)
                .sorted((a, b) -> Double.compare((b.getQuantity() / b.getAvgDailySales()), (a.getQuantity() / a.getAvgDailySales())))
                .limit(10)
                .collect(Collectors.toList());

        // Map sang cấu trúc DTO mới
        List<InventoryForecastResponse.ItemAtRisk> atRiskDtos = lowByDays.stream()
                .map(i -> {
                    double days = i.getQuantity() / i.getAvgDailySales();
                    long need = (long) Math.ceil(i.getAvgDailySales() * 21.0 - i.getQuantity()); // mục tiêu ~21 ngày tồn
                    if (need < 0) need = 0;
                    return InventoryForecastResponse.ItemAtRisk.builder()
                            .code(i.getCode())
                            .name(i.getName())
                            .quantity(i.getQuantity())
                            .daysRemaining(days)
                            .recommendedPurchaseQty(need)
                            .build();
                })
                .collect(Collectors.toList());

        // Hết hàng cũng được xem là "at risk" với daysRemaining = 0 và gợi ý nhập
        atRiskDtos.addAll(
                outOfStock.stream()
                        .map(i -> InventoryForecastResponse.ItemAtRisk.builder()
                                .code(i.getCode())
                                .name(i.getName())
                                .quantity(i.getQuantity())
                                .daysRemaining(0.0)
                                .recommendedPurchaseQty(50L) // giá trị mặc định, người dùng sẽ tự điều chỉnh
                                .build())
                        .collect(Collectors.toList())
        );

        List<InventoryForecastResponse.OverstockItem> overStockDtos = overStock.stream()
                .map(i -> {
                    double days = i.getQuantity() / i.getAvgDailySales();
                    String recommendation = "Cân nhắc khuyến mãi/xả tồn hoặc luân chuyển sang kho bán chạy hơn trong 2-4 tuần tới.";
                    return InventoryForecastResponse.OverstockItem.builder()
                            .code(i.getCode())
                            .name(i.getName())
                            .quantity(i.getQuantity())
                            .daysOfStock(days)
                            .recommendation(recommendation)
                            .build();
                })
                .collect(Collectors.toList());

        String summary;
        int atRiskCount = atRiskDtos.size();
        int overStockCount = overStockDtos.size();

        if (atRiskCount == 0 && overStockCount == 0) {
            summary = "Tồn kho hiện tại nhìn chung ở mức an toàn. Không có SKU nào sắp hết hàng hoặc tồn kho quá cao theo dữ liệu 7 ngày gần nhất.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Báo cáo nhanh tồn kho dựa trên dữ liệu bán hàng và tồn kho gần đây: ");
            if (atRiskCount > 0) {
                sb.append("Có ").append(atRiskCount)
                        .append(" SKU có nguy cơ thiếu hàng trong 7 ngày tới hoặc đã hết hàng, nên ưu tiên đặt nhập sớm. ");
            }
            if (overStockCount > 0) {
                sb.append("Có ").append(overStockCount)
                        .append(" SKU đang tồn kho cao (ước tính > 30 ngày bán), cần xem xét khuyến mãi hoặc luân chuyển để giảm tồn.");
            }
            summary = sb.toString();
        }

        return InventoryForecastResponse.builder()
                .itemsAtRisk(atRiskDtos)
                .overstockItems(overStockDtos)
                .summary(summary)
                .build();
    }
}
