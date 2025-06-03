package com.doma.artserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        
        // UTF-8 인코딩 설정
        for (int i = 0; i < restTemplate.getMessageConverters().size(); i++) {
            Object converter = restTemplate.getMessageConverters().get(i);
            if (converter instanceof StringHttpMessageConverter) {
                StringHttpMessageConverter stringConverter = (StringHttpMessageConverter) converter;
                stringConverter.setDefaultCharset(StandardCharsets.UTF_8);
                
                List<MediaType> mediaTypes = new ArrayList<>();
                mediaTypes.add(new MediaType("text", "plain", StandardCharsets.UTF_8));
                mediaTypes.add(new MediaType("text", "xml", StandardCharsets.UTF_8));
                mediaTypes.add(new MediaType("application", "xml", StandardCharsets.UTF_8));
                
                stringConverter.setSupportedMediaTypes(mediaTypes);
            }
        }
        
        return restTemplate;
    }
}
