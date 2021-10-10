package cc.ikey.playground.simpleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@SpringBootApplication
public class SimpleAppApplication {
    private final String getUUIDUrl = "https://httpbin.org/uuid";
    private final RestTemplate restTemplate;

    public SimpleAppApplication(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(SimpleAppApplication.class, args);
    }

    @GetMapping("uuid")
    public String getUUID() {
        ResponseEntity<UUIDResponse> response = restTemplate.getForEntity(getUUIDUrl, UUIDResponse.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody().getUuid();
        }
        return "获取失败";
    }

    public static class UUIDResponse {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
}
