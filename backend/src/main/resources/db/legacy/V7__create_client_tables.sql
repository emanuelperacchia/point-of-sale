-- Clients table for POS customer registration
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    document_type VARCHAR(20),
    document_number VARCHAR(50),
    email VARCHAR(150),
    phone VARCHAR(20),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for search performance
CREATE INDEX idx_clients_name ON clients(name);
CREATE INDEX idx_clients_document ON clients(document_number);
CREATE INDEX idx_clients_email ON clients(email);

-- Comments
COMMENT ON TABLE clients IS 'Clientes para asociar a ventas en el POS';
COMMENT ON COLUMN clients.document_type IS 'Tipo de documento: RUT, CI, PASSPORT';
COMMENT ON COLUMN clients.document_number IS 'Número de documento (RUT, CI, etc.)';
