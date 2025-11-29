DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'btree_gist') THEN
        RAISE NOTICE 'Extension btree_gist no disponible, saltando constraints de exclusion';
        RETURN;
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'no_psychologist_overlap'
    ) THEN
        EXECUTE 'ALTER TABLE "appointments" ADD CONSTRAINT no_psychologist_overlap
            EXCLUDE USING gist (
                psychologist_id WITH =,
                tsrange(start_time, end_time, ''[)'') WITH &&
            )';
        RAISE NOTICE 'Constraint no_psychologist_overlap creado';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'no_patient_overlap'
    ) THEN
        EXECUTE 'ALTER TABLE "appointments" ADD CONSTRAINT no_patient_overlap
            EXCLUDE USING gist (
                patient_id WITH =,
                tsrange(start_time, end_time, ''[)'') WITH &&
            )';
        RAISE NOTICE 'Constraint no_patient_overlap creado';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'no_room_overlap'
    ) THEN
        EXECUTE 'ALTER TABLE "appointments" ADD CONSTRAINT no_room_overlap
            EXCLUDE USING gist (
                room_id WITH =,
                tsrange(start_time, end_time, ''[)'') WITH &&
            )';
        RAISE NOTICE 'Constraint no_room_overlap creado';
    END IF;
    
    RAISE NOTICE 'Todos los constraints de exclusion estan configurados';
END $$;
