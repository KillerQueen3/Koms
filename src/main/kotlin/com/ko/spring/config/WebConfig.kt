package com.ko.spring.config

import org.apache.tomcat.util.descriptor.web.SecurityCollection
import org.apache.tomcat.util.descriptor.web.SecurityConstraint
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.multipart.commons.CommonsMultipartResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/image/**").addResourceLocations("file:./image/")
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/")
        registry.addResourceHandler("/audio/**").addResourceLocations("file:./audio/")
    }

    @Bean
    fun getResolver(): CommonsMultipartResolver {
        val resolver = CommonsMultipartResolver()
        resolver.setMaxUploadSize(10485760)
        return resolver
    }

    @Bean
    fun webServerFactory(): ConfigurableServletWebServerFactory? {
        val factory = TomcatServletWebServerFactory()
        factory.addConnectorCustomizers(TomcatConnectorCustomizer { connector ->
            connector.setProperty(
                "relaxedQueryChars",
                "|{}[]\\"
            )
        })
        return factory
    }
}