package mw.nwra.ewaterpermit.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;

public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {
	ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, Object> customerInfo) {

		String customerInfoJson = null;
		try {
			customerInfoJson = objectMapper.writeValueAsString(customerInfo);
		} catch (final JsonProcessingException e) {
			System.out.println("JSON writing error " + e.getMessage());
		}

		return customerInfoJson;
	}

	@Override
	public Map<String, Object> convertToEntityAttribute(String customerInfoJSON) {

		Map<String, Object> customerInfo = null;
		try {
			customerInfo = objectMapper.readValue(customerInfoJSON, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (final IOException e) {
			System.out.println("JSON writing error " + e.getMessage());
		}

		return customerInfo;
	}
}
