package com.mari.iso20022.controller;

import com.mari.iso20022.dto.PaymentIngestionResponse;
import com.mari.iso20022.service.PaymentIngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentIngestionController {
    private final PaymentIngestionService ingestionService;

    public PaymentIngestionController(PaymentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/iso20022", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PaymentIngestionResponse ingest(@RequestBody String xml) {
        return ingestionService.ingest(xml);
    }
}
