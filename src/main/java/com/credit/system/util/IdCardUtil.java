package com.credit.system.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class IdCardUtil {

    private static final Pattern ID_CARD_PATTERN =
        Pattern.compile("(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)");

    public static int calculateAge(String idCard) {
        if (idCard == null || !ID_CARD_PATTERN.matcher(idCard).matches()) {
            throw new IllegalArgumentException("无效的身份证号");
        }
        String birthDateStr = idCard.substring(6, 14);
        LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public static boolean isValid(String idCard) {
        return idCard != null && ID_CARD_PATTERN.matcher(idCard).matches();
    }

    public static String extractGender(String idCard) {
        if (!isValid(idCard)) {
            throw new IllegalArgumentException("无效的身份证号");
        }
        int genderDigit = Integer.parseInt(idCard.substring(16, 17));
        return (genderDigit % 2 == 0) ? "女" : "男";
    }

    public static String extractBirthDate(String idCard) {
        if (!isValid(idCard)) {
            throw new IllegalArgumentException("无效的身份证号");
        }
        return idCard.substring(6, 10) + "-" + idCard.substring(10, 12) + "-" + idCard.substring(12, 14);
    }
}
