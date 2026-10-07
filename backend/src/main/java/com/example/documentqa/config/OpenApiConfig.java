package com.example.documentqa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI documentQaOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Document QA RAG API")
                .description("""
                    Upload a PDF or UTF-8 TXT file, ask questions about its contents, \
                    and inspect answers with retrieved sources and saved history.

                    **Processing is synchronous**: upload responses return after parsing, \
                    embedding, and chunk storage complete.

                    **Grounding**: the model answers only from retrieved excerpts. When retrieval \
                    is insufficient, a fixed refusal is returned. Sources describe the retrieved \
                    context, not independently verified claims.
                    """)
                .version("1.0.0")
                .license(new License().name("Local learning project")));
    }
}
