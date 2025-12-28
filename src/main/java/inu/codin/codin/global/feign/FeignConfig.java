package inu.codin.codin.global.feign;

import feign.RequestInterceptor;
import inu.codin.codin.global.auth.util.SecurityUtils;
import inu.codin.codin.global.common.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String jwtToken = getJwtToken();
            String txId = MDC.get(LogConstant.MDC_TX_ID);

            if (jwtToken != null && !jwtToken.isEmpty()) {
                requestTemplate.header("Authorization", "Bearer " + jwtToken);
            }
            if (txId != null) {
                requestTemplate.header(LogConstant.HEADER_TX_ID, txId);
            }
        };
    }

    private String getJwtToken() {
        return SecurityUtils.getUserToken();
    }
}