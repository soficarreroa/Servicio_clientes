package com.empresa.pedidos.esb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Crea el cliente HTTP que apunta al ESB, con tiempos máximos de conexión y lectura. */
@Configuration
public class EsbClientConfig {

    @Bean
    public RestClient esbRestClient(RestClient.Builder builder, EsbProperties propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) propiedades.connectTimeout().toMillis());
        fabrica.setReadTimeout((int) propiedades.readTimeout().toMillis());

        return builder
                .baseUrl(propiedades.baseUrl())
                .requestFactory(fabrica)
                .build();
    }
}
