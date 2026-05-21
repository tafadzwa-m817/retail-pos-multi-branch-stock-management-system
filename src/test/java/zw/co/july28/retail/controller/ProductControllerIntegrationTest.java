package zw.co.july28.retail.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("ProductController integration tests")
class ProductControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String adminToken;
    private String cashierToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken   = login("admin@july28retail.co.zw",   "admin123");
        cashierToken = login("cashier@july28retail.co.zw", "cashier123");
    }

    private String login(String email, String pass) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", pass))))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    @Test
    @DisplayName("GET /api/products: returns list of active products")
    void getProducts_authenticated_returnsActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(10)); // 10 seeded
    }

    @Test
    @DisplayName("GET /api/products/search?q=coke: returns matching products")
    void searchProducts_byName_returnsMatches() throws Exception {
        mockMvc.perform(get("/api/products/search?q=Coca")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Coca-Cola 500ml"));
    }

    @Test
    @DisplayName("GET /api/products/barcode/{barcode}: returns product for known barcode")
    void getByBarcode_knownBarcode_returnsProduct() throws Exception {
        mockMvc.perform(get("/api/products/barcode/6001234567895")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Coca-Cola 500ml"));
    }

    @Test
    @DisplayName("GET /api/products/barcode/{barcode}: returns 404 for unknown barcode")
    void getByBarcode_unknownBarcode_returns404() throws Exception {
        mockMvc.perform(get("/api/products/barcode/NONEXISTENT")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/products: ADMIN can create product")
    void createProduct_asAdmin_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Test Product", "sku", "TEST-SKU-999",
                "categoryId", 1, "costPrice", 5.00, "sellingPrice", 10.00, "reorderLevel", 5
        );

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Product"))
                .andExpect(jsonPath("$.data.sku").value("TEST-SKU-999"));
    }

    @Test
    @DisplayName("POST /api/products: duplicate SKU returns 400")
    void createProduct_duplicateSku_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Duplicate", "sku", "COKE-500ML", // existing SKU
                "categoryId", 1, "costPrice", 1.0, "sellingPrice", 2.0, "reorderLevel", 5
        );

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
