-- Demo activity, loaded only under the "demo" profile (see application.yml).
-- Everything is relative to current_date so the dashboards stay meaningful whenever the
-- stack is started. Kept out of db/migration so a real deployment never gets this data.

insert into app_user (name, email, password_hash, role) values
    ('Juan Pérez',    'juan.perez@libris.cl',    '$2a$10$I3eA1WZt2pWZ5s71SAvXXuaO27RGWpHnwxjpXZkivoZ7rcaWFP8Lm', 'BIBLIOTECARIO'),
    ('Luis Martínez', 'luis.martinez@libris.cl', '$2a$10$I3eA1WZt2pWZ5s71SAvXXuaO27RGWpHnwxjpXZkivoZ7rcaWFP8Lm', 'BIBLIOTECARIO');

-- Luis reached three late returns inside the 90-day window, so his account is blocked.
update app_user
set blocked_until = now() + interval '7 days'
where lower(email) = 'luis.martinez@libris.cl';

-- Loan history. Rows with a null return date are the copies currently out.
insert into loan (book_id, borrower_name, borrower_email, loan_date, due_date, return_date)
select b.id, d.borrower_name, d.borrower_email, d.loan_date, d.due_date, d.return_date
from (values
    -- Luis: three late returns -> blocked account
    ('9780262033848', 'Luis Martínez', 'luis.martinez@libris.cl', current_date - 80, current_date - 66, current_date - 60),
    ('9780201835953', 'Luis Martínez', 'luis.martinez@libris.cl', current_date - 55, current_date - 41, current_date - 35),
    ('9780735619678', 'Luis Martínez', 'luis.martinez@libris.cl', current_date - 30, current_date - 16, current_date - 10),
    -- Juan: two late returns -> one strike away from a block
    ('9780596007126', 'Juan Pérez',    'juan.perez@libris.cl',    current_date - 70, current_date - 56, current_date - 50),
    ('9780134685991', 'Juan Pérez',    'juan.perez@libris.cl',    current_date - 40, current_date - 26, current_date - 20),
    -- Ana: a single late return
    ('9780321146533', 'Ana Torres',    'lector@libris.cl',        current_date - 60, current_date - 46, current_date - 40),
    -- Returns that were on time
    ('9780321601919', 'Carlos Gómez',  'bibliotecario@libris.cl', current_date - 50, current_date - 36, current_date - 38),
    ('9781491950357', 'María López',   'admin@libris.cl',         current_date - 45, current_date - 31, current_date - 33),
    ('9780978739218', 'Ana Torres',    'lector@libris.cl',        current_date - 25, current_date - 11, current_date - 14),
    ('9780321127426', 'Juan Pérez',    'juan.perez@libris.cl',    current_date - 20, current_date -  6, current_date -  8),
    -- Copies still out: two due soon, one overdue, one comfortably within term
    ('9780132350884', 'Carlos Gómez',  'bibliotecario@libris.cl', current_date - 12, current_date +  2, null),
    ('9780321125217', 'Carlos Gómez',  'bibliotecario@libris.cl', current_date - 12, current_date +  2, null),
    ('9780201616224', 'Ana Torres',    'lector@libris.cl',        current_date - 11, current_date +  3, null),
    ('9780201485677', 'Juan Pérez',    'juan.perez@libris.cl',    current_date - 19, current_date -  5, null),
    ('9780201633610', 'María López',   'admin@libris.cl',         current_date -  5, current_date +  9, null)
) as d(isbn, borrower_name, borrower_email, loan_date, due_date, return_date)
join book b on b.isbn = d.isbn;

-- Book availability must agree with the loans above.
update book set status = 'PRESTADO'
where isbn in ('9780132350884', '9780321125217', '9780201616224', '9780201485677', '9780201633610');

-- Held for the reader that was notified first (see the NOTIFICADO reservation below).
update book set status = 'RESERVADO' where isbn = '9780131177055';

insert into reservation (book_id, requester_name, requester_email, requested_at, status, notified_at, resolved_at)
select b.id, d.requester_name, d.requester_email, d.requested_at, d.status, d.notified_at, d.resolved_at
from (values
    -- Waiting list for Clean Code, in request order
    ('9780132350884', 'Ana Torres',   'lector@libris.cl',     now() - interval '3 days', 'PENDIENTE',  null,                      null),
    ('9780132350884', 'Juan Pérez',   'juan.perez@libris.cl', now() - interval '1 day',  'PENDIENTE',  null,                      null),
    ('9780201485677', 'Carlos Gómez', 'bibliotecario@libris.cl', now() - interval '2 days', 'PENDIENTE', null,                    null),
    -- Already notified: the copy is being held for Ana
    ('9780131177055', 'Ana Torres',   'lector@libris.cl',     now() - interval '8 days', 'NOTIFICADO', now() - interval '1 day',  null),
    -- Historical entry, gave up waiting
    ('9780201633610', 'Juan Pérez',   'juan.perez@libris.cl', now() - interval '15 days', 'CANCELADO', null,                      now() - interval '12 days')
) as d(isbn, requester_name, requester_email, requested_at, status, notified_at, resolved_at)
join book b on b.isbn = d.isbn;
