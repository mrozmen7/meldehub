package ch.meldehub.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaseController {

    @GetMapping("/api/cases/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/api/cases/hello")
    public String hello() {
        return "Merhaba from gorev-1";
    }
}
