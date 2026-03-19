# Design: Tender Monitor Metrics (spec 007)

## Metrics

| Metric (Prometheus name) | Type | Tags | Description |
|--------------------------|------|------|-------------|
| monitor_tenders_found_total | Counter | subscription_id, source | New tenders found per cycle |
| monitor_cycles_total | Counter | subscription_id, source, result (found/empty/error) | Cycle outcome |
| monitor_emails_sent_total | Counter | subscription_id | Emails sent |
| monitor_api_errors_total | Counter | subscription_id, source | API/fetch errors |
| monitor_cycle_duration_seconds | Timer | subscription_id, source | Per-subscription cycle duration |

## Changes

### SubscriptionProcessor.kt
- Inject MeterRegistry
- Record all metrics inside process()
- Timer wraps entire processing block (success + failure)

### MonitorService.kt
- No changes needed (per-subscription metrics cover the full cycle)

### Grafana Dashboard
- New file: monitoring/grafana/dashboards/tender-monitor.json
- Auto-provisioned by existing dashboards.yml (scans entire /etc/grafana/provisioning/dashboards)
