package com.managination.numa.didserver.config;

import io.github.springwolf.asyncapi.v3.model.channel.ChannelObject;
import io.github.springwolf.asyncapi.v3.model.channel.ChannelParameter;
import io.github.springwolf.core.asyncapi.AsyncApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AsyncApiCustomizerConfig {

    @Bean
    public AsyncApiCustomizer asyncApiCustomizer() {
        return asyncApi -> {
            var channels = asyncApi.getChannels();
            if (channels != null) {
                for (var entry : channels.entrySet()) {
                    if (entry.getValue() instanceof ChannelObject channel) {
                        channel.setParameters(Map.of(
                            "sessionId", ChannelParameter.builder()
                                .description("The unique session UUID created via POST /sessions")
                                .build()
                        ));
                    }
                }
            }
        };
    }
}
