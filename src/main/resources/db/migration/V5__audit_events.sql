CREATE TABLE audit_events (
    audit_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id uuid REFERENCES users(user_id),
    organization_id uuid REFERENCES organizations(organization_id) ON DELETE SET NULL,
    action varchar(64) NOT NULL
        CONSTRAINT chk_audit_action CHECK (action IN (
            'LOGIN_SUCCESS', 'LOGIN_FAILED', 'MEMBER_INVITED', 'MEMBER_ACCEPTED',
            'MEMBER_DEACTIVATED', 'MEMBER_ROLE_CHANGED', 'OWNERSHIP_TRANSFERRED',
            'PROCESS_CREATED', 'PROCESS_UPDATED', 'PROCESS_SUSPENDED',
            'PROCESS_REACTIVATED', 'PROCESS_CLOSED', 'MOVEMENT_ADDED',
            'ORGANIZATION_CREATED', 'ORGANIZATION_SUSPENDED',
            'ORGANIZATION_ACTIVATED', 'ORGANIZATION_DEACTIVATED',
            'ENTITLEMENT_CHANGED', 'PLAN_CHANGED', 'PASSWORD_RESET'
        )),
    resource_type varchar(64) NOT NULL,
    resource_id varchar(64),
    ip_address varchar(45),
    user_agent text,
    payload jsonb
        CONSTRAINT chk_audit_payload_size CHECK (payload IS NULL OR octet_length(payload::text) <= 4096),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_org_created ON audit_events (organization_id, created_at DESC);
CREATE INDEX idx_audit_actor_created ON audit_events (actor_id, created_at DESC);
