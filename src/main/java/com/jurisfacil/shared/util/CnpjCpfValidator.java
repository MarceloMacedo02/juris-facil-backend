package com.jurisfacil.shared.util;

import java.util.stream.IntStream;

import com.jurisfacil.shared.exception.AbstractBusinessException;
import com.jurisfacil.shared.exception.ErrorCode;

public final class CnpjCpfValidator {

    private CnpjCpfValidator() {
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return isCpfValid(digits);
        }
        return digits.length() == 14 && isCnpjValid(digits);
    }

    public static void assertValid(String value) {
        if (!isValid(value)) {
            throw new InvalidDocumentException();
        }
    }

    public static String normalize(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private static boolean isCpfValid(String value) {
        if (value.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(value.substring(0, 9), 10);
        int second = checkDigit(value.substring(0, 9) + first, 11);
        return value.charAt(9) - '0' == first && value.charAt(10) - '0' == second;
    }

    private static boolean isCnpjValid(String value) {
        if (value.chars().distinct().count() == 1) {
            return false;
        }
        int first = cnpjCheckDigit(value.substring(0, 12));
        int second = cnpjCheckDigit(value.substring(0, 12) + first);
        return value.charAt(12) - '0' == first && value.charAt(13) - '0' == second;
    }

    private static int checkDigit(String value, int factor) {
        int sum = IntStream.range(0, value.length())
                .map(index -> (value.charAt(index) - '0') * (factor - index))
                .sum();
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int cnpjCheckDigit(String value) {
        int factor = value.length() - 7;
        int sum = 0;
        for (int index = 0; index < value.length(); index++) {
            sum += (value.charAt(index) - '0') * factor--;
            if (factor < 2) {
                factor = 9;
            }
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    public static final class InvalidDocumentException extends AbstractBusinessException {
        public InvalidDocumentException() {
            super(ErrorCode.VALIDATION_ERROR.name(), "CPF/CNPJ has invalid check digits.");
        }
    }
}
