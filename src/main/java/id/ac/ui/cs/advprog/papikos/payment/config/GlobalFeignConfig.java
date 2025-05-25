package id.ac.ui.cs.advprog.papikos.payment.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalFeignConfig {

    @Value("${internal.token.secret}")
    private String internalTokenSecret;

    @Bean
    public RequestInterceptor propertyBasedRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                if (internalTokenSecret != null && !internalTokenSecret.isEmpty()) {
                    template.header("X-Internal-Token", internalTokenSecret);
                }
            }
        };
    }
}
