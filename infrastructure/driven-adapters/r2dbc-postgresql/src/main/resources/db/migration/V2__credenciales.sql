-- HU3: credencial de acceso. Solo se guarda el hash bcrypt, nunca la clave.
-- Nullable para no romper filas creadas en HU1; un usuario sin hash no puede iniciar sesion.
ALTER TABLE usuario ADD COLUMN password_hash VARCHAR(100);
