create table if not exists sales_source (
    sale_id varchar(64) primary key,
    city varchar(120) not null,
    salesman varchar(120) not null,
    amount numeric(12,2) not null,
    event_time timestamp not null,
    published boolean not null default false
);

create table if not exists city_sales_totals (
    city varchar(120) not null,
    salesman varchar(120) not null,
    total_amount numeric(14,2) not null,
    primary key (city, salesman)
);

create table if not exists top_sales_per_city (
    city varchar(120) primary key,
    salesman varchar(120) not null,
    total_amount numeric(14,2) not null
);

create table if not exists salesman_totals (
    salesman varchar(120) primary key,
    total_amount numeric(14,2) not null
);

create table if not exists top_salesman_country (
    id int primary key,
    salesman varchar(120) not null,
    total_amount numeric(14,2) not null
);

insert into sales_source(sale_id, city, salesman, amount, event_time, published) values
('db-1','Sao Paulo','Ana',1200.50,now() - interval '4 minute',false),
('db-2','Rio de Janeiro','Pietro',900.00,now() - interval '3 minute',false),
('db-3','Sao Paulo','Rafael',1600.00,now() - interval '2 minute',false),
('db-4','Curitiba','Ana',700.00,now() - interval '1 minute',false)
on conflict (sale_id) do nothing;