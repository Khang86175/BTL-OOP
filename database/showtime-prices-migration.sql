-- MANUAL QA PLAN ONLY. Not executed by the application or tests.
-- Verify connection/account and back up cineticketp3qa before running any ALTER.
SELECT DATABASE(), CURRENT_USER(), VERSION();
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='cineticketp3qa' AND TABLE_NAME='showtimes'
  AND COLUMN_NAME IN ('base_price','normal_price','vip_price');
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA='cineticketp3qa' AND TABLE_NAME='showtimes';

-- PHASE A: run only if BOTH new columns and chk_showtimes_prices are absent.
-- If one already exists, stop and review definitions; this script is not a rerunnable migration runner.
ALTER TABLE cineticketp3qa.showtimes
 ADD COLUMN normal_price DECIMAL(12,2) NULL,
 ADD COLUMN vip_price DECIMAL(12,2) NULL,
 ADD CONSTRAINT chk_showtimes_prices CHECK (
  (normal_price IS NULL OR normal_price > 0) AND (vip_price IS NULL OR vip_price > 0)
 );

SELECT id, movie_id, room_id, base_price, normal_price, vip_price, status
FROM cineticketp3qa.showtimes ORDER BY id;

-- PHASE B: NO AUTOMATIC BACKFILL. Approve prices per showtime with the group.
-- normal_price may reuse a POSITIVE base_price after approval; no VIP surcharge is assumed.
-- VIP must be explicitly chosen; mapping both prices to base_price also requires approval.
-- UPDATE cineticketp3qa.showtimes SET normal_price=base_price
-- WHERE normal_price IS NULL AND base_price > 0;
-- Set approved vip_price per showtime using Admin forms for future schedules,
-- or reviewed SQL for historical schedules; never modify Booking price_at_booking.

SELECT id, base_price, normal_price, vip_price
FROM cineticketp3qa.showtimes
WHERE normal_price IS NULL OR vip_price IS NULL OR normal_price <= 0 OR vip_price <= 0;

-- PHASE C: only after all environments/rows are migrated, coordinate JPA nullable=false in a later release.
-- ALTER TABLE cineticketp3qa.showtimes
--  MODIFY normal_price DECIMAL(12,2) NOT NULL,
--  MODIFY vip_price DECIMAL(12,2) NOT NULL;
-- KEEP base_price and all IDs. Do not DROP it until every Booking caller has switched.
