package com.mari.iso20022.parser;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.exception.InvalidIso20022MessageException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Pacs008ParserTest {
    private final Pacs008Parser parser = new Pacs008Parser();
    private final String sample = new String(getClass().getResourceAsStream("/iso20022/pacs008/sample-pacs008-payment.xml").readAllBytes(), StandardCharsets.UTF_8);

    Pacs008ParserTest() throws Exception {}

    @Test
    void extractsPaymentFieldsFromPacs008() {
        Iso20022Payment payment = parser.parse(sample);
        assertEquals("MARI202610060001", payment.messageId());
        assertEquals("TXN100001", payment.transactionId());
        assertEquals(new BigDecimal("25000.00"), payment.amount());
        assertEquals("AED", payment.currency());
        assertEquals("ABC Trading LLC", payment.debtorName());
        assertEquals("XYZ Supplies LLC", payment.creditorName());
        assertEquals("AE070331234567890123456", payment.debtorIban());
        assertEquals("AE120331987654321098765", payment.creditorIban());
        assertEquals("ABCDAEAD", payment.debtorBic());
        assertEquals("WXYZAEAD", payment.creditorBic());
        assertEquals("CLRG", payment.settlementMethod());
        assertEquals("Invoice INV-2026-1042", payment.remittanceInformation());
    }

    @Test
    void rejectsMalformedXml() {
        assertThrows(InvalidIso20022MessageException.class, () -> parser.parse("<Document>"));
    }

    @Test
    void rejectsMissingRequiredField() {
        String missingTxId = sample.replace("<TxId>TXN100001</TxId>", "");
        assertThrows(InvalidIso20022MessageException.class, () -> parser.parse(missingTxId));
    }

    @Test
    void rejectsExternalEntityPayload() {
        String payload = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" + sample;
        assertThrows(InvalidIso20022MessageException.class, () -> parser.parse(payload));
    }
}
