-- Core schema for the library loan system.

create table app_user (
    id            bigserial    primary key,
    name          varchar(120) not null,
    email         varchar(180) not null,
    password_hash varchar(100) not null,
    role          varchar(20)  not null,
    blocked_until timestamptz,
    created_at    timestamptz  not null default now(),
    constraint ck_app_user_role check (role in ('ADMIN', 'BIBLIOTECARIO'))
);

-- Case-insensitive uniqueness: alice@x.cl and Alice@X.cl are the same account.
create unique index uk_app_user_email on app_user (lower(email));

create table book (
    id               bigserial    primary key,
    title            varchar(250) not null,
    author           varchar(180) not null,
    isbn             varchar(20)  not null,
    publication_year integer,
    status           varchar(20)  not null,
    cover_url        varchar(500),
    description      text,
    created_at       timestamptz  not null default now(),
    constraint uk_book_isbn unique (isbn),
    constraint ck_book_status check (status in ('DISPONIBLE', 'PRESTADO', 'RESERVADO')),
    constraint ck_book_year check (publication_year is null or publication_year between 1000 and 2200)
);

create index ix_book_status on book (status);
create index ix_book_title on book (lower(title));
create index ix_book_author on book (lower(author));

create table book_subject (
    book_id bigint       not null,
    subject varchar(120) not null,
    constraint pk_book_subject primary key (book_id, subject),
    constraint fk_book_subject_book foreign key (book_id) references book (id) on delete cascade
);

create index ix_book_subject_subject on book_subject (subject);

create table loan (
    id                     bigserial    primary key,
    book_id                bigint       not null,
    borrower_name          varchar(120) not null,
    borrower_email         varchar(180) not null,
    loan_date              date         not null,
    due_date               date         not null,
    return_date            date,
    reminder_sent_at       timestamptz,
    overdue_notice_sent_at timestamptz,
    created_at             timestamptz  not null default now(),
    constraint fk_loan_book foreign key (book_id) references book (id),
    constraint ck_loan_due_after_loan check (due_date >= loan_date),
    constraint ck_loan_return_after_loan check (return_date is null or return_date >= loan_date)
);

create index ix_loan_borrower_email on loan (lower(borrower_email));
create index ix_loan_book on loan (book_id);
create index ix_loan_open_due_date on loan (due_date) where return_date is null;

-- A copy can only be out once at a time. Enforced by the database, not just by the service.
create unique index uk_loan_open_per_book on loan (book_id) where return_date is null;

create table reservation (
    id              bigserial    primary key,
    book_id         bigint       not null,
    requester_name  varchar(120) not null,
    requester_email varchar(180) not null,
    requested_at    timestamptz  not null,
    status          varchar(20)  not null,
    notified_at     timestamptz,
    resolved_at     timestamptz,
    constraint fk_reservation_book foreign key (book_id) references book (id),
    constraint ck_reservation_status check (status in ('PENDIENTE', 'NOTIFICADO', 'CANCELADO', 'CUMPLIDO'))
);

create index ix_reservation_queue on reservation (book_id, status, requested_at);
create index ix_reservation_requester on reservation (lower(requester_email));

-- The same reader cannot hold two live places in the queue for the same title.
create unique index uk_reservation_active
    on reservation (book_id, lower(requester_email))
    where status in ('PENDIENTE', 'NOTIFICADO');
