# Requirements: Tender Monitor Metrics (spec 007)

## Overview
Add Micrometer business metrics to the tender monitoring subsystem so that operators can build meaningful Grafana dashboards and observe monitor health.

## User Stories
- As an operator, I want to see how many new tenders are found per subscription over time
- As an operator, I want to see the rate of monitor cycle errors to detect API issues
- As an operator, I want a Grafana dashboard showing all monitor KPIs in one place

## Acceptance Criteria
1. After deploy:  returns metrics
2. After next monitor cycle: Grafana → Tender Monitor dashboard shows real data
3. Numbers on dashboard match log entries ()

## Constraints
- Use existing Micrometer dependency (already in build.gradle.kts)
- No new infrastructure required
