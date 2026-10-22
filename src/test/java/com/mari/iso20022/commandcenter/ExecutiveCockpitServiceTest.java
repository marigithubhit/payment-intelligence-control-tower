package com.mari.iso20022.commandcenter;

import com.mari.iso20022.commandcenter.dto.CommandCenterDtos.*;
import com.mari.iso20022.commandcenter.service.ExecutiveCockpitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExecutiveCockpitServiceTest {

    @Autowired
    private ExecutiveCockpitService cockpitService;

    @Test
    void generatesExecutiveOverviewWithValidCalculations() {
        ExecutiveOverview overview = cockpitService.getOverview();

        assertNotNull(overview);
        assertNotNull(overview.kpiSummary());
        assertNotNull(overview.investigationSummary());
        assertNotNull(overview.riskBreakdown());
        assertNotNull(overview.slaMetrics());
        assertNotNull(overview.ageingBuckets());
        assertNotNull(overview.bottlenecks());
        assertNotNull(overview.priorityAttentionQueue());

        assertTrue(overview.kpiSummary().totalTransactions() >= 0);
        assertTrue(overview.kpiSummary().straightThroughProcessingRate() >= 0.0);
        assertTrue(overview.kpiSummary().straightThroughProcessingRate() <= 100.0);
        assertTrue(overview.slaMetrics().overallSlaComplianceRate() >= 0.0);
        assertTrue(overview.slaMetrics().overallSlaComplianceRate() <= 100.0);
        assertNotNull(overview.dataDisclaimer());
    }

    @Test
    void generatesRegionalIntelligenceForConfiguredMeaMarkets() {
        RegionalIntelligence regional = cockpitService.getRegionalIntelligence();

        assertNotNull(regional);
        assertNotNull(regional.markets());
        assertFalse(regional.markets().isEmpty());

        List<String> marketCodes = regional.markets().stream()
                .map(MarketPerformance::marketCode)
                .toList();

        assertTrue(marketCodes.contains("AE"), "UAE market must be configured");
        assertTrue(marketCodes.contains("SA"), "Saudi Arabia market must be configured");
        assertTrue(marketCodes.contains("BH"), "Bahrain market must be configured");
        assertTrue(marketCodes.contains("ZA"), "South Africa market must be configured");

        assertNotNull(regional.rails());
        assertFalse(regional.rails().isEmpty());
    }

    @Test
    void generatesDeterministicExceptionPatterns() {
        List<ExceptionPattern> patterns = cockpitService.getExceptionPatterns();

        assertNotNull(patterns);
        assertFalse(patterns.isEmpty());

        for (ExceptionPattern pattern : patterns) {
            assertNotNull(pattern.patternId());
            assertNotNull(pattern.patternName());
            assertNotNull(pattern.detectionLogic());
            assertNotNull(pattern.recommendedNextStep());
            assertNotNull(pattern.confidenceLimitations());
        }
    }

    @Test
    void generatesPrioritizedQueueWithTransparentScoreBreakdown() {
        List<PriorityQueueItem> queue = cockpitService.getPrioritizedQueue();

        assertNotNull(queue);
        for (PriorityQueueItem item : queue) {
            assertNotNull(item.transactionId());
            assertNotNull(item.scoreBreakdown());
            assertTrue(item.priorityScore() >= 0);
            assertTrue(item.priorityScore() <= 100);
            assertEquals(item.priorityScore(), item.scoreBreakdown().totalScore());
        }
    }
}
