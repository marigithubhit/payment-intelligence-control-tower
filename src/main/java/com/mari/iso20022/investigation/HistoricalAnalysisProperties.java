package com.mari.iso20022.investigation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "historical-analysis")
public class HistoricalAnalysisProperties {
    private int windowDays = 90;

    public int getWindowDays() { return windowDays; }
    public void setWindowDays(int windowDays) { this.windowDays = windowDays; }
}
