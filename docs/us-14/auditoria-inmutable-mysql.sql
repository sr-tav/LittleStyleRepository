-- US-14 / RNF-08: impide modificar o borrar filas de auditoría en MySQL.
-- Ejecutar una vez con una cuenta de despliegue autorizada, después de crear auditoria_admin.
-- En producción, la cuenta de ejecución de la aplicación tampoco debe tener permisos DROP/TRIGGER.

DELIMITER //

CREATE TRIGGER auditoria_admin_no_update
BEFORE UPDATE ON auditoria_admin
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Los registros de auditoria administrativa son inmutables';
END//

CREATE TRIGGER auditoria_admin_no_delete
BEFORE DELETE ON auditoria_admin
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Los registros de auditoria administrativa son inmutables';
END//

DELIMITER ;

-- H2 no ejecuta estos triggers MySQL. En el perfil local se limita la escritura al repositorio
-- append-only y la manipulación de datos se detecta al verificar la cadena SHA-256.
