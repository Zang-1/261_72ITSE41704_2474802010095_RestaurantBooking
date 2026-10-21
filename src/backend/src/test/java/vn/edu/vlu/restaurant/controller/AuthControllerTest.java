package vn.edu.vlu.restaurant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

/** UC-01 Log in and log out, and server-side role checks (FR-01, NFR-10). */
class AuthControllerTest extends ApiTestSupport {

    @Test
    @DisplayName("TC-RES-01 Login with a valid account opens a session with the user's role")
    void loginWithValidAccount() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "manager", "password", "Manager@2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manager"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("TC-RES-02 Wrong password gives a generic message that does not say which field was wrong")
    void loginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "waiter", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("TC-RES-03 Empty username is rejected with a message next to the field")
    void loginWithEmptyUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "", "password", "Waiter@2026"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").value("Username is required"));
    }

    @Test
    @DisplayName("TC-RES-04 An API call without logging in is refused")
    void requestWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/tables"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please log in"));
    }

    @Test
    @WithMockUser(roles = "WAITER_HOST")
    @DisplayName("TC-RES-05 A Waiter/Host cannot create a table (Manager only)")
    void waiterCannotCreateTable() throws Exception {
        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("number", 30, "capacity", 4, "sectionId", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    @DisplayName("Kitchen Staff cannot open the reservation list")
    void kitchenStaffCannotSeeReservations() throws Exception {
        mockMvc.perform(get("/api/reservations").param("date", tomorrow().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Logout ends the session")
    void logoutEndsSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "waiter", "password", "Waiter@2026"))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
}
