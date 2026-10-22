package com.mari.iso20022.agent.reasoning;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reasoning")
public class ReasoningProperties {
    private String promptVersion = "1.0";
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
}
