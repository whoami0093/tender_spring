package com.example.app.config

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaController {
    @GetMapping("/admin/{path:[^\\.]*}", "/admin")
    fun spa(): String = "forward:/admin/index.html"
}
