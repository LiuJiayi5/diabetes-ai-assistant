package com.diabetes.assistant.modules.dify.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class DifyHttpClientConfig {

    private static final Duration DIFY_TIMEOUT = Duration.ofSeconds(120);

    @Bean
    public RestClient.Builder restClientBuilder() {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(DIFY_TIMEOUT)
                        .withReadTimeout(DIFY_TIMEOUT)
        );
        return RestClient.builder().requestFactory(requestFactory);
    }
}
