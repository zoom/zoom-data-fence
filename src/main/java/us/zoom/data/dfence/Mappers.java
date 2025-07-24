package us.zoom.data.dfence;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class Mappers {
    public static ObjectMapper yamlKebabObjectMapper() {
        return yamlObjectMapper().setPropertyNamingStrategy(new PropertyNamingStrategies.KebabCaseStrategy());
    }

    public static ObjectMapper jsonKebabObjectMapper() {
        return jsonObjectMapper().setPropertyNamingStrategy(new PropertyNamingStrategies.KebabCaseStrategy());
    }

    public static ObjectMapper jsonObjectMapper() {
        return new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public static ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)).configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                true).configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }
}
