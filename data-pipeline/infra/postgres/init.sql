create table if not exists sales_source (
    sale_id varchar(64) primary key,
    city varchar(120) not null,
    salesman varchar(120) not null,
    amount numeric(12,2) not null,
    event_time timestamp not null,
    source varchar(8) not null default 'DB',
    published boolean not null default false
);

create table if not exists city_sales_totals (
    city varchar(120) not null,
    salesman varchar(120) not null,
    source varchar(8) not null,
    total_amount numeric(14,2) not null,
    primary key (city, salesman, source)
);

create table if not exists top_sales_per_city (
    city varchar(120) primary key,
    salesman varchar(120) not null,
    source varchar(8) not null,
    total_amount numeric(14,2) not null
);

create table if not exists salesman_totals (
    salesman varchar(120) not null,
    source varchar(8) not null,
    primary key (salesman, source),
    total_amount numeric(14,2) not null
);

create table if not exists top_salesman_country (
    id int primary key,
    salesman varchar(120) not null,
    source varchar(8) not null,
    total_amount numeric(14,2) not null
);

insert into sales_source(sale_id, city, salesman, amount, event_time, source, published) values
('db-1','Sao Paulo','Ana',1200.50,now() - interval '4 minute','DB',false),
('db-2','Gravatai','Pietro',900.00,now() - interval '3 minute','DB',false),
('db-3','Porto Alegre','Rafael',1600.00,now() - interval '2 minute','DB',false),
('db-4','Curitiba','Ana',700.00,now() - interval '1 minute','DB',false)
on conflict (sale_id) do nothing;