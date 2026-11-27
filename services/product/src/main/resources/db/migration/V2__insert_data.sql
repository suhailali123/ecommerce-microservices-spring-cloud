INSERT INTO category (id, description, name)
VALUES
    (nextval('category_seq'), 'Computers, laptops, and accessories', 'Computer & Peripherals'),
    (nextval('category_seq'), 'Monitors and display panels', 'Displays'),
    (nextval('category_seq'), 'Keyboards, mice, and input devices', 'Input Devices');

INSERT INTO product (id, available_quantity, description, name, price, category_id)
VALUES
    (nextval('product_seq'), 10, 'Mechanical RGB Gaming Keyboard', 'Keychron K2 Pro', 99.99, (SELECT id FROM category WHERE name = 'Input Devices')),
    (nextval('product_seq'), 15, 'Wireless Ergonomic Mouse', 'Logitech MX Master 3S', 89.50, (SELECT id FROM category WHERE name = 'Input Devices')),
    (nextval('product_seq'), 8, '27-inch 4K IPS USB-C Monitor', 'Dell UltraSharp U2723QE', 479.00, (SELECT id FROM category WHERE name = 'Displays')),
    (nextval('product_seq'), 20, '1TB NVMe PCIe Gen4 SSD', 'Samsung 990 Pro 1TB', 119.00, (SELECT id FROM category WHERE name = 'Computer & Peripherals'));