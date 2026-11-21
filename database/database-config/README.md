# Database configuration (MySQL 8)

LifeLink uses **MySQL 8**. The connection is configured in `backend/src/main/resources/application.properties` and filled from environment variables / the `.env` file in the project root.

## 1. Install and start MySQL
Install MySQL 8 (Windows: MySQL Installer, "Server only"). Make sure the Windows service **MySQL80** is running. Remember the root password.

## 2. Configure the application
Copy `.env.example` to `.env` and set:
```
DB_URL=jdbc:mysql://localhost:3306/lifelink?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```
The database `lifelink` and all tables are created automatically on first start (`DDL_AUTO=update`), and demo data is loaded when the database is empty.

## 3. Optional: create tables manually
```
mysql -u root -p < database/schema.sql
```
Then set `DDL_AUTO=validate` in `.env` and start the app with `DEMO_DATA_ENABLED=true` to let the application create the demo accounts (they need real BCrypt hashes).

## 4. Optional: dedicated database user (recommended instead of root)
Edit the password in `create-app-user.sql`, run it as root, then use `DB_USERNAME=lifelink_app` and that password in `.env`.

## 5. Reset the database
```
mysql -u root -p -e "DROP DATABASE lifelink;"
```
Start the application again; it recreates everything including the demo data.

## Files in this folder
- `README.md` - this guide
- `create-app-user.sql` - creates a limited MySQL user for the application
- `application-mysql.properties.example` - the datasource settings explained
