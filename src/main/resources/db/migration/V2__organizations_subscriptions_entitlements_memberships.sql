CREATE TABLE organizations (
    organization_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(255) NOT NULL,
    cnpj_cpf varchar(18),
    contact_email varchar(255) NOT NULL,
    phone varchar(20),
    city varchar(255),
    state varchar(2),
    status varchar(50) NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT chk_organizations_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE')),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organizations_cnpj_cpf UNIQUE (cnpj_cpf)
);

CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_organizations_name ON organizations (name);

CREATE TABLE subscriptions (
    subscription_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(organization_id),
    plan_tier varchar(30) NOT NULL
        CONSTRAINT chk_subscriptions_plan
        CHECK (plan_tier IN ('BASICO', 'PROFISSIONAL', 'ENTERPRISE')),
    status varchar(30) NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT chk_subscriptions_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    current_period_end date,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subscriptions_organization UNIQUE (organization_id)
);

CREATE INDEX idx_subscriptions_period ON subscriptions (current_period_end);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);

CREATE TABLE modules (
    code varchar(50) PRIMARY KEY,
    name varchar(120) NOT NULL,
    commercial boolean NOT NULL DEFAULT true
);

INSERT INTO modules (code, name, commercial) VALUES
    ('CORE', 'Núcleo', false),
    ('PROCESS', 'M01 - Processos', true),
    ('DEADLINES', 'M02 - Prazos e Compromissos', true),
    ('DOCUMENTS', 'M03 - Documentos', true),
    ('TASKS', 'M04 - Tarefas', true),
    ('AI_JURIDICA', 'M05 - IA Jurídica', true),
    ('TRIBUNALS', 'M06 - Tribunais', true),
    ('REPORTS', 'M07 - Relatórios', true),
    ('BILLING', 'M08 - Cobrança', true);

CREATE TABLE module_entitlements (
    entitlement_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(organization_id),
    module_code varchar(50) NOT NULL REFERENCES modules(code),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_entitlements_org_module UNIQUE (organization_id, module_code)
);

CREATE INDEX idx_entitlements_org ON module_entitlements (organization_id);

CREATE TABLE memberships (
    membership_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(organization_id),
    user_id uuid NOT NULL REFERENCES users(user_id),
    role varchar(30) NOT NULL
        CONSTRAINT chk_memberships_role
        CHECK (role IN ('OWNER', 'ADMIN', 'LAWYER', 'ASSISTANT')),
    status varchar(30) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_memberships_status
        CHECK (status IN ('ACTIVE', 'PENDING', 'INACTIVE')),
    invite_token_hash varchar(64),
    invite_expires_at timestamptz,
    invited_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at timestamptz,
    deactivated_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_memberships_org_user_active
    ON memberships (organization_id, user_id) WHERE status <> 'INACTIVE';
CREATE UNIQUE INDEX uq_memberships_user_active
    ON memberships (user_id) WHERE status <> 'INACTIVE';
CREATE INDEX idx_memberships_org_role_status
    ON memberships (organization_id, role, status);
CREATE INDEX idx_memberships_user ON memberships (user_id);
