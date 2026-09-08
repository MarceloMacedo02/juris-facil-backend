CREATE TABLE process_movements (
    movement_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(organization_id),
    process_id uuid NOT NULL REFERENCES processes(process_id) ON DELETE CASCADE,
    movement_date timestamptz NOT NULL,
    movement_type varchar(20) NOT NULL
        CONSTRAINT chk_process_movements_type CHECK (movement_type IN (
            'DECISAO', 'DESPACHO', 'PETICAO', 'SENTENCA', 'AUDIENCIA', 'OUTRO'
        )),
    title varchar(255) NOT NULL,
    description text,
    source varchar(20) NOT NULL DEFAULT 'MANUAL'
        CONSTRAINT chk_process_movements_source CHECK (source IN ('MANUAL', 'COURT')),
    created_by uuid REFERENCES memberships(membership_id) ON DELETE SET NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movements_process_cron
    ON process_movements (process_id, movement_date DESC);
CREATE INDEX idx_movements_org_process
    ON process_movements (organization_id, process_id);
