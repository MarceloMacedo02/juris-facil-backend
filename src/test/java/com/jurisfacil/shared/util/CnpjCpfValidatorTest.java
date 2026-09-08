package com.jurisfacil.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CnpjCpfValidatorTest {

    @Test
    void acceptsValidCpfAndCnpjWithOrWithoutMask() {
        assertThat(CnpjCpfValidator.isValid("529.982.247-25")).isTrue();
        assertThat(CnpjCpfValidator.isValid("52998224725")).isTrue();
        assertThat(CnpjCpfValidator.isValid("04.252.011/0001-10")).isTrue();
        assertThat(CnpjCpfValidator.isValid("04252011000110")).isTrue();
    }

    @Test
    void rejectsInvalidCheckDigitsAndRepeatedDigits() {
        assertThat(CnpjCpfValidator.isValid("529.982.247-26")).isFalse();
        assertThat(CnpjCpfValidator.isValid("04.252.011/0001-11")).isFalse();
        assertThat(CnpjCpfValidator.isValid("11111111111")).isFalse();
        assertThat(CnpjCpfValidator.isValid("123")).isFalse();
    }
}
