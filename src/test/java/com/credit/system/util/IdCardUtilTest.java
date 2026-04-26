package com.credit.system.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IdCardUtil 工具类测试")
class IdCardUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "110101199001011234",
            "320102198503176715",
            "440305199212205530"
    })
    @DisplayName("有效身份证号应通过校验")
    void shouldValidateValidIdCard(String idCard) {
        assertThat(IdCardUtil.isValid(idCard)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "11010119900101123Y",
            "abcd",
            ""
    })
    @DisplayName("无效身份证号应返回false")
    void shouldRejectInvalidIdCard(String idCard) {
        assertThat(IdCardUtil.isValid(idCard)).isFalse();
    }

    @Test
    @DisplayName("null输入应返回false")
    void shouldReturnFalseForNull() {
        assertThat(IdCardUtil.isValid(null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "110101199001011234, 36",
            "320102198503176715, 41",
            "440305201212205530, 13"
    })
    @DisplayName("应正确计算年龄")
    void shouldCalculateAge(String idCard, int expectedAge) {
        assertThat(IdCardUtil.calculateAge(idCard)).isEqualTo(expectedAge);
    }

    @Test
    @DisplayName("无效身份证号计算年龄应抛出异常")
    void shouldThrowExceptionForInvalidIdCard() {
        assertThatThrownBy(() -> IdCardUtil.calculateAge("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效");
    }

    @ParameterizedTest
    @CsvSource({
            "110101199001011234, 男",
            "440305201212205530, 男",
            "320102198503176715, 男"
    })
    @DisplayName("应正确提取性别")
    void shouldExtractGender(String idCard, String expectedGender) {
        assertThat(IdCardUtil.extractGender(idCard)).isEqualTo(expectedGender);
    }

    @ParameterizedTest
    @CsvSource({
            "110101199001011234, 1990-01-01",
            "440305201212205530, 2012-12-20"
    })
    @DisplayName("应正确提取出生日期")
    void shouldExtractBirthDate(String idCard, String expectedBirthDate) {
        assertThat(IdCardUtil.extractBirthDate(idCard)).isEqualTo(expectedBirthDate);
    }
}
