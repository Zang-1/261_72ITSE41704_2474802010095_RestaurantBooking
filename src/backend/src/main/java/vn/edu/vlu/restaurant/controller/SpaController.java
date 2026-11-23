package vn.edu.vlu.restaurant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * When the built React app is served by Spring Boot (start.bat), a browser that opens or refreshes
 * /floor or /order/5 must get index.html; React Router then shows the page. Paths with a dot are
 * files (JS, CSS, icons) and paths under /api belong to the REST controllers, so neither is matched.
 */
@Controller
public class SpaController {

    @GetMapping({"/", "/{page:(?!api$)[^.]*}", "/{page:(?!api$)[^.]*}/{id:[^.]*}"})
    public String index() {
        return "forward:/index.html";
    }
}
