package com.dumy.config;

import com.dumy.module.session.constant.SessionConstants;
import com.dumy.module.session.web.CurrentUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dumy API")
                        .description("Dumy skeleton API documentation")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(SessionConstants.SESSION_SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(SessionConstants.SESSION_TOKEN_HEADER)
                                .description("Session token issued by POST /api/auth/login")));
    }

    @Bean
    public GlobalOpenApiCustomizer sessionAuthOpenApiCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (SessionConstants.ALLOWLIST_PREFIXES.stream().anyMatch(path::startsWith)) {
                return;
            }
            pathItem.readOperations().forEach(operation ->
                    operation.addSecurityItem(new SecurityRequirement().addList(SessionConstants.SESSION_SECURITY_SCHEME)));
        });
    }
}
