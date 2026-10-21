CREATE TABLE car_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    car_id INT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_car_image_storage_key (storage_key),
    UNIQUE KEY uk_car_image_car_sort_order (car_id, sort_order),
    CONSTRAINT fk_car_image_car
        FOREIGN KEY (car_id) REFERENCES car (id) ON DELETE RESTRICT,
    CONSTRAINT chk_car_image_size CHECK (size_bytes > 0),
    CONSTRAINT chk_car_image_sort_order CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
