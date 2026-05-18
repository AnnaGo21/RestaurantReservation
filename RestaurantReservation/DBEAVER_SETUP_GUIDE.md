# DBeaver Setup Guide

## Install DBeaver

1. Download from: https://dbeaver.io/download/
2. Choose **DBeaver Community Edition** (free)
3. Install and open

---

## Connect to PostgreSQL Database

### **Step 1: Create New Connection**
1. Click **Database** → **New Database Connection**
2. Select **PostgreSQL**
3. Click **Next**

### **Step 2: Enter Connection Details**

```
Host: localhost
Port: 5432
Database: restaurant_reservations
Username: postgres
Password: postgres
```

### **Step 3: Test Connection**
1. Click **Test Connection**
2. If it says "Download driver files", click **Download**
3. Should see: **Connected** ✅
4. Click **Finish**

---

## View Your Data

### **Tables Location:**
```
restaurant_reservations
└── Databases
    └── restaurant_reservations
        └── Schemas
            └── public
                └── Tables
                    ├── users
                    ├── restaurants
                    ├── restaurant_tables
                    ├── guests
                    ├── reservations
                    └── flyway_schema_history
```

### **To View Table Data:**
1. Expand the tree to find a table (e.g., `restaurants`)
2. Right-click → **View Data**
3. Or double-click the table

### **To Run SQL Queries:**
1. Click **SQL Editor** → **New SQL Script**
2. Write your query:
   ```sql
   SELECT * FROM restaurants;
   SELECT * FROM users;
   SELECT * FROM reservations;
   ```
3. Press **Ctrl + Enter** to run

---

## Useful Queries for Testing

### **1. Check all restaurants**
```sql
SELECT * FROM restaurants;
```

### **2. Check all users and their roles**
```sql
SELECT id, full_name, email, role, restaurant_id
FROM users;
```

### **3. Check all reservations with guest info**
```sql
SELECT
    r.id,
    g.full_name as guest_name,
    g.phone,
    r.start_time,
    r.end_time,
    r.party_size,
    r.status,
    rt.label as table_name
FROM reservations r
JOIN guests g ON r.guest_id = g.id
JOIN restaurant_tables rt ON r.restaurant_table_id = rt.id
ORDER BY r.start_time DESC;
```

### **4. Check table availability**
```sql
SELECT
    rt.label,
    rt.capacity,
    rt.status,
    COUNT(r.id) as active_reservations
FROM restaurant_tables rt
LEFT JOIN reservations r ON rt.id = r.restaurant_table_id
    AND r.status IN ('CONFIRMED', 'PENDING')
    AND r.start_time <= NOW()
    AND r.end_time >= NOW()
WHERE rt.restaurant_id = 1
GROUP BY rt.id, rt.label, rt.capacity, rt.status;
```

### **5. Analytics - Peak hours**
```sql
SELECT
    EXTRACT(HOUR FROM start_time) as hour,
    COUNT(*) as reservation_count
FROM reservations
WHERE restaurant_id = 1
GROUP BY hour
ORDER BY reservation_count DESC;
```

### **6. No-show statistics**
```sql
SELECT
    g.full_name,
    g.phone,
    g.total_visits,
    g.no_show_count,
    ROUND(g.no_show_count::decimal / NULLIF(g.total_visits, 0) * 100, 2) as no_show_percentage
FROM guests g
WHERE g.restaurant_id = 1
ORDER BY g.no_show_count DESC;
```

---

## DBeaver Tips

### **Auto-complete**
- Press **Ctrl + Space** while typing SQL

### **Format SQL**
- Select SQL → Press **Ctrl + Shift + F**

### **Export Data**
- Right-click table → **Export Data**
- Choose format (CSV, JSON, SQL, etc.)

### **View ER Diagram**
1. Right-click **public** schema
2. Select **View Diagram**
3. See all tables and relationships visually

### **Edit Data Directly**
1. View table data
2. Double-click a cell to edit
3. Press **Ctrl + S** to save

---

## Troubleshooting

### **Can't connect?**
1. Make sure Docker PostgreSQL is running:
   ```powershell
   docker ps
   ```
2. Should see `restaurant_reservations_postgres` container

### **Start Docker if not running:**
```powershell
docker-compose up -d
```

### **Wrong password?**
- Default is `postgres` / `postgres`
- Check `docker-compose.yml` if changed

---

## What to Check After Testing API

### **After creating restaurant:**
```sql
SELECT * FROM restaurants;
-- Should see your restaurant with ID 1
```

### **After registering user:**
```sql
SELECT * FROM users;
-- Should see user with hashed password
```

### **After creating reservation:**
```sql
SELECT * FROM reservations;
SELECT * FROM guests;
-- Both tables should have new records
```

### **Check Flyway migrations:**
```sql
SELECT * FROM flyway_schema_history;
-- Should see 3 migrations (V1, V2, V3)
```
