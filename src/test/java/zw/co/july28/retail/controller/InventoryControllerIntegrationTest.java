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
@DisplayName("InventoryController integration tests")
class InventoryControllerIntegrationTest {

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
    @DisplayName("GET /api/inventory: returns all inventory records")
    void getAllInventory_returns200WithRecords() throws Exception {
        mockMvc.perform(get("/api/inventory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(30)); // 10 products × 3 branches
    }

    @Test
    @DisplayName("GET /api/inventory/branch/1: returns inventory for branch 1")
    void getByBranch_returns200WithFilteredRecords() throws Exception {
        mockMvc.perform(get("/api/inventory/branch/1")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    @DisplayName("GET /api/inventory/low-stock: returns items below reorder level")
    void getLowStock_returnsCorrectItems() throws Exception {
        mockMvc.perform(get("/api/inventory/low-stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].lowStock").value(true));
    }

    @Test
    @DisplayName("POST /api/inventory/adjust: MANAGER can adjust stock")
    void adjustStock_asManager_returns200() throws Exception {
        String managerToken = login("manager@july28retail.co.zw", "manager123");
        Map<String, Object> body = Map.of("productId", 1, "branchId", 1, "quantity", 999, "reason", "Stocktake");

        mockMvc.perform(post("/api/inventory/adjust")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(999));
    }

    @Test
    @DisplayName("POST /api/inventory/adjust: CASHIER receives 403")
    void adjustStock_asCashier_returns403() throws Exception {
        Map<String, Object> body = Map.of("productId", 1, "branchId", 1, "quantity", 50);

        mockMvc.perform(post("/api/inventory/adjust")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }
}
