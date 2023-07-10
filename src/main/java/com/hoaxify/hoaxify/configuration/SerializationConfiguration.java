package com.hoaxify.hoaxify.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import java.io.IOException;

@Configuration
public class SerializationConfiguration {

    @Bean
    public Module springDataPageModule() {
        JsonSerializer<Page> pageSerializer = new JsonSerializer<>() {
            @Override
            public void serialize(Page page, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
                generator.writeStartObject();
                generator.writeNumberField("numberOfElements", page.getNumberOfElements());
                generator.writeNumberField("totalElements", page.getTotalElements());
                generator.writeNumberField("totalPages", page.getTotalPages());
                generator.writeNumberField("number", page.getNumber());
                generator.writeNumberField("size", page.getSize());

                generator.writeBooleanField("first", page.isFirst());
                generator.writeBooleanField("last", page.isLast());
                generator.writeBooleanField("next", page.hasNext());
                generator.writeBooleanField("previous", page.hasPrevious());

                generator.writeFieldName("content");
                serializerProvider.defaultSerializeValue(page.getContent(), generator);
                generator.writeEndObject();
            }
        };
        return new SimpleModule().addSerializer(Page.class, pageSerializer);
    }
}
