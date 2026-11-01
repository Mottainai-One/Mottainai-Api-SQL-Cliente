-- Permission-only update required by GET /api/v1/client/loyalty/rewards.
BEGIN;

GRANT SELECT (reward_id, name, description, points_cost, active, valid_from, valid_until)
    ON TABLE mottainai.loyalty_reward TO mottainai_client_api;

ALTER TABLE mottainai.loyalty_reward ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS client_api_loyalty_reward_select ON mottainai.loyalty_reward;
CREATE POLICY client_api_loyalty_reward_select ON mottainai.loyalty_reward
    FOR SELECT TO mottainai_client_api
    USING (active IS TRUE
        AND (valid_from IS NULL OR valid_from <= CURRENT_TIMESTAMP)
        AND (valid_until IS NULL OR valid_until > CURRENT_TIMESTAMP));

COMMIT;
