comment on table scan is 'X-ray body scans recorded in prisons';
comment on column scan.id is 'Scan primary key';
comment on column scan.prisoner_number is 'Who was scanned';
comment on column scan.scan_date is 'When the scan itself took place';
comment on column scan.created_at is 'When this scan record was saved';

create index scan_prisoner_number_idx on scan (prisoner_number);
create index scan_scan_date_idx on scan (scan_date);
