ALTER TABLE subscriptions
    ADD COLUMN filter_local_keywords VARCHAR(500);

COMMENT ON COLUMN subscriptions.filter_local_keywords IS
    'Comma-separated keywords for post-fetch local filtering of objectInfo. '
    'If set, only tenders whose objectInfo contains at least one of these keywords (case-insensitive) are kept.';
