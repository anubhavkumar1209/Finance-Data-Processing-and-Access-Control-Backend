package com.financedashboard.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    @Valid
    private Rule auth = new Rule(5, 60);

    @Valid
    private Rule api = new Rule(120, 60);

    @Getter
    @Setter
    public static class Rule {

        @Min(1)
        private int maxRequests;

        @Min(1)
        private long windowSeconds;

        public Rule() {
        }

        public Rule(int maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
