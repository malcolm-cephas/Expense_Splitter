package com.malcolm.expensesplitter.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to serve the React Single Page Application (SPA).
 * It ensures that any non-API route is served by index.html,
 * allowing React Router to handle client-side routing.
 */
@Controller
public class WebFrontendController {

    @RequestMapping(value = { "/", "/{path:[^\\.]*}" })
    public String index() {
        return "forward:/index.html";
    }
}
