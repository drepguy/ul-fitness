-- ============================================================
-- DATA RESET: Exercises, Aliases, Workouts, Workout Exercises, Sets
-- Target: MariaDB 11, database ul_fitness
-- user_id = 1 (ulrich@ulf.local)
-- Gym IDs: Thomas Sport Center = 1, All Inclusive Fitness = 2
-- ============================================================

-- -----------------------------------------------------------
-- 1. SEED ALL EXERCISES (original + new)
-- -----------------------------------------------------------
INSERT INTO exercises (id, name, category, kind, gym_id, icon_key, is_system, owner_id, created_at) VALUES
(1,  'Hackenschmidt',        'legs',  'machine',    2, 'leg_press',     TRUE, NULL, NOW(6)),
(2,  'Hip Thrust Machine',   'legs',  'machine',    2, 'hip_thrust',    TRUE, NULL, NOW(6)),
(3,  'Beinpresse horizontal','legs',  'machine',    2, 'leg_press',     TRUE, NULL, NOW(6)),
(4,  'Wadenpresse horizontal','legs', 'machine',    2, 'calf',          TRUE, NULL, NOW(6)),
(5,  'Beinpresse 45°',       'legs',  'machine',    2, 'leg_press',     TRUE, NULL, NOW(6)),
(6,  'Wadenpresse 45°',      'legs',  'machine',    2, 'calf',          TRUE, NULL, NOW(6)),
(7,  'Wadenheber sitzend',   'legs',  'machine',    2, 'calf',          TRUE, NULL, NOW(6)),
(8,  'Beinstrecker',         'legs',  'machine',    2, 'leg_ext',       TRUE, NULL, NOW(6)),
(9,  'Beinbeuger',           'legs',  'machine',    2, 'leg_curl',      TRUE, NULL, NOW(6)),
(10, 'Hyperextension',       'core',  'bodyweight', NULL, 'hyperext',  TRUE, NULL, NOW(6)),
(11, 'Brustpresse',          'push',  'machine',    1, 'chest_press',   TRUE, NULL, NOW(6)),
(12, 'Latzug',               'pull',  'cable',      1, 'lat_pull',      TRUE, NULL, NOW(6)),
(13, 'Rudern',               'pull',  'cable',      1, 'row',           TRUE, NULL, NOW(6)),
(14, 'Brustfly',             'push',  'cable',      1, 'chest_press',   TRUE, NULL, NOW(6)),
(15, 'Schulterpresse',       'push',  'machine',    1, 'shoulder_press', TRUE, NULL, NOW(6)),
(16, 'Seitheben',            'push',  'free_weight', 1, 'lateral_raise', TRUE, NULL, NOW(6)),
(17, 'Face Pulls',           'pull',  'cable',      1, 'face_pull',     TRUE, NULL, NOW(6)),
(18, 'Bizeps Hammer Curls',  'pull',  'free_weight', 1, 'bicep_curl',   TRUE, NULL, NOW(6)),
(19, 'Trizeps Skull Crush',  'push',  'free_weight', 1, 'triceps',      TRUE, NULL, NOW(6)),
(20, 'Bauch',                'core',  'machine',    1, 'ab_machine',    TRUE, NULL, NOW(6)),
(21, 'Beinbeuger liegend',   'legs',  'machine',    2, 'leg_curl',      TRUE, NULL, NOW(6)),
(22, 'Bauchmaschine',        'core',  'machine',    2, 'ab_machine',    TRUE, NULL, NOW(6)),
(23, 'Beinpresse',           'legs',  'machine',    1, 'leg_press',     TRUE, NULL, NOW(6)),
(24, 'Wadenmaschine',        'legs',  'machine',    1, 'calf',          TRUE, NULL, NOW(6)),
(25, 'Trizeps Kabelzug',     'push',  'cable',      1, 'triceps',       TRUE, NULL, NOW(6)),
(26, 'Bizeps Kabelzug',      'pull',  'cable',      1, 'bicep_curl',    TRUE, NULL, NOW(6)),
(27, 'Rudern Brustgestützt', 'pull',  'machine',    1, 'row',           TRUE, NULL, NOW(6)),
(28, 'Unterarme',            'other', 'free_weight', 1, 'dumbbell',     TRUE, NULL, NOW(6));

-- Reset auto_increment
ALTER TABLE exercises AUTO_INCREMENT = 29;

-- -----------------------------------------------------------
-- 2. INSERT ALIASES
-- -----------------------------------------------------------
INSERT INTO exercise_aliases (exercise_id, alias, created_at) VALUES
(1,  'Hackschmitt', NOW(6)),
(1,  'Hack Squat',  NOW(6)),
(11, 'Brust',       NOW(6)),
(14, 'Chest fly',   NOW(6)),
(3,  'Beinpresse',  NOW(6)),
(19, 'Skullcrusher', NOW(6));

-- ===========================================================
--  ALL INCLUSIVE FITNESS  (gym_id = 2)
-- ===========================================================

-- -----------------------------------------------------------
-- Workout 1: 2026-08-01
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-01 10:00:00', '2026-08-01 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Hackenschmidt
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hackenschmidt' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 35.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 35.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 12, 35.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 20.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 20.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 20.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 40.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 47.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 47.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 40.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 47.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 47.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Wadenpresse 45°
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse 45°' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 75.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 13, 85.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bauchmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauchmaschine' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 37.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 42.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Hyperextension
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hyperextension' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 8.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 2: 2026-08-05
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-05 10:00:00', '2026-08-05 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Beinpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinpresse horizontal' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 17, 87.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 95.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 95.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 20.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 13, 20.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Wadenpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse horizontal' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 80.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 105.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 11, 105.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 11, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Hyperextension
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hyperextension' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 0.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 3: 2026-08-08
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-08 10:00:00', '2026-08-08 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Hackenschmidt (warmup 10x0, 10x15, then 14x40, 10x45, 8x45)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hackenschmidt' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 0.00, TRUE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 15.00, TRUE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 14, 40.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 4, 10, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 5, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 25.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 55.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 55.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 52.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 52.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 11, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Wadenpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse horizontal' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 110.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 102.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 102.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Hyperextension
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hyperextension' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 4: 2026-08-12
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-12 10:00:00', '2026-08-12 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Hackenschmidt (warmup 10x0, 10x15, then 6x50, 12x45, 7x45)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hackenschmidt' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 0.00, TRUE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 15.00, TRUE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 6, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 4, 12, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 5, 7, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 25.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Wadenpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse horizontal' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 110.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 110.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 110.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bauchmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauchmaschine' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 20.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 15, 27.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Hyperextension
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hyperextension' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 0.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 2.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 5: 2026-08-15
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-15 10:00:00', '2026-08-15 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Beinpresse horizontal (warmup 10x40, 10x70, then 16x80)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinpresse horizontal' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 40.00, TRUE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 70.00, TRUE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 16, 80.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 30.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 30.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 30.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 6, 65.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 6, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Wadenpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse horizontal' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 117.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 110.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 110.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bauchmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauchmaschine' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 16, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 32.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 12, 32.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Hyperextension
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hyperextension' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 11, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 6: 2026-08-19
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-19 10:00:00', '2026-08-19 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Beinpresse horizontal (warmup 10x50, 10x75 — 105 abort, no working set)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinpresse horizontal' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 50.00, TRUE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 75.00, TRUE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 30.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 30.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 30.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 65.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 65.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Wadenpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse horizontal' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 117.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 117.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 117.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bauchmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauchmaschine' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 16, 27.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 15, 35.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 12, 35.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Beinstrecker (extra)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 65.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 7: 2026-08-22 (deload)
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-22 10:00:00', '2026-08-22 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Beinpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinpresse horizontal' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 72.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 65.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 42.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 42.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 37.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 37.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Wadenpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse horizontal' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 65.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bauchmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauchmaschine' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 25.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 12, 25.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 8: 2026-08-26 (deload)
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 2, '2026-08-26 10:00:00', '2026-08-26 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Beinpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinpresse horizontal' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 72.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 72.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Hip Thrust Machine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hip Thrust Machine' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 15.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Beinstrecker
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinstrecker' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 42.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 35.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Beinbeuger
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinbeuger' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 42.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 42.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Wadenpresse horizontal
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenpresse horizontal' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 72.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 72.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bauchmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauchmaschine' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 27.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 32.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- ===========================================================
--  THOMAS SPORT CENTER  (gym_id = 1)
-- ===========================================================

-- -----------------------------------------------------------
-- Workout 9: 2026-07-10
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-07-10 10:00:00', '2026-07-10 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Beinpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 73.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 82.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 6, 38.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 38.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 6, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Wadenmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenmaschine' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 91.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Bauch
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauch' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 32.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 34.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Trizeps Skull Crush
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Skull Crush' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 2.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 10. Hyperextension
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hyperextension' LIMIT 1), 9);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 0.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 10: 2026-07-17
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-07-17 10:00:00', '2026-07-17 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Beinpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Beinpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 38.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 38.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 7, 60.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 52.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bauch
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauch' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 32.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Wadenmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenmaschine' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 100.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Hyperextension
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Hyperextension' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 0.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 11: 2026-07-21
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-07-21 10:00:00', '2026-07-21 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Brustpresse (first block)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 36.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 32.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 7, 60.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 52.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Brustpresse (second block)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 36.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 32.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 32.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 27.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 18.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 18.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Bauch
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bauch' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 32.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 27.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 27.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Wadenmaschine
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Wadenmaschine' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Trizeps Skull Crush
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Skull Crush' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 0.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 1.25, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 1.25, FALSE, NULL, FALSE, NULL, NOW(6));

-- 10. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 9);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 7.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 12: 2026-08-02
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-02 10:00:00', '2026-08-02 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 43.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 6, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 38.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 55.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 27.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 27.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 23.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 6, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 6, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 9.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 9.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 9.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 7.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Trizeps Kabelzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Kabelzug' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 18.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 23.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 36.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 36.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 13: 2026-08-06
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-06 10:00:00', '2026-08-06 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 6, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 38.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 18, 9.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 11, 13.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Trizeps Kabelzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Kabelzug' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 15, 23.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 23.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Bizeps Kabelzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Kabelzug' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 18, 27.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 36.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 36.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 14: 2026-08-09
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-09 10:00:00', '2026-08-09 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 6, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 50.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 32.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 32.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 27.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 12, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 14.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 6, 9.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Trizeps Kabelzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Kabelzug' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 23.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 23.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 23.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Bizeps Kabelzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Kabelzug' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 32.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 32.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 32.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 15: 2026-08-13
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-13 10:00:00', '2026-08-13 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 6, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 7, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 29.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 27.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 6, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 14.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Trizeps Skull Crush (bar only)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Skull Crush' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 0.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 0.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 16: 2026-08-16
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-16 10:00:00', '2026-08-16 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 6, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 32.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 14.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Trizeps Skull Crush
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Skull Crush' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 15, 2.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 2.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 17: 2026-08-20
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-20 10:00:00', '2026-08-20 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 7, 36.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 50.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 32.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 65.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 9, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 18.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 12, 14.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Trizeps Skull Crush
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Skull Crush' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 16, 2.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 2.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 18: 2026-08-23 (deload)
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-23 10:00:00', '2026-08-23 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 29.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 23.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 23.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 12, 42.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 42.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 9.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 9.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 8, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Trizeps Skull Crush (bar only)
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Skull Crush' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 14, 0.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 0.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 7.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 7.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Unterarme
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Unterarme' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 11, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 11, 7.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 10. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 9);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 32.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- -----------------------------------------------------------
-- Workout 19: 2026-08-27
-- -----------------------------------------------------------
INSERT INTO workouts (user_id, gym_id, started_at, ended_at, notes, created_at)
VALUES (1, 1, '2026-08-27 10:00:00', '2026-08-27 11:30:00', NULL, NOW(6));
SET @wid = LAST_INSERT_ID();

-- 1. Brustpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustpresse' LIMIT 1), 0);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 41.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 41.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 2. Latzug
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Latzug' LIMIT 1), 1);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 7, 54.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 6, 54.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 3. Schulterpresse
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Schulterpresse' LIMIT 1), 2);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 9, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 8, 34.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 34.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 4. Rudern Brustgestützt
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Rudern Brustgestützt' LIMIT 1), 3);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 9, 57.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 57.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 5. Face Pulls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Face Pulls' LIMIT 1), 4);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 14, 14.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 8, 18.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 6. Seitheben
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Seitheben' LIMIT 1), 5);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 3, 10, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 7. Trizeps Skull Crush
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Trizeps Skull Crush' LIMIT 1), 6);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 2.50, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 13, 2.50, FALSE, NULL, FALSE, NULL, NOW(6));

-- 8. Bizeps Hammer Curls
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Bizeps Hammer Curls' LIMIT 1), 7);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 13, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 10.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 9. Unterarme
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Unterarme' LIMIT 1), 8);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 10.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 12, 5.00, FALSE, NULL, FALSE, NULL, NOW(6));

-- 10. Brustfly
INSERT INTO workout_exercises (workout_id, exercise_id, order_idx)
VALUES (@wid, (SELECT id FROM exercises WHERE name = 'Brustfly' LIMIT 1), 9);
SET @weid = LAST_INSERT_ID();
INSERT INTO sets (workout_exercise_id, set_no, reps, weight_kg, is_warmup, rpe, is_failure, note, created_at) VALUES
(@weid, 1, 10, 45.00, FALSE, NULL, FALSE, NULL, NOW(6)),
(@weid, 2, 6, 45.00, FALSE, NULL, FALSE, NULL, NOW(6));
