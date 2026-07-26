package com.projecthelper.seed;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {
    private boolean enabled;
    private String defaultPassword = "ProjectHelper@123";
}
