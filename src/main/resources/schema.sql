create table if not exists optimization_report (
    id uuid primary key,
    created_at timestamp with time zone not null,
    channel varchar(8) not null,
    product_name varchar(120) not null,
    status varchar(40) not null,
    payload clob not null
);

create index if not exists idx_report_created_at on optimization_report(created_at desc);

create table if not exists optimization_feedback (
    id uuid primary key,
    report_id uuid not null,
    created_at timestamp with time zone not null,
    metric varchar(120) not null,
    baseline double precision not null,
    observed double precision not null,
    notes varchar(2000),
    constraint fk_feedback_report foreign key (report_id) references optimization_report(id)
);
