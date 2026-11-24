package vn.edu.vlu.restaurant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The React pages are served as index.html, while /api keeps its own answers. */
class SpaControllerTest extends ApiTestSupport {

    @Test
    @DisplayName("Opening a React page directly returns the app's index.html")
    void pagesForwardToIndex() throws Exception {
        mockMvc.perform(get("/")).andExpect(forwardedUrl("/index.html"));
        mockMvc.perform(get("/floor")).andExpect(forwardedUrl("/index.html"));
        mockMvc.perform(get("/order/5")).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @DisplayName("Unknown API paths are not turned into the React page")
    void apiPathsAreNotForwarded() throws Exception {
        mockMvc.perform(get("/api/tables")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api")).andExpect(forwardedUrl(null));
    }
}
