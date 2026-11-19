-- Base de datos de InventarioMovil (Hito 4)
-- Ejecutar una sola vez en phpMyAdmin (pestaña SQL) o en la consola de MySQL.

CREATE DATABASE IF NOT EXISTS db_inventario CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_inventario;

DROP TABLE IF EXISTS detalle_venta;
DROP TABLE IF EXISTS venta;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS comerciante;

CREATE TABLE comerciante (
    id_comerciante INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    apellido VARCHAR(80) NOT NULL,
    nombre_puesto VARCHAR(100),
    telefono VARCHAR(20),
    codigo_afiliado VARCHAR(20) UNIQUE,
    ruta_qr_cobro VARCHAR(255),
    -- Datos para el inicio de sesión (login)
    usuario VARCHAR(40) NOT NULL UNIQUE,
    clave_hash VARCHAR(255) NOT NULL,      -- la clave nunca se guarda en texto, solo su hash
    token VARCHAR(64),                     -- se genera al iniciar sesión y se borra al cerrarla
    token_vence DATETIME,                  -- el token deja de servir a esta hora (30 min sin usar la app)
    intentos_fallidos INT NOT NULL DEFAULT 0,
    bloqueado_hasta DATETIME               -- se llena después de 5 claves incorrectas seguidas
) ENGINE=InnoDB;

CREATE TABLE categoria (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    descripcion VARCHAR(200)
) ENGINE=InnoDB;

CREATE TABLE producto (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(200),
    precio_compra DECIMAL(10,2) NOT NULL,
    precio_venta DECIMAL(10,2) NOT NULL,
    stock_actual INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 0,
    unidad_medida VARCHAR(20) NOT NULL DEFAULT 'Unidad',
    id_categoria INT NOT NULL,
    id_comerciante INT NOT NULL DEFAULT 1,
    fecha_registro DATETIME NOT NULL,
    FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria) ON DELETE RESTRICT,
    FOREIGN KEY (id_comerciante) REFERENCES comerciante(id_comerciante)
) ENGINE=InnoDB;

CREATE TABLE venta (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora DATETIME NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    metodo_pago ENUM('EFECTIVO','QR','OTRO') NOT NULL,
    estado_pago ENUM('PAGADO','PENDIENTE') NOT NULL,
    id_comerciante INT NOT NULL DEFAULT 1,
    FOREIGN KEY (id_comerciante) REFERENCES comerciante(id_comerciante)
) ENGINE=InnoDB;

CREATE TABLE detalle_venta (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_venta) REFERENCES venta(id_venta) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Usuario de prueba:  usuario = admin   clave = admin123
-- El texto largo es el hash de "admin123" generado con password_hash() de PHP.
INSERT INTO comerciante (nombre, apellido, nombre_puesto, telefono, codigo_afiliado, usuario, clave_hash)
VALUES ('Marco', 'Calle', 'Puesto 16 de Julio', '70000000', 'FEC-0001', 'admin',
        '$2y$10$udeoQ1R3oRARNRzgFEgnQO5rRZotYFGjvuVuIwUCpPj2bPO1x4/HS');

INSERT INTO categoria (nombre, descripcion) VALUES
('Abarrotes', 'Arroz, azúcar, fideo, aceite'),
('Bebidas', 'Gaseosas, jugos, agua'),
('Lácteos', 'Leche, yogurt, queso'),
('Snacks', 'Galletas, dulces'),
('Limpieza', 'Detergente, jabón, lavandina');

INSERT INTO producto (nombre, descripcion, precio_compra, precio_venta, stock_actual, stock_minimo, unidad_medida, id_categoria, fecha_registro) VALUES
('Coca-Cola 500 ml', '', 6.00, 8.00, 15, 5, 'Unidad', 2, NOW()),
('Pepsi 500 ml', '', 5.00, 7.00, 9, 5, 'Unidad', 2, NOW()),
('Arroz 1 kg', '', 9.00, 12.00, 3, 5, 'Kg', 1, NOW()),
('Aceite 1 L', '', 11.00, 14.00, 0, 3, 'Litro', 1, NOW()),
('Azúcar 1 kg', '', 6.00, 8.00, 22, 5, 'Kg', 1, NOW()),
('Leche 1 L', '', 5.50, 7.00, 2, 4, 'Litro', 3, NOW()),
('Galletas', '', 2.00, 3.00, 0, 6, 'Paquete', 4, NOW());
