package com.mari.iso20022.agent.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "risk-agent")
public class RiskAgentProperties {
    private int criticalPoints = 80;
    private int highPoints = 50;
    private int mediumPoints = 25;
    private int lowPoints = 10;
    private int scoreCap = 100;
    private int criticalThreshold = 80;
    private int highThreshold = 50;
    private int mediumThreshold = 20;
    private BigDecimal confidenceThreshold = new BigDecimal("0.70");

    public int getCriticalPoints() { return criticalPoints; }
    public void setCriticalPoints(int criticalPoints) { this.criticalPoints = criticalPoints; }
    public int getHighPoints() { return highPoints; }
    public void setHighPoints(int highPoints) { this.highPoints = highPoints; }
    public int getMediumPoints() { return mediumPoints; }
    public void setMediumPoints(int mediumPoints) { this.mediumPoints = mediumPoints; }
    public int getLowPoints() { return lowPoints; }
    public void setLowPoints(int lowPoints) { this.lowPoints = lowPoints; }
    public int getScoreCap() { return scoreCap; }
    public void setScoreCap(int scoreCap) { this.scoreCap = scoreCap; }
    public int getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(int criticalThreshold) { this.criticalThreshold = criticalThreshold; }
    public int getHighThreshold() { return highThreshold; }
    public void setHighThreshold(int highThreshold) { this.highThreshold = highThreshold; }
    public int getMediumThreshold() { return mediumThreshold; }
    public void setMediumThreshold(int mediumThreshold) { this.mediumThreshold = mediumThreshold; }
    public BigDecimal getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(BigDecimal confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
}
