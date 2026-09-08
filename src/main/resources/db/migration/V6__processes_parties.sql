CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE processes (
    process_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(organization_id),
    cnj_number varchar(25),
    title varchar(255) NOT NULL,
    court varchar(50) NOT NULL,
    court_unit varchar(100) NOT NULL,
    location varchar(255) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT chk_processes_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    notes text,
    secret_justice boolean NOT NULL DEFAULT false,
    claim_value numeric(15, 2),
    area varchar(50),
    responsible_member_id uuid REFERENCES memberships(membership_id),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz
);

CREATE UNIQUE INDEX uq_processes_cnj_tenant_active
    ON processes (organization_id, cnj_number)
    WHERE status <> 'CLOSED' AND cnj_number IS NOT NULL;
CREATE INDEX idx_processes_org_status_updated
    ON processes (organization_id, status, updated_at DESC);
CREATE INDEX idx_processes_org_title_trgm
    ON processes USING gin (title gin_trgm_ops);

CREATE TABLE process_parties (
    party_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(organization_id),
    process_id uuid NOT NULL REFERENCES processes(process_id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    role varchar(20) NOT NULL
        CONSTRAINT chk_process_parties_role CHECK (role IN ('AUTOR', 'REU', 'TERCEIRO')),
    is_client boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_process_parties_one_client
    ON process_parties (process_id)
    WHERE is_client = true;
CREATE INDEX idx_process_parties_org_process
    ON process_parties (organization_id, process_id);
