ALTER TABLE scan ADD COLUMN result VARCHAR(12) NOT NULL default 'INCONCLUSIVE';

comment on column scan.result is 'Result of the scan: POSITIVE, NEGATIVE or INCONCLUSIVE';
