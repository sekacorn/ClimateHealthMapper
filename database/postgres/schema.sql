-- ClimateHealthMapper PostgreSQL Schema
-- Production-ready schema with SSO, MFA, RBAC, and full data model

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- User Roles Enum
CREATE TYPE user_role AS ENUM ('USER', 'MODERATOR', 'ADMIN', 'ENTERPRISE');

-- MBTI Types Enum
CREATE TYPE mbti_type AS ENUM (
    'ENTJ', 'INFP', 'INFJ', 'ESTP', 'INTJ', 'INTP',
    'ISTJ', 'ESFJ', 'ISFP', 'ENTP', 'ISFJ', 'ESFP',
    'ENFJ', 'ESTJ', 'ISTP', 'ENFP'
);

-- Data Format Enum
CREATE TYPE data_format AS ENUM ('CSV', 'JSON', 'FHIR', 'VCF', 'GEOJSON');

-- Users Table with SSO and MFA Support
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255), -- Nullable for SSO users
    role user_role NOT NULL DEFAULT 'USER',
    mbti_preference mbti_type,

    -- SSO Fields
    sso_provider VARCHAR(100), -- e.g., 'google', 'okta', 'azure_ad', 'saml'
    sso_subject VARCHAR(255), -- Unique identifier from SSO provider
    sso_enabled BOOLEAN DEFAULT false,

    -- MFA Fields
    mfa_enabled BOOLEAN DEFAULT false,
    mfa_secret VARCHAR(255), -- TOTP secret
    mfa_backup_codes TEXT[], -- Array of backup codes

    -- Enterprise Fields
    organization_id UUID,
    department VARCHAR(255),

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,

    CONSTRAINT valid_sso_config CHECK (
        (sso_enabled = true AND sso_provider IS NOT NULL AND sso_subject IS NOT NULL) OR
        (sso_enabled = false)
    )
);

-- Organizations Table (for Enterprise)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL,
    domain VARCHAR(255),

    -- SSO Configuration
    sso_provider VARCHAR(100),
    sso_metadata TEXT, -- SAML metadata or OIDC configuration
    sso_entity_id VARCHAR(255),

    -- Subscription
    subscription_tier VARCHAR(50) DEFAULT 'BASIC', -- BASIC, PRO, ENTERPRISE
    max_users INTEGER DEFAULT 10,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Add foreign key for organization
ALTER TABLE users ADD CONSTRAINT fk_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL;

-- MFA Sessions Table
CREATE TABLE mfa_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    verified BOOLEAN DEFAULT false,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SSO Sessions Table
CREATE TABLE sso_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(100) NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_refreshed TIMESTAMP
);

-- Environmental Data Table
CREATE TABLE environmental_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- Location
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    location_name VARCHAR(255),
    region VARCHAR(255),

    -- Air Quality Metrics
    pm25 DECIMAL(10, 2), -- PM2.5 particulate matter
    pm10 DECIMAL(10, 2), -- PM10 particulate matter
    aqi INTEGER, -- Air Quality Index
    ozone DECIMAL(10, 2),
    no2 DECIMAL(10, 2), -- Nitrogen dioxide
    so2 DECIMAL(10, 2), -- Sulfur dioxide
    co DECIMAL(10, 2), -- Carbon monoxide

    -- Temperature
    temperature DECIMAL(5, 2),
    heat_index DECIMAL(5, 2),
    humidity DECIMAL(5, 2),

    -- Source
    data_source VARCHAR(100), -- e.g., 'NOAA', 'EPA', 'Manual'
    data_format data_format,

    -- Timestamps
    measurement_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Spatial index
    CONSTRAINT valid_coordinates CHECK (
        latitude BETWEEN -90 AND 90 AND
        longitude BETWEEN -180 AND 180
    )
);

-- Health Data Table (FHIR-Compatible)
CREATE TABLE health_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- FHIR Fields
    fhir_resource_type VARCHAR(100), -- e.g., 'Observation', 'Condition'
    fhir_resource_id VARCHAR(255),
    fhir_raw_data JSONB, -- Full FHIR resource

    -- Common Health Metrics
    condition_code VARCHAR(100), -- ICD-10 or SNOMED code
    condition_name VARCHAR(255),
    severity VARCHAR(50), -- mild, moderate, severe

    -- Vital Signs
    heart_rate INTEGER,
    blood_pressure_systolic INTEGER,
    blood_pressure_diastolic INTEGER,
    respiratory_rate INTEGER,
    oxygen_saturation DECIMAL(5, 2),

    -- Symptoms
    symptoms TEXT[],

    -- Source
    data_source VARCHAR(100), -- e.g., 'Epic', 'Cerner', 'Manual'
    data_format data_format,

    -- Timestamps
    recorded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Genomic Data Table
CREATE TABLE genomic_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- VCF Data
    chromosome VARCHAR(10),
    position BIGINT,
    reference_allele VARCHAR(255),
    alternate_allele VARCHAR(255),
    quality_score DECIMAL(10, 2),
    filter_status VARCHAR(50),

    -- Gene Information
    gene_symbol VARCHAR(100),
    variant_type VARCHAR(100), -- SNP, insertion, deletion, etc.
    clinical_significance VARCHAR(100), -- pathogenic, benign, etc.

    -- Risk Associations
    disease_associations TEXT[], -- Array of associated diseases
    risk_score DECIMAL(5, 4),

    -- Source
    data_source VARCHAR(100), -- e.g., '23andMe', 'AncestryDNA'
    data_format data_format DEFAULT 'VCF',
    vcf_raw_data TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Health Risk Predictions Table
CREATE TABLE health_predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- Risk Assessment
    risk_type VARCHAR(255) NOT NULL, -- e.g., 'asthma_exacerbation', 'heatstroke'
    risk_score DECIMAL(5, 4) NOT NULL, -- 0.0 to 1.0
    risk_level VARCHAR(50), -- low, medium, high, critical

    -- Contributing Factors
    environmental_factors JSONB,
    health_factors JSONB,
    genomic_factors JSONB,

    -- Recommendations
    recommendations TEXT[],
    interventions TEXT[],

    -- Model Information
    model_version VARCHAR(50),
    confidence_score DECIMAL(5, 4),

    -- Timestamps
    predicted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP
);

-- User Sessions Table
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_name VARCHAR(255),

    -- Session State
    view_state JSONB, -- Camera position, zoom level, etc.
    filters JSONB,
    selected_datasets UUID[],

    -- Bookmarks
    bookmarks JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Annotations Table
CREATE TABLE annotations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id UUID REFERENCES user_sessions(id) ON DELETE CASCADE,

    -- Annotation Content
    annotation_type VARCHAR(50), -- note, highlight, marker
    content TEXT NOT NULL,
    position JSONB, -- 3D coordinates or screen position

    -- Styling
    color VARCHAR(7), -- Hex color
    style JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Collaboration Sessions Table
CREATE TABLE collaboration_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_name VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Session Configuration
    max_participants INTEGER DEFAULT 10,
    is_public BOOLEAN DEFAULT false,
    password_hash VARCHAR(255),

    -- Session State
    shared_view_state JSONB,
    shared_data UUID[],

    -- Status
    is_active BOOLEAN DEFAULT true,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);

-- Collaboration Participants Table
CREATE TABLE collaboration_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES collaboration_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Permissions
    can_edit BOOLEAN DEFAULT false,
    can_annotate BOOLEAN DEFAULT true,

    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,

    UNIQUE(session_id, user_id)
);

-- User Actions Log (for collaboration)
CREATE TABLE user_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES collaboration_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Action Details
    action_type VARCHAR(100) NOT NULL, -- zoom, pan, filter, annotate, etc.
    action_data JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Visualizations Table
CREATE TABLE visualizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    session_id UUID REFERENCES user_sessions(id) ON DELETE CASCADE,

    -- Visualization Details
    visualization_type VARCHAR(100) NOT NULL, -- '3d_climate_map', 'health_heatmap', etc.
    title VARCHAR(255),
    description TEXT,

    -- Configuration
    config JSONB NOT NULL, -- Three.js scene configuration
    data_sources UUID[], -- References to data tables

    -- Export
    thumbnail_url VARCHAR(500),
    export_formats VARCHAR(50)[], -- PNG, SVG, STL

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Error Logs Table
CREATE TABLE error_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Error Details
    error_type VARCHAR(100),
    error_message TEXT,
    stack_trace TEXT,

    -- Context
    service_name VARCHAR(100), -- Which microservice
    endpoint VARCHAR(255),
    request_data JSONB,

    -- Resolution
    resolved BOOLEAN DEFAULT false,
    resolution_notes TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- LLM Query History Table
CREATE TABLE llm_queries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,

    -- Query
    query_text TEXT NOT NULL,
    query_context JSONB, -- MBTI type, current view, etc.

    -- Response
    response_text TEXT,
    response_metadata JSONB,

    -- Performance
    processing_time_ms INTEGER,
    model_used VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit Logs Table (for compliance)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,

    -- Action Details
    action VARCHAR(100) NOT NULL, -- login, data_upload, export, etc.
    resource_type VARCHAR(100),
    resource_id UUID,

    -- Metadata
    ip_address INET,
    user_agent TEXT,
    details JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_sso_subject ON users(sso_subject);
CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_environmental_data_user ON environmental_data(user_id);
CREATE INDEX idx_environmental_data_location ON environmental_data(latitude, longitude);
CREATE INDEX idx_environmental_data_time ON environmental_data(measurement_time);
CREATE INDEX idx_health_data_user ON health_data(user_id);
CREATE INDEX idx_health_data_time ON health_data(recorded_at);
CREATE INDEX idx_genomic_data_user ON genomic_data(user_id);
CREATE INDEX idx_genomic_data_gene ON genomic_data(gene_symbol);
CREATE INDEX idx_health_predictions_user ON health_predictions(user_id);
CREATE INDEX idx_collaboration_sessions_active ON collaboration_sessions(is_active);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_time ON audit_logs(created_at);

-- Triggers for Updated_At
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_sessions_updated_at BEFORE UPDATE ON user_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_annotations_updated_at BEFORE UPDATE ON annotations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_visualizations_updated_at BEFORE UPDATE ON visualizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Views for Common Queries
CREATE VIEW user_health_overview AS
SELECT
    u.id as user_id,
    u.username,
    u.email,
    u.mbti_preference,
    COUNT(DISTINCT hd.id) as health_records_count,
    COUNT(DISTINCT ed.id) as environmental_records_count,
    COUNT(DISTINCT gd.id) as genomic_records_count,
    COUNT(DISTINCT hp.id) as predictions_count,
    MAX(hp.predicted_at) as last_prediction_date
FROM users u
LEFT JOIN health_data hd ON u.id = hd.user_id
LEFT JOIN environmental_data ed ON u.id = ed.user_id
LEFT JOIN genomic_data gd ON u.id = gd.user_id
LEFT JOIN health_predictions hp ON u.id = hp.user_id
GROUP BY u.id, u.username, u.email, u.mbti_preference;

-- Sample Data (for development/testing)
-- Default admin user (password: admin123 - should be changed in production)
INSERT INTO users (username, email, password_hash, role, is_active, email_verified)
VALUES ('admin', 'admin@climatehealth.org', crypt('admin123', gen_salt('bf')), 'ADMIN', true, true);

-- Sample organization
INSERT INTO organizations (name, domain, subscription_tier, max_users)
VALUES ('ClimateHealth Foundation', 'climatehealth.org', 'ENTERPRISE', 1000);
