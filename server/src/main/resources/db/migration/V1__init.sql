-- UL Fitness V1 — gyms, exercises (+aliases), workouts, templates (spec 0.4.3)
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6)
);
CREATE TABLE gyms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NULL,
  name VARCHAR(120) NOT NULL,
  city VARCHAR(120) NULL,
  is_system BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6),
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_gym_owner_name(owner_id, name)
);
CREATE TABLE exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NULL,
  gym_id BIGINT NULL,
  name VARCHAR(120) NOT NULL,
  category ENUM('push','pull','legs','core','full','cardio','other') NOT NULL,
  kind ENUM('free_weight','machine','cable','bodyweight','other') NOT NULL DEFAULT 'free_weight',
  icon_key VARCHAR(40) NOT NULL DEFAULT 'dumbbell',
  is_system BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6),
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
  UNIQUE KEY uq_exercise_owner_gym_name(owner_id, gym_id, name)
);
CREATE TABLE exercise_aliases (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exercise_id BIGINT NOT NULL,
  alias VARCHAR(120) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6),
  FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE,
  UNIQUE KEY uq_alias(alias, exercise_id),
  INDEX idx_alias(alias)
);
CREATE TABLE workouts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  gym_id BIGINT NULL,
  started_at DATETIME(6) NOT NULL,
  ended_at DATETIME(6) NULL,
  notes TEXT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE SET NULL,
  INDEX idx_workouts_user_started(user_id, started_at),
  INDEX idx_workouts_gym(gym_id)
);
CREATE TABLE workout_exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workout_id BIGINT NOT NULL,
  exercise_id BIGINT NOT NULL,
  order_idx INT NOT NULL,
  FOREIGN KEY (workout_id) REFERENCES workouts(id) ON DELETE CASCADE,
  FOREIGN KEY (exercise_id) REFERENCES exercises(id)
);
CREATE TABLE sets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workout_exercise_id BIGINT NOT NULL,
  set_no INT NOT NULL,
  reps INT NOT NULL CHECK (reps >= 0),
  weight_kg DECIMAL(5,2) NOT NULL CHECK (weight_kg >= 0),
  is_warmup BOOLEAN NOT NULL DEFAULT FALSE,
  rpe TINYINT NULL CHECK (rpe BETWEEN 1 AND 10),
  is_failure BOOLEAN NOT NULL DEFAULT FALSE,
  note TEXT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6),
  FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises(id) ON DELETE CASCADE
);
CREATE TABLE workout_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  gym_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6),
  updated_at DATETIME(6) NOT NULL DEFAULT NOW(6) ON UPDATE NOW(6),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
  UNIQUE KEY uq_template_user_gym_name(user_id, gym_id, name)
);
CREATE TABLE workout_template_exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  exercise_id BIGINT NOT NULL,
  order_idx INT NOT NULL,
  default_sets INT NOT NULL DEFAULT 3,
  default_reps INT NULL,
  default_weight_kg DECIMAL(5,2) NULL,
  FOREIGN KEY (template_id) REFERENCES workout_templates(id) ON DELETE CASCADE,
  FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
);
-- Seed gyms
INSERT INTO gyms(owner_id, name, is_system) VALUES (NULL,'Thomas Sport Center',TRUE),(NULL,'All Inclusive Fitness',TRUE);
-- Seed exercises (canonical, icon_key)
INSERT INTO exercises(owner_id, gym_id, name, category, kind, icon_key, is_system) VALUES
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Hackenschmidt','legs','machine','leg_press',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Hip Thrust Machine','legs','machine','hip_thrust',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Beinpresse horizontal','legs','machine','leg_press',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Wadenpresse horizontal','legs','machine','calf',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Beinpresse 45°','legs','machine','leg_press',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Wadenpresse 45°','legs','machine','calf',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Wadenheber sitzend','legs','machine','calf',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Beinstrecker','legs','machine','leg_ext',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Beinbeuger','legs','machine','leg_curl',TRUE),
 (NULL,NULL,'Hyperextension','core','bodyweight','hyperext',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Brustpresse','push','machine','chest_press',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Latzug','pull','cable','lat_pull',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Rudern','pull','cable','row',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Brustfly','push','cable','chest_press',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Schulterpresse','push','machine','shoulder_press',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Seitheben','push','free_weight','lateral_raise',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Face Pulls','pull','cable','face_pull',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Bizeps Hammer Curls','pull','free_weight','bicep_curl',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Trizeps Skull Crush','push','free_weight','triceps',TRUE),
 (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Bauch','core','machine','ab_machine',TRUE);
-- Aliases
INSERT INTO exercise_aliases(exercise_id, alias) VALUES
 ((SELECT id FROM exercises WHERE name='Hackenschmidt' LIMIT 1),'Hackschmitt'),
 ((SELECT id FROM exercises WHERE name='Hackenschmidt' LIMIT 1),'Hack Squat'),
 ((SELECT id FROM exercises WHERE name='Brustpresse' LIMIT 1),'Brust'),
 ((SELECT id FROM exercises WHERE name='Brustfly' LIMIT 1),'Chest fly'),
 ((SELECT id FROM exercises WHERE name='Beinpresse horizontal' LIMIT 1),'Beinpresse');
