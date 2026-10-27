-- DROP SCHEMA shopping_demo;

CREATE SCHEMA IF NOT EXISTS shopping_demo AUTHORIZATION pg_database_owner;

-- DROP FUNCTION update_timestamp();

CREATE OR REPLACE FUNCTION update_timestamp()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$function$
;

-- DROP SEQUENCE cart_items_id_seq;

CREATE SEQUENCE cart_items_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE carts_id_seq;

CREATE SEQUENCE carts_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE categories_id_seq;

CREATE SEQUENCE categories_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE order_items_id_seq;

CREATE SEQUENCE order_items_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE orders_id_seq;

CREATE SEQUENCE orders_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE products_id_seq;

CREATE SEQUENCE products_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;-- carts definition

-- Drop table

-- DROP TABLE carts;

CREATE TABLE carts (
	id serial4 NOT NULL,
	total_price numeric(10, 2) DEFAULT 0 NOT NULL,
	created_at timestamptz DEFAULT now() NULL,
	created_by varchar(50) NULL,
	updated_at timestamptz DEFAULT now() NULL,
	CONSTRAINT carts_id_not_null NOT NULL id,
	CONSTRAINT carts_pkey PRIMARY KEY (id),
	CONSTRAINT carts_total_price_not_null NOT NULL total_price
);
CREATE INDEX idx_carts_created_at ON carts USING btree (created_at);
CREATE INDEX idx_carts_created_by ON carts USING btree (created_by);

-- Table Triggers

create trigger set_carts_timestamp before
update
    on
    carts for each row execute function update_timestamp();


-- categories definition

-- Drop table

-- DROP TABLE categories;

CREATE TABLE categories (
	id serial4 NOT NULL,
	"name" varchar(50) NOT NULL,
	description varchar(100) NULL,
	created_at timestamptz DEFAULT now() NULL,
	created_by varchar(50) NULL,
	updated_at timestamptz DEFAULT now() NULL,
	updated_by varchar(50) NULL,
	deleted bool DEFAULT false NULL,
	CONSTRAINT categories_id_not_null NOT NULL id,
	CONSTRAINT categories_name_not_null NOT NULL name,
	CONSTRAINT categories_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_categories_deleted ON categories USING btree (deleted);
CREATE INDEX idx_categories_name_deleted ON categories USING btree (name, deleted);
CREATE UNIQUE INDEX idx_categories_name_unique ON categories USING btree (name);

-- Table Triggers

create trigger set_categories_timestamp before
update
    on
    categories for each row execute function update_timestamp();


-- orders definition

-- Drop table

-- DROP TABLE orders;

CREATE TABLE orders (
	id serial4 NOT NULL,
	cart_id int4 NULL,
	created_at timestamptz DEFAULT now() NULL,
	created_by varchar(50) NULL,
	updated_at timestamptz DEFAULT now() NULL,
	total numeric(10, 2) NOT NULL,
	CONSTRAINT orders_id_not_null NOT NULL id,
	CONSTRAINT orders_pkey PRIMARY KEY (id),
	CONSTRAINT orders_total_not_null NOT NULL total,
	CONSTRAINT orders_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE SET NULL
);
CREATE INDEX idx_orders_created_at ON orders USING btree (created_at);
CREATE INDEX idx_orders_created_by ON orders USING btree (created_by);
CREATE INDEX idx_orders_total ON orders USING btree (total);
CREATE INDEX idx_orders_user_date ON orders USING btree (created_by, created_at);


-- products definition

-- Drop table

-- DROP TABLE products;

CREATE TABLE products (
	id serial4 NOT NULL,
	category_id int4 NULL,
	"name" varchar(50) NOT NULL,
	sku varchar(50) NOT NULL,
	description varchar(100) NULL,
	price numeric(10, 2) NOT NULL,
	stock int4 DEFAULT 0 NOT NULL,
	weight_kg numeric(10, 2) NULL,
	created_at timestamptz DEFAULT now() NULL,
	created_by varchar(50) NULL,
	updated_at timestamptz DEFAULT now() NULL,
	updated_by varchar(50) NULL,
	deleted bool DEFAULT false NULL,
	CONSTRAINT products_id_not_null NOT NULL id,
	CONSTRAINT products_name_not_null NOT NULL name,
	CONSTRAINT products_pkey PRIMARY KEY (id),
	CONSTRAINT products_price_not_null NOT NULL price,
	CONSTRAINT products_sku_not_null NOT NULL sku,
	CONSTRAINT products_stock_not_null NOT NULL stock,
	CONSTRAINT products_category_id_fkey FOREIGN KEY (category_id) REFERENCES categories(id)
);
CREATE INDEX idx_products_category_active ON products USING btree (category_id, deleted);
CREATE INDEX idx_products_category_id ON products USING btree (category_id);
CREATE INDEX idx_products_deleted ON products USING btree (deleted);
CREATE UNIQUE INDEX idx_products_sku ON products USING btree (sku);

-- Table Triggers

create trigger set_products_timestamp before
update
    on
    products for each row execute function update_timestamp();


-- cart_items definition

-- Drop table

-- DROP TABLE cart_items;

CREATE TABLE cart_items (
	id serial4 NOT NULL,
	cart_id int4 NULL,
	product_id int4 NULL,
	quantity int4 DEFAULT 1 NOT NULL,
	price numeric(10, 2) NOT NULL,
	CONSTRAINT cart_items_id_not_null NOT NULL id,
	CONSTRAINT cart_items_pkey PRIMARY KEY (id),
	CONSTRAINT cart_items_price_not_null NOT NULL price,
	CONSTRAINT cart_items_quantity_not_null NOT NULL quantity,
	CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id),
	CONSTRAINT cart_items_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
	CONSTRAINT cart_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE INDEX idx_cart_items_cart_id ON cart_items USING btree (cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items USING btree (product_id);

-- DROP FUNCTION update_cart_total();

CREATE OR REPLACE FUNCTION update_cart_total()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
  UPDATE carts
  SET total_price = (
    SELECT SUM(ci.quantity * p.price)
    FROM cart_items ci
    JOIN products p ON ci.product_id = p.id
    WHERE ci.cart_id = NEW.cart_id
  )
  WHERE id = NEW.cart_id;
  RETURN NEW;
END;
$function$
;

-- Table Triggers

create trigger set_cart_total after
insert
    or
delete
    or
update
    on
    cart_items for each row execute function update_cart_total();


-- order_items definition

-- Drop table

-- DROP TABLE order_items;

CREATE TABLE order_items (
	id serial4 NOT NULL,
	order_id int4 NULL,
	product_id int4 NULL,
	quantity int4 DEFAULT 1 NOT NULL,
	price numeric(10, 2) NOT NULL,
	CONSTRAINT order_items_id_not_null NOT NULL id,
	CONSTRAINT order_items_pkey PRIMARY KEY (id),
	CONSTRAINT order_items_price_not_null NOT NULL price,
	CONSTRAINT order_items_quantity_not_null NOT NULL quantity,
	CONSTRAINT uq_order_product UNIQUE (order_id, product_id),
	CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
	CONSTRAINT order_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE INDEX idx_order_items_order_id ON order_items USING btree (order_id);
CREATE INDEX idx_order_items_product_id ON order_items USING btree (product_id);

-- DROP FUNCTION update_order_total();

CREATE OR REPLACE FUNCTION update_order_total()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
  UPDATE orders
  SET total = (
    SELECT SUM(oi.quantity * oi.price)
    FROM order_items oi
    WHERE oi.order_id = NEW.order_id
  )
  WHERE id = NEW.order_id;
  RETURN NEW;
END;
$function$
;

-- Table Triggers

create trigger set_order_total after
insert
    or
delete
    or
update
    on
    order_items for each row execute function update_order_total();




