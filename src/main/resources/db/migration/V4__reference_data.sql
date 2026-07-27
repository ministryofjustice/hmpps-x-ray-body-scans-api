create table if not exists reference_data_domain
(
    code             varchar(30) primary key,
    description      varchar(120) not null,
    created_at       timestamp    not null default now(),
    created_by       varchar(120) not null,
    last_modified_at timestamp    not null default now(),
    last_modified_by varchar(120) not null,
    deactivated_at   timestamp,
    deactivated_by   varchar(120)
);
comment on table reference_data_domain is 'Domains of data referenced in x-ray body scan records';
create index if not exists reference_data_domain_deactivated_at_idx on reference_data_domain (deactivated_at);
insert into reference_data_domain(code, description, created_by, last_modified_by)
values ('JUSTIFICATION', 'Why was the scan carried out?', 'CONNECT_DPS', 'CONNECT_DPS'),
       ('OUTCOME', 'What was the result of the scan?', 'CONNECT_DPS', 'CONNECT_DPS'),
       ('TYPE_OF_FIND', 'What type of item was detected?', 'CONNECT_DPS', 'CONNECT_DPS');


create table if not exists reference_data_code
(
    id               serial primary key,
    domain           varchar(30)  not null references reference_data_domain (code) on delete restrict on update cascade,
    code             varchar(30)  not null,
    description      varchar(120) not null,
    list_sequence    int          not null,
    created_at       timestamp    not null default now(),
    created_by       varchar(120) not null,
    last_modified_at timestamp    not null default now(),
    last_modified_by varchar(120) not null,
    deactivated_at   timestamp,
    deactivated_by   varchar(120),
    unique (domain, code)
);
comment on table reference_data_code is 'Values referenced in x-ray body scan records';
create index if not exists reference_data_code_list_sequence_idx on reference_data_code (list_sequence);
create index if not exists reference_data_code_deactivated_at_idx on reference_data_code (deactivated_at);
insert into reference_data_code(domain, code, description, list_sequence, created_by, last_modified_by)
values ('JUSTIFICATION', 'INTELLIGENCE', 'Intelligence-led', 1, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('JUSTIFICATION', 'REASONABLE_SUSPICION', 'Reasonable suspicion', 2, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('OUTCOME', 'NEGATIVE', 'No item detected', 1, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('OUTCOME', 'INCONCLUSIVE', 'Inconclusive', 2, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('OUTCOME', 'POSITIVE', 'Item detected', 3, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('TYPE_OF_FIND', 'ORGANIC', 'Organic', 1, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('TYPE_OF_FIND', 'INORGANIC', 'Inorganic', 2, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('TYPE_OF_FIND', 'ORGANIC_AND_INORGANIC', 'Organic and inorganic', 3, 'CONNECT_DPS', 'CONNECT_DPS'),
       ('TYPE_OF_FIND', 'NOT_KNOWN', 'Not known', 4, 'CONNECT_DPS', 'CONNECT_DPS');
