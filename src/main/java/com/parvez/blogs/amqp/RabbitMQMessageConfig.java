package com.parvez.blogs.amqp;

import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQMessageConfig {

    @Bean
    public JacksonJsonMessageConverter jackson2JsonMessageConverter() {
        DefaultJacksonJavaTypeMapper defaultJacksonJavaTypeMapper = new DefaultJacksonJavaTypeMapper();

        defaultJacksonJavaTypeMapper.setTrustedPackages("com.parvez.blogs.dto");

        JacksonJsonMessageConverter jsonMessageConverter = new JacksonJsonMessageConverter();
        jsonMessageConverter.setJavaTypeMapper(defaultJacksonJavaTypeMapper);
        return jsonMessageConverter;
    }
}
