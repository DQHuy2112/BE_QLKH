# AI Reports Flow (Gemini-powered)

This document summarises how each section in **📊 Báo cáo AI Thông minh** (frontend `src/app/(dashboard)/reports/page.tsx`) interacts with the backend AI service and Gemini.

## 1. Shared Concepts

| Layer | Details |
| --- | --- |
| Frontend | React page `reports/page.tsx` renders 5 tabs and lazy-loads data via functions from `src/services/ai.service.ts`. |
| Gateway/API | Frontend calls `/api/ai/reports/*` on the backend. Requests include the JWT (Authorization header). |
| AI Service | Spring Boot module `BE_QLKH/ai-service` exposes `AiReportController`. Each handler orchestrates data collection + Gemini prompt building. |
| Gemini | Accessed via `GeminiService.invokeGemini(prompt)` which sends prompts to `https://generativelanguage.googleapis.com/v1beta/...` using `GEMINI_API_KEY` (`application.yaml` → `gemini.api-key`). |

`GeminiService` is now the single entry point for calling Gemini (same as the OCR “Đọc ảnh bằng AI” flow).

## 2. Smart Inventory Alerts (`🔔 Cảnh báo tồn kho`)

1. **Frontend** calls `getSmartInventoryAlerts()` from `ai.service.ts`.  
2. **Backend** `SmartInventoryAlertService.analyzeInventoryAlerts(token)`:
   - Fetches stocks/products/exports from internal APIs via `WebClient`.
   - Computes stock summaries + severity buckets.
   - Builds prompt summarising critical/warning/info alerts and calls `geminiService.invokeGemini`.
   - Returns structured alerts + AI summary.
3. **Frontend** renders cards, summary, recommendations.

## 3. Demand Forecast (`🔮 Dự đoán nhu cầu`)

1. Tab loads `getDemandForecast()`/`getProductDemandForecast()` from `ai.service.ts`.
2. `DemandForecastingService` collects sell-through history and stock level, calculates averages.
3. Builds prompt describing days remaining, optimal stock level, etc., sends to Gemini for explanations.
4. Returns structured forecasts and AI-generated reasoning which the UI displays (plus per-product component `ProductDemandForecastReport`).

## 4. Sales Insights (`💰 Phân tích bán hàng`)

1. Frontend invokes `getSalesInsights(days)` (default 30).
2. `SalesInsightService.analyzeSalesInsights`:
   - Fetches exports + products.
   - Calculates revenue trend, top products, declining items, best-selling hours.
   - Creates prompt summarising metrics, requests Gemini to produce narrative analysis & recommendations.
3. UI shows visual sections (trend cards, list, AI summary).

## 5. Inventory Turnover (`🔄 Chu kỳ tồn kho`)

1. `getInventoryTurnover(periodDays)` called from tab.
2. `InventoryTurnoverService.analyzeInventoryTurnover`:
   - Aggregates stock movement, dead stock, overstocked items.
   - Prompt summarises KPIs → Gemini returns insight block.
3. Rendering includes overall turnover rate, warning cards, AI bullet list.

## 6. Stock Optimization (`🎯 Tối ưu kho`)

1. `getStockOptimization()` fetches data.
2. `StockOptimizationService.optimizeStock`:
   - Pre-computes optimal min/max stock per product and warehouse recommendations.
   - Builds prompt with highlighted items; Gemini produces concise action list & conclusion.
3. UI displays AI summary plus detailed product cards.

## 7. Supporting Docs / Components

- **`GeminiService`** (`BE_QLKH/ai-service/.../service/GeminiService.java`) contains the reusable WebClient + prompt invocation shared by OCR + all reports.
- **`ai.service.ts`** provides typed fetchers for frontend components.
- **`FE_QLKH/src/components/ai/*`** include shared UI for AI features.

## 8. Environment Variables

| Variable | Location | Description |
| --- | --- | --- |
| `GEMINI_API_KEY` | `docker-compose.yml`, `ai-service/src/main/resources/application.yaml` | API key used by `GeminiService`. Required for both OCR and AI reports. |
| `NEXT_PUBLIC_API_BASE_URL` | `FE_QLKH/.env` (set per deployment) | Base URL for API gateway; FE routes all `/api/ai/...` requests through this. |

Ensure `GEMINI_API_KEY` is set in the runtime environment (Docker stack, local dev `.env`) so both OCR and report tabs function identically.

