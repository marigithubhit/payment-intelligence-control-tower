package com.mari.iso20022.agent.verification;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents/verification")
public class VerificationAgentController {
    private final VerificationAgent verificationAgent;

    public VerificationAgentController(VerificationAgent verificationAgent) {
        this.verificationAgent = verificationAgent;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public VerificationAgentResponse verify(@Valid @RequestBody VerificationAgentRequest request) {
        return verificationAgent.verify(request);
    }
}
