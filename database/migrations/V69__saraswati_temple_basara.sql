-- V69__saraswati_temple_basara.sql
-- TRACK-154: the catalogue's "Saraswati Temple" row was recorded at Basrur, Karnataka.
-- The reviewed Google place is the Gnana Saraswati temple at Basara, Telangana, on the
-- Godavari, and the curator confirmed Basara is the intended place.
--
-- Updates the row by its fixed id so existing databases keep the same temple (and the
-- krithi and cycle rows that point at it). R__seed_09 carries the same values, so a fresh
-- database inserts Basara directly. The place id itself comes from R__seed_12.
-- Ref: application_documentation/04-database/schema.md

SET search_path TO public;

UPDATE temples
SET city = 'Basara',
    state = 'Telangana',
    nadi_tirtha = 'Godavari',
    notes = 'Gnana Saraswati temple on the banks of the Godavari at Basara.',
    updated_at = NOW()
WHERE id = '01930000-0000-7000-8000-00000000004d'
  AND name_normalized = 'saraswati temple'
  AND city = 'Basrur';
