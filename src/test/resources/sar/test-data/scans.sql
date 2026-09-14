INSERT INTO body_scan (id, prisoner_number, prison_id, scan_date, justification, outcome, type_of_find, created_by, last_modified_by, created_at, last_modified_at, deleted_at, deleted_reason)
VALUES (
  '019fc832-0000-7000-0000-000000000001',
  'A1234BC',
  'MDI',
  '2026-08-02',
  (SELECT id FROM reference_data_code WHERE domain = 'JUSTIFICATION' AND code = 'REASONABLE_SUSPICION'),
  (SELECT id FROM reference_data_code WHERE domain = 'OUTCOME' AND code = 'POSITIVE'),
  (SELECT id FROM reference_data_code WHERE domain = 'TYPE_OF_FIND' AND code = 'INORGANIC'),
  'abc12a',
  'abc12a',
  '2026-08-02 14:21:10',
  '2026-08-02 14:21:10',
  '2026-08-03 9:48:02',
  'Recorded in error'
);

INSERT INTO body_scan (id, prisoner_number, prison_id, scan_date, justification, outcome, type_of_find, case_note_id, created_by, last_modified_by, created_at, last_modified_at)
VALUES (
  '019fc832-0000-7000-0000-000000000002',
  'A1234BC',
  'LEI',
  '2026-08-12',
  (SELECT id FROM reference_data_code WHERE domain = 'JUSTIFICATION' AND code = 'REASONABLE_SUSPICION'),
  (SELECT id FROM reference_data_code WHERE domain = 'OUTCOME' AND code = 'POSITIVE'),
  (SELECT id FROM reference_data_code WHERE domain = 'TYPE_OF_FIND' AND code = 'INORGANIC'),
  '01a067dc-332f-754e-b41f-d8fe1eaeba89',
  'someone',
  'someone',
  '2026-08-13 10:00:00',
  '2026-08-13 10:00:00'
);

INSERT INTO body_scan (id, prisoner_number, prison_id, scan_date, justification, outcome, type_of_find, created_by, last_modified_by, created_at, last_modified_at)
VALUES (
  '019fc832-0000-7000-0000-000000000003',
  'A1234BC',
  'LEI',
  '2026-08-13',
  (SELECT id FROM reference_data_code WHERE domain = 'JUSTIFICATION' AND code = 'INTELLIGENCE'),
  (SELECT id FROM reference_data_code WHERE domain = 'OUTCOME' AND code = 'NEGATIVE'),
  NULL,
  'someone-else',
  'someone-else',
  '2026-08-13 13:00:00',
  '2026-08-13 13:00:00'
);
