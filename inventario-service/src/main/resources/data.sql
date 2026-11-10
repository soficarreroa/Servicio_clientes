-- INSERT IGNORE: no duplica productos al reiniciar (codigo es UNIQUE)
INSERT IGNORE INTO producto (codigo, nombre, descripcion, precio, stock, activo) VALUES
('PRD-001', 'Teclado mecánico',     'Teclado mecánico retroiluminado',  185000.00, 25, true),
('PRD-002', 'Mouse inalámbrico',    'Mouse óptico 2.4 GHz',              65000.00, 40, true),
('PRD-003', 'Monitor 24 pulgadas',  'Monitor Full HD IPS',              620000.00,  8, true),
('PRD-004', 'Auriculares Bluetooth','Auriculares con cancelación de ruido', 240000.00, 0, true),
('PRD-005', 'Cámara web HD',        'Cámara 1080p con micrófono',       130000.00, 15, false);
