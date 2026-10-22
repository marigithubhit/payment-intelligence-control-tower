package com.mari.iso20022.commandcenter;

import com.mari.iso20022.commandcenter.dto.CommandCenterDtos.*;
import com.mari.iso20022.commandcenter.service.CorrectiveActionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CorrectiveActionServiceTest {

    @Autowired
    private CorrectiveActionService actionService;

    @Test
    void createsAndManagesCorrectiveActionLifecycle() {
        // 1. Create proposed action
        CreateActionRequest createReq = new CreateActionRequest(
                null,
                "TXN-TEST-9001",
                "Beneficiary Remediation Test",
                "Testing action creation workflow",
                "BENEFICIARY_DATA_REMEDIATION",
                "Payments Operations",
                "Senior Officer",
                "Analyst A",
                true,
                "Mismatch Rate",
                "15.0%"
        );
        CorrectiveActionResponse created = actionService.createAction(createReq);
        assertNotNull(created.id());
        assertEquals("PROPOSED", created.status());
        assertEquals("PENDING", created.approvalStatus());

        // 2. Add an audit note
        actionService.addNote(created.id(), new ActionNoteRequest("Reviewer 1", "Lead", "Investigated case thoroughly"));
        CorrectiveActionResponse withNote = actionService.getAction(created.id());
        assertEquals(1, withNote.notes().size());
        assertEquals("Investigated case thoroughly", withNote.notes().get(0).note());

        // 3. Approve action
        CorrectiveActionResponse approved = actionService.processApproval(created.id(),
                new ActionApprovalRequest("APPROVED", "Executive VP", "Approved after customer callback"));
        assertEquals("APPROVED", approved.approvalStatus());
        assertEquals("IN_PROGRESS", approved.status());
        assertEquals("Executive VP", approved.approvedBy());

        // 4. Update status to COMPLETED
        CorrectiveActionResponse completed = actionService.updateStatus(created.id(),
                new ActionStatusUpdateRequest("COMPLETED"));
        assertEquals("COMPLETED", completed.status());
        assertNotNull(completed.completedAt());

        // 5. Record measured outcome
        CorrectiveActionResponse measured = actionService.updateOutcome(created.id(),
                new ActionOutcomeUpdateRequest("0.5%", "MEASURED_IMPROVEMENT", "Mismatch rate reduced to 0.5%"));
        assertEquals("0.5%", measured.postActionMetricValue());
        assertEquals("MEASURED_IMPROVEMENT", measured.outcomeStatus());
    }
}
