# API Testing Guide

## Base URL
```
http://localhost:8080/api
```

## 1. Create a Restaurant (First!)

```bash
curl -X POST http://localhost:8080/api/restaurants \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Georgian Bistro\",\"phone\":\"+995555123456\",\"address\":\"Rustaveli Ave 1, Tbilisi\",\"cuisineType\":\"Georgian\",\"openingHours\":\"Mon-Sun: 10:00-23:00\",\"timezone\":\"Asia/Tbilisi\"}"
```

**Response:** Note the `id` (let's say it's `1`)

---

## 2. Register a User (Owner)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"John Doe\",\"email\":\"owner@restaurant.com\",\"password\":\"password123\",\"role\":\"OWNER\",\"restaurantId\":1}"
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "owner@restaurant.com",
  "role": "OWNER"
}
```

**Save the token!** You'll need it for authenticated requests.

---

## 3. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"owner@restaurant.com\",\"password\":\"password123\"}"
```

---

## 4. Create Tables

```bash
# Table 1
curl -X POST http://localhost:8080/api/tables \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d "{\"label\":\"Table 1\",\"capacity\":4,\"status\":\"AVAILABLE\",\"positionX\":10.0,\"positionY\":10.0,\"restaurantId\":1}"

# Table 2
curl -X POST http://localhost:8080/api/tables \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d "{\"label\":\"Table 2\",\"capacity\":2,\"status\":\"AVAILABLE\",\"positionX\":20.0,\"positionY\":10.0,\"restaurantId\":1}"
```

**Response:** Note the table IDs (e.g., `1`, `2`)

---

## 5. Get All Tables for Restaurant

```bash
curl -X GET "http://localhost:8080/api/tables?restaurantId=1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 6. Create a Reservation

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d "{\"restaurantId\":1,\"tableId\":1,\"guestName\":\"Alice Smith\",\"guestPhone\":\"+995555111222\",\"guestEmail\":\"alice@example.com\",\"startTime\":\"2026-05-13T19:00:00\",\"partySize\":4,\"notes\":\"Window seat preferred\"}"
```

**Note:** The system will:
- Create the guest automatically (or find existing by phone)
- Calculate end time (start + 90 minutes)
- Check for table conflicts
- Set status to CONFIRMED

---

## 7. Check Available Tables

```bash
curl -X GET "http://localhost:8080/api/reservations/available-tables?restaurantId=1&startTime=2026-05-13T19:00:00&endTime=2026-05-13T20:30:00" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 8. Get Daily Reservations

```bash
curl -X GET "http://localhost:8080/api/reservations/daily?restaurantId=1&date=2026-05-13" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 9. Get Dashboard

```bash
curl -X GET "http://localhost:8080/api/dashboard?restaurantId=1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Response:**
```json
{
  "todayReservations": [...],
  "upcomingReservations": [...],
  "occupiedTables": [...],
  "freeTables": [...],
  "noShowPercentage": 0.0,
  "totalReservationsToday": 1
}
```

---

## 10. Update Reservation Status

```bash
# Mark as completed
curl -X PATCH "http://localhost:8080/api/reservations/1/status?status=COMPLETED" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Mark as no-show
curl -X PATCH "http://localhost:8080/api/reservations/2/status?status=NO_SHOW" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Cancel
curl -X PATCH http://localhost:8080/api/reservations/3/cancel \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 11. Get Guest by Phone

```bash
curl -X GET "http://localhost:8080/api/guests/search?restaurantId=1&phone=%2B995555111222" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Shows guest history:** total visits, no-show count

---

## 12. Get Analytics

```bash
curl -X GET "http://localhost:8080/api/analytics?restaurantId=1&start=2026-05-01&end=2026-05-31" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Response:**
```json
{
  "peakHours": {
    "19": 5,
    "20": 3,
    "18": 2
  },
  "busiestDays": {
    "FRIDAY": 8,
    "SATURDAY": 10,
    "SUNDAY": 5
  },
  "averagePartySize": 3.5,
  "totalReservations": 23,
  "completedReservations": 18,
  "cancelledReservations": 2,
  "noShows": 3,
  "noShowPercentage": 13.04
}
```

---

## Testing with Postman or Insomnia

1. Import these requests into Postman/Insomnia
2. Create an environment variable `BASE_URL = http://localhost:8080/api`
3. Create an environment variable `TOKEN` for the JWT
4. Use `{{BASE_URL}}` and `{{TOKEN}}` in requests

---

## All API Endpoints

### Authentication
- `POST /api/auth/register` - Register user
- `POST /api/auth/login` - Login

### Restaurants
- `GET /api/restaurants` - Get all restaurants
- `GET /api/restaurants/{id}` - Get restaurant by ID
- `POST /api/restaurants` - Create restaurant
- `PUT /api/restaurants/{id}` - Update restaurant
- `DELETE /api/restaurants/{id}` - Delete restaurant

### Tables
- `GET /api/tables?restaurantId=X` - Get tables by restaurant
- `GET /api/tables/{id}` - Get table by ID
- `POST /api/tables` - Create table
- `PUT /api/tables/{id}` - Update table
- `DELETE /api/tables/{id}` - Delete table

### Guests
- `GET /api/guests?restaurantId=X` - Get guests by restaurant
- `GET /api/guests/{id}` - Get guest by ID
- `GET /api/guests/search?restaurantId=X&phone=Y` - Find by phone
- `POST /api/guests` - Create guest
- `PUT /api/guests/{id}` - Update guest
- `DELETE /api/guests/{id}` - Delete guest

### Reservations
- `POST /api/reservations` - Create reservation
- `GET /api/reservations/{id}` - Get reservation by ID
- `GET /api/reservations/daily?restaurantId=X&date=Y` - Daily calendar
- `GET /api/reservations/weekly?restaurantId=X&weekStart=Y` - Weekly calendar
- `GET /api/reservations/available-tables?restaurantId=X&startTime=Y&endTime=Z` - Check availability
- `PATCH /api/reservations/{id}/status?status=X` - Update status
- `PATCH /api/reservations/{id}/cancel` - Cancel reservation

### Dashboard
- `GET /api/dashboard?restaurantId=X` - Get dashboard data

### Analytics
- `GET /api/analytics?restaurantId=X&start=Y&end=Z` - Get analytics

---

## Database Access

To view tables directly:

```bash
# Connect to PostgreSQL
psql -U postgres -d restaurant_reservations

# List tables
\dt

# View data
SELECT * FROM restaurants;
SELECT * FROM users;
SELECT * FROM restaurant_tables;
SELECT * FROM guests;
SELECT * FROM reservations;

# Check Flyway migrations
SELECT * FROM flyway_schema_history;
```
