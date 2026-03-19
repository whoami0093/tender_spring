# Tasks: Tender Monitor Metrics (spec 007)

## Phase 1: Metrics
- [x] Inject MeterRegistry into SubscriptionProcessor
- [x] Add monitor_tenders_found_total counter
- [x] Add monitor_cycles_total counter (found/empty/error)
- [x] Add monitor_emails_sent_total counter
- [x] Add monitor_api_errors_total counter
- [x] Add monitor_cycle_duration_seconds timer

## Phase 2: Dashboard
- [x] Create monitoring/grafana/dashboards/tender-monitor.json

## Phase 3: Verify
- [ ] curl /actuator/prometheus | grep monitor_ returns metrics
- [ ] Grafana dashboard shows data after next cycle
