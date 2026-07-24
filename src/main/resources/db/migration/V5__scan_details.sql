drop table scan;

create table if not exists body_scan
(
    id               uuid primary key,
    prisoner_number  varchar(7)   not null,
    prison_id        varchar(10)  not null,
    scan_date        date         not null,

    justification    int          not null references reference_data_code (id) on delete restrict on update cascade,
    outcome          int          not null references reference_data_code (id) on delete restrict on update cascade,
    type_of_find     int references reference_data_code (id) on delete restrict on update cascade,

    created_at       timestamp    not null default now(),
    created_by       varchar(120) not null,
    last_modified_at timestamp    not null default now(),
    last_modified_by varchar(120) not null
);
comment on table body_scan is 'X-ray body scans recorded in prisons';
comment on column body_scan.id is 'Primary key as a UUIDv7';
comment on column body_scan.prisoner_number is 'Who was scanned';
comment on column body_scan.prison_id is 'Where they were scanned';
comment on column body_scan.scan_date is 'When the scan itself took place';
comment on column body_scan.justification is 'Why the scan was carried out';
comment on column body_scan.outcome is 'What the outcome of the scan was';
comment on column body_scan.type_of_find is 'What type of item was detected, if any';
comment on column body_scan.created_at is 'When this scan record was created';
comment on column body_scan.created_by is 'Who created this scan record';
comment on column body_scan.last_modified_at is 'When this scan record was updated';
comment on column body_scan.last_modified_by is 'Who updated this scan record';

create index body_scan_prisoner_number_idx on body_scan (prisoner_number);
create index body_scan_scan_date_idx on body_scan (scan_date);
create index body_scan_justification_idx on body_scan (justification);
create index body_scan_outcome_idx on body_scan (outcome);
