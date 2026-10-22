package com.mari.iso20022.validation;

import java.util.Locale;

public final class BeneficiaryIdentity {
    private BeneficiaryIdentity() {
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
