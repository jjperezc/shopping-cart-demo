# Shopping Cart Demo

A small server-rendered shopping cart web application written in **Clojure**. Customers browse a product catalog, fill a cart and check out; an admin manages the product catalog, including bulk imports from CSV. Data lives in **PostgreSQL**, and the whole stack runs with a single `docker compose up`.

## Features

**Customers**
- Browse the product catalog as a card grid, with search by name, SKU or description
- Add products to a cart, remove items, see subtotals and a running total
- Check out: the cart becomes an order and a fresh, empty cart is created automatically
- Order history, with the items of each order

**Admin**
- Product list with search
- Add, edit and delete products (delete is a *soft delete*; rows stay in the database)
- Category dropdown fed from the categories table, which was created from scratch according to the CSV file provided
- Bulk product upload from a CSV file, with a per-row result report and a log file

**Backend**
- Role-based access: public, admin-only and customer-only route groups
- Cookie sessions
- Parameterized SQL everywhere, HTML-escaped output and server-side input validation (for invalid texts)
- Database triggers keep `updated_at` and the cart/order totals in sync

## Approach
The implementation of this application was to use Conjure as main language for Backend and Frontend as functional languages was one of the requirements of the job application. Although I had little knowledge of functional programming, I had never used it before on any project so this one was my first experience with Clojure.

The approach to implement this project was the following:

1. Define database on a diagram with relations and key fields, then complement with required attributes and define types. (See diagram on doc folder).
2. Define page mapping for admin and user. How would these pages communicate and how navigation is going to be.
2. Create initial git repository and configuration.
3. Create login, authentication functionality and "admin" pages.
4. Add features to add/edit/delete products.
5. Include bulk files and validation.
6. Create pages pages for users. Include product catalog, add items to cart and place orders.
7. Add additional featires like remove itesm form cart and view order history.
8. Add styling to the shopping cart and fix issues with sessions.
9. Create docker configuration to build project.

During the implemenattion, AI was used continusly to improve some of the code. The main usage was:
- Fix of errors related to the session and wrapping of session.
- Create validation for product data, included code injection.
- Add style to the project.
- Fix errors related to the docker configuration to build the project.

## Decisions and Assumptions
- As there was a need to create products and a shopping cart, 2 different profiles were needed: admin and user. To make it simple I created and auth file to store users and passwords, but this for sure would need to be changed (Ex. store users in database, encript password and make use of https)
- Products need to have a category, so I created a Category table which is created along the database. Category should have its own CRUD, which was not implemented for this demo.
- Validated bulk files, as this was needed to avoid sql and code injection, as well as invalid data.
- Added control fields to almost all tables, to store who and when data was created and/or updated.
- Made usage of triggers to update modification date, as well as update/store the total for cart and orders every time the cart items were updated. 
- Created a separated css style sheet and did not included it on any clojure file, this to have a more clean code.
- When bulking products, I added a page to keep track of the result for every line. This is also exported to the log directory were you can find a log with the results depending on the date and time for each upload.
- Created README file with AI, and modified file to add/remove comments.

## Tech stack

| Area | Technology |
|------|------------|
| Language | Clojure 1.11.1 |
| Web | Ring 1.10.0 (Jetty), Compojure 1.7.0 |
| HTML | Hiccup 1.0.5 (server-side rendering) |
| Database access | next.jdbc 1.2.659, PostgreSQL JDBC driver 42.7.3 |
| CSV | org.clojure/data.csv 1.1.0 |
| Database | PostgreSQL 18 |
| Packaging | Leiningen, Docker, Docker Compose |

## Quick start with Docker

Requirements: Docker with the Compose plugin.

```bash
docker compose up --build
```

Then open <http://localhost:3030>.

This starts two containers:

- **db**: PostgreSQL 18. On its first start it runs every script in `db/init/` (see [Database](#database)).
- **app**: the Clojure application, started once the database reports healthy.

Useful commands:

```bash
docker compose logs -f app     # follow the application logs
docker compose down            # stop, keeping the data
docker compose down -v         # stop AND delete the database (init scripts run again next time)
```

### Demo users

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | Admin |
| `user1` | `user123` | Customer |
| `user2` | `user234` | Customer |
| `user3` | `user345` | Customer |

These are hardcoded in `src/shopping_cart_demo/auth.clj` for demo purposes only. See [Known limitations](#known-limitations).

## Running locally without Docker

Requirements: JDK 17+, [Leiningen](https://leiningen.org/), and a running PostgreSQL.

1. Create the database and load the scripts:

   ```bash
   psql -U postgres -c "CREATE DATABASE shopping_demo"
   psql -U postgres -d shopping_demo -f db/init/01-schema.sql -f db/init/02-seed.sql
   ```

2. Make sure the `logs/` directory exists (bulk-upload logs are written there):

   ```bash
   mkdir -p logs
   ```

3. Start the app, pointing it at your database:

   ```bash
   # bash / zsh
   DB_NAME=shopping_demo lein run

   # PowerShell
   $env:DB_NAME = "shopping_demo"; lein run
   ```

   The app listens on <http://localhost:3000> unless `PORT` is set.

## Configuration

All settings are environment variables. The defaults target a local PostgreSQL; `docker-compose.yml` overrides them for the containers.

| Variable | Default (code) | In Docker Compose | Description |
|----------|----------------|-------------------|-------------|
| `PORT` | `3000` | `3030` | HTTP port of the app |
| `DB_HOST` | `localhost` | `db` | Database host |
| `DB_PORT` | `5432` | `5432` | Database port |
| `DB_NAME` | `shopping` | `shopping_demo` | Database name |
| `DB_USER` | `postgres` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | `${DB_PASSWORD:-postgres}` | Database password |

To set a real password for Docker, create a `.env` file next to `docker-compose.yml` (and keep it out of git):

```
DB_PASSWORD=choose-something-strong
```

To reach the containerized database from your host (for example with a SQL client), uncomment the `ports` lines of the `db` service in `docker-compose.yml`. They map port `5433`, so they don't clash with a local PostgreSQL.

## Database

The database is created from the scripts in `db/init/`, which PostgreSQL runs **once, in alphabetical order, the first time the data volume is empty**:

| File | Purpose |
|------|---------|
| `db/init/01-schema.sql` | Tables, indexes, sequences, functions and triggers |
| `db/init/02-seed.sql` | Sample data: 18 product categories |

If you change these files, run `docker compose down -v` so they are applied again.

### Tables

| Table | Description |
|-------|-------------|
| `categories` | Product categories (soft delete via `deleted`) |
| `products` | Catalog items: SKU, price, stock, weight, category (soft delete via `deleted`) |
| `carts` | One cart per shopping session, with a `total_price` maintained by a trigger |
| `cart_items` | Items in a cart; `(cart_id, product_id)` is unique, so adding the same product again increases its quantity |
| `orders` | Completed checkouts, with the total copied from the cart |
| `order_items` | The items of each order |

### Triggers

| Trigger | Table | Effect |
|---------|-------|--------|
| `set_carts_timestamp`, `set_categories_timestamp`, `set_products_timestamp` | carts, categories, products | Refresh `updated_at` on every update |
| `set_cart_total` | `cart_items` | Recalculates `carts.total_price` on insert, update and delete |
| `set_order_total` | `order_items` | Recalculates the order total |

## Bulk product upload

Admins can import products from **Products → Bulk Upload**. The file must be a CSV with this header row:

```csv
name,sku,description,category,price,stock,weight_kg
Running Shoes,RS-001,Lightweight running shoes,Footwear,89.99,150,0.35
Organic Coffee,CB-010,"Single origin, medium roast",Food & Beverage,18.75,500,1.0
```

Rules:

- `category` is the category **name** (matched case-insensitively). It is looked up and only its id is stored. An unknown name rejects that row.
- `name`, `sku`, `category`, `price` and `stock` are required; `description` and `weight_kg` are optional.
- `sku` may only contain letters, digits, `.`, `_` and `-`.
- `price` must be between 0 and 1,000,000; `stock` must be a whole number between 0 and 1,000,000; `weight_kg`, if given, must be a positive number.
- Every row is validated and inserted independently, so one bad row doesn't stop the rest. The result page lists each row as success or error.
- Every row result is also appended to a timestamped file in `logs/` (a Docker volume when running in containers).

Current demo uses CSV file provided with the instructions and downloaded on October 19th 2026. 

## Routes

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/` | Public | Login page |
| POST | `/login` | Public | Sign in (admins go to `/products`, customers to `/catalog`) |
| GET | `/logout` | Public | End the session |
| GET | `/products` | Admin | Product list (`?q=` to search) |
| GET, POST | `/products/add` | Admin | Add a product |
| GET, POST | `/products/edit/:id` | Admin | Edit a product |
| POST | `/products/delete/:id` | Admin | Soft-delete a product |
| GET, POST | `/products/bulk` | Admin | Bulk CSV upload |
| GET | `/catalog` | Customer | Product catalog (`?q=` to search) |
| GET | `/cart` | Customer | Current cart |
| POST | `/cart/:id/add` | Customer | Add a product to the cart |
| POST | `/cart/item/:item-id/remove` | Customer | Remove an item from the cart |
| POST | `/cart/:id/checkout` | Customer | Place the order and start a new cart |
| GET | `/orders` | Customer | Order history |
| GET | `/orders/:id` | Customer | Items of one of your orders |
| GET | `/static/*` | Public | Static files (`resources/public`, e.g. `style.css`) |

Requests to a protected path without a session are redirected to the login page; a logged-in user of the wrong role gets a 403.

## Project structure

```
shopping-cart-demo/
├── Dockerfile                  # multi-stage build: lein uberjar -> slim JRE image
├── docker-compose.yml          # app + PostgreSQL
├── project.clj                 # Leiningen project and dependencies
├── db/init/
│   ├── 01-schema.sql           # database schema
│   └── 02-seed.sql             # sample categories
├── resources/public/
│   └── style.css               # shopping-cart theme
├── src/shopping_cart_demo/
│   ├── core.clj                # entry point and middleware stack
│   ├── routes.clj              # routes, access control and HTML pages
│   ├── db.clj                  # all SQL and the connection settings
│   ├── auth.clj                # demo users and login check
│   ├── validation.clj          # product input validation
│   └── csv.clj                 # CSV parsing for bulk upload
└── logs/                       # bulk-upload logs
```

## Building the jar

```bash
lein uberjar
java -jar target/shopping-cart-demo.jar
```

## Known limitations

This is a demo application. Before using anything like it in production:

- **Authentication:** users and plaintext passwords are hardcoded in `auth.clj`.
- **Sessions:** the default in-memory session store loses all sessions on restart and doesn't work across multiple instances.
- **HTTPS:** the app speaks plain HTTP. 
