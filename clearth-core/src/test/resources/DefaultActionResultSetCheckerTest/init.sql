create table TRADES
(
    TRADE_ID varchar(10),
    BUY_FIRM varchar(3),
    SELL_FIRM varchar(3),
    INSTRUMENT_ID varchar(10),
    QUANTITY int,
    PRICE decimal(20, 2),
    CURRENCY varchar(3)
);

insert into TRADES values ('0000000001', 'AAA', 'BBB', 'ABC0000001', 35, 45000.35, 'EUR');
insert into TRADES values ('0000000002', 'AAA', 'CCC', 'FGH0000009', 90, 72000.00, 'EUR');
insert into TRADES values ('0000000003', 'DDD', 'CCC', 'OPI0000005', 18, 12345.67, 'USD');
insert into TRADES values ('0000000004', 'DDD', 'EEE', 'LKJ0000004', 45, 68009.12, 'USD');
insert into TRADES values ('0000000005', 'DDD', 'FFF', 'UIO0000002', 13, 10000.00, 'CHF');
insert into TRADES values ('0000000006', 'KKK', 'WWW', 'DFG0000005', 73, 12000.50, 'LKR');
insert into TRADES values ('0000000007', 'WWW', 'ZZZ', 'ASD0000007', 3, 898330.50, 'QAR');
insert into TRADES values ('0000000008', 'III', 'OOO', 'ERT0000002', 83, 80090.50, 'NOK');
insert into TRADES values ('0000000009', 'UUU', 'OOO', 'DSA0000005', 46, 60090.54, 'AUD');
insert into TRADES values ('0000000010', 'UUU', 'TTT', 'TYU0000003', 98, 62620.00, 'AUD');
insert into TRADES values ('0000000011', 'PPP', 'GGG', 'JKL0000005', 48, 75394.00, 'MDL');
insert into TRADES values ('0000000012', 'GGG', 'KKK', 'JKL0000003', 59, 50000.00, 'KGS');
insert into TRADES values ('0000000013', 'GGG', 'WWW', 'WER0000006', 34, 50050.05, 'KGS');