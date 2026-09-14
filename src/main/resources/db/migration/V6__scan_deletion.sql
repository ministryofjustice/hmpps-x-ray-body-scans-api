alter table body_scan add column deleted_at timestamp;
alter table body_scan add column deleted_reason varchar(255);

comment on column body_scan.deleted_at is 'When the scan was deleted';
comment on column body_scan.deleted_reason is 'Why the scan was deleted';

create index body_scan_deleted_at_idx on body_scan (deleted_at);
