package com.mari.iso20022;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.mari.iso20022.investigation.HistoricalAnalysisProperties;
import com.mari.iso20022.agent.risk.RiskAgentProperties;
import com.mari.iso20022.agent.reasoning.OllamaProperties;
import com.mari.iso20022.agent.reasoning.ReasoningProperties;

@SpringBootApplication
@EnableConfigurationProperties({HistoricalAnalysisProperties.class, RiskAgentProperties.class,
        OllamaProperties.class, ReasoningProperties.class})
public class Iso20022AiInvestigationApplication {

    public static void main(String[] args) {
        SpringApplication.run(Iso20022AiInvestigationApplication.class, args);
    }
}
