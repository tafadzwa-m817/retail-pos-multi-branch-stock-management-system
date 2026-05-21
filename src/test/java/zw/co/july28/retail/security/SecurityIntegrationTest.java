package zw.co.july28.retail.security;

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
@DisplayName("Security integration tests — auth enforcement & role-based access")
class SecurityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String adminToken;
    private String cashierToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken   = login("admin@july28retail.co.zw",   "admin123");
        cashierToken = login("cashier@july28retail.co.zw", "cashier123");
        managerToken = login("manager@july28retail.co.zw", "manager123");
    }

    private String login(String email, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    // ── Public endpoints ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/store-settings: publicly accessible without token")
    void storeSettings_publicEndpoint_noAuthRequired() throws Exception {
        mockMvc.perform(get("/api/store-settings"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/login: publicly accessible — bad credentials return 401, not 403")
    void login_publicEndpoint_accessible() throws Exception {
        // "x" fails @Email validation → 400; use a valid-format email to hit the auth layer
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized()); // endpoint reachable, credentials rejected = 401
    }

    // ── Unauthenticated access ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/products: returns 401 when no token provided")
    void products_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/sales: returns 401 when no token")
    void sales_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/dashboard: returns 401 when no token")
    void dashboard_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ── Role-based access: CASHIER restrictions ───────────────────────────────

    @Test
    @DisplayName("GET /api/users: CASHIER receives 403 (ADMIN+ only)")
    void getUsers_asCashier_returns403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/branches: CASHIER receives 403 (ADMIN+ only)")
    void createBranch_asCashier_returns403() throws Exception {
        mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Branch\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/products/1: CASHIER receives 403 (ADMIN+ only)")
    void deleteProduct_asCashier_returns403() throws Exception {
        mockMvc.perform(delete("/api/products/1")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/audit-logs: CASHIER receives 403 (ADMIN+ only)")
    void auditLogs_asCashier_returns403() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());
    }

    // ── Authenticated access succeeds ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/products: CASHIER can read product catalog")
    void getProducts_asCashier_returns200() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/branches: BRANCH_MANAGER can list branches")
    void getBranches_asManager_returns200() throws Exception {
        mockMvc.perform(get("/api/branches")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/users: SUPER_ADMIN can list all users")
    void getUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/dashboard: BRANCH_MANAGER can access dashboard")
    void dashboard_asManager_returns200() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveBranches").exists());
    }

    @Test
    @DisplayName("Invalid JWT token returns 401")
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
