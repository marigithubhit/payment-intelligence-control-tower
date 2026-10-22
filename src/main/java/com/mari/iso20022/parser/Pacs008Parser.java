package com.mari.iso20022.parser;

import com.mari.iso20022.dto.Iso20022Payment;
import com.mari.iso20022.exception.InvalidIso20022MessageException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class Pacs008Parser {
    private static final String NAMESPACE = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

    public Iso20022Payment parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw invalid("XML content is empty");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            if (root == null || !"Document".equals(root.getLocalName()) || !NAMESPACE.equals(root.getNamespaceURI())) {
                throw invalid("Unsupported XML message; expected pacs.008.001.08");
            }

            Element creditTransfer = requiredElement(root, "FIToFICstmrCdtTrf");
            Element header = requiredElement(creditTransfer, "GrpHdr");
            Element transfer = requiredElement(creditTransfer, "CdtTrfTxInf");
            Element paymentId = requiredElement(transfer, "PmtId");
            Element amount = requiredElement(transfer, "IntrBkSttlmAmt");
            String currency = requiredAttribute(amount, "Ccy");
            BigDecimal parsedAmount = new BigDecimal(value(amount));
            if (parsedAmount.signum() <= 0) throw invalid("Amount must be greater than zero");

            String debtorName = value(requiredElement(transfer, "Dbtr"), "Nm");
            String debtorIban = valuePath(transfer, "DbtrAcct", "Id", "IBAN");
            String debtorBic = valuePath(transfer, "DbtrAgt", "FinInstnId", "BICFI");
            String creditorName = value(requiredElement(transfer, "Cdtr"), "Nm");
            String creditorIban = valuePath(transfer, "CdtrAcct", "Id", "IBAN");
            String creditorBic = valuePath(transfer, "CdtrAgt", "FinInstnId", "BICFI");

            String transactionCount = requiredValue(header, "NbOfTxs");
            int count = Integer.parseInt(transactionCount);
            if (count < 1) throw invalid("Number of transactions must be at least one");

            return new Iso20022Payment(
                    requiredValue(header, "MsgId"), OffsetDateTime.parse(requiredValue(header, "CreDtTm")), count,
                    value(requiredElement(requiredElement(header, "SttlmInf"), "SttlmMtd")),
                    requiredValue(paymentId, "InstrId"), requiredValue(paymentId, "EndToEndId"),
                    requiredValue(paymentId, "TxId"), parsedAmount, currency,
                    LocalDate.parse(requiredValue(transfer, "IntrBkSttlmDt")), debtorName, debtorIban, debtorBic,
                    creditorName, creditorIban, creditorBic,
                    value(requiredElement(requiredElement(transfer, "RmtInf"), "Ustrd")));
        } catch (InvalidIso20022MessageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidIso20022MessageException("Malformed or incomplete pacs.008 message", exception);
        }
    }

    private Element requiredElement(Element parent, String name) {
        Element element = first(parent, name);
        if (element == null) throw invalid("Missing required field: " + name);
        return element;
    }

    private String requiredValue(Element parent, String name) {
        String result = value(requiredElement(parent, name));
        if (result.isBlank()) throw invalid("Missing required field: " + name);
        return result;
    }

    private String value(Element parent, String name) {
        return value(requiredElement(parent, name));
    }

    private String valuePath(Element parent, String... path) {
        Element current = parent;
        for (String segment : path) current = requiredElement(current, segment);
        return value(current);
    }

    private String value(Element element) {
        String result = element.getTextContent().trim();
        if (result.isBlank()) throw invalid("Required field is empty: " + element.getLocalName());
        return result;
    }

    private String requiredAttribute(Element element, String name) {
        String result = element.getAttribute(name).trim();
        if (result.isBlank()) throw invalid("Missing required attribute: " + name);
        return result;
    }

    private Element first(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())
                    && NAMESPACE.equals(element.getNamespaceURI())) return element;
        }
        return null;
    }

    private InvalidIso20022MessageException invalid(String message) {
        return new InvalidIso20022MessageException(message);
    }
}
