
package com.example.auth.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 追加
        mapper.registerModule(new Jdk8Module());
        SimpleModule module = new SimpleModule();
        module.addSerializer(ByteArray.class, new ByteArraySerializer());
        mapper.registerModule(module);
        return mapper;
    }

    public static class ByteArraySerializer extends JsonSerializer<ByteArray> {
        @Override
        public void serialize(ByteArray value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getBase64Url());
        }
    }
}
