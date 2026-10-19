package com.bukang.common.global.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

// TextEncryptor가 있어야 만들 수 있으므로 CryptoConfig와 같은 조건으로 등록한다
@Component
@Converter
@ConditionalOnProperty(prefix = "crypto", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {
	private final TextEncryptor textEncryptor;

	@Override
	public String convertToDatabaseColumn(String attribute) {
		if (attribute == null) {
			return null;
		}
		return textEncryptor.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		return textEncryptor.decrypt(dbData);
	}
}
