DROP TABLE IF EXISTS deleted_cases;

CREATE TABLE deleted_cases
(
    case_ref VARCHAR(16) NOT NULL UNIQUE,
    tasks_processed BOOLEAN NOT NULL,
    tasks_deleted BOOLEAN NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- deleted_cases comments
comment on column deleted_cases.case_ref is 'Unique case ref';

-- case ref index
CREATE INDEX deleted_cases_case_ref_idx ON deleted_cases (case_ref);