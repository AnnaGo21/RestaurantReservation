# Restaurant Reservation & Analytics Platform

A lightweight SaaS platform for restaurants in Georgia focused on reservation operations, no-show reduction, table organization, and restaurant analytics.

## Overview

This platform is an **internal operational tool** for restaurants to manage reservations received from phone calls, social media (Instagram/Facebook), WhatsApp, existing reservation websites, and walk-ins. It simplifies reservation operations and provides valuable analytics that restaurants currently lack.

### What This Platform Is NOT
- ❌ POS system
- ❌ Inventory system
- ❌ Accounting platform
- ❌ Food delivery platform
- ❌ Public reservation marketplace (initially)

### What This Platform IS
- ✅ The easiest restaurant reservation and operational insights platform
- ✅ Real-time table availability management
- ✅ No-show protection and tracking
- ✅ Actionable analytics and insights
- ✅ Simple, fast, and pressure-tested for busy restaurant operations

---

## Features

### 🔐 Authentication & Authorization
- Secure JWT-based authentication
- Role-based access control (OWNER, MANAGER, STAFF)
- Multi-user support per restaurant

### 🏢 Restaurant Management
- Restaurant profile management
- Opening hours configuration
- Logo and branding
- Timezone support

### 🪑 Table Management
- Create and configure tables
- Set seating capacities
- Enable/disable tables
- Simple floor layout positioning

### 📅 Reservation Management (Core Module)
- Manual reservation entry by staff
- Automatic table availability checking
- Overlap and double-booking prevention
- Default 90-minute reservation duration
- Guest information tracking (name, phone, party size, notes)

### ⏱️ Real-Time Availability Engine
- Intelligent table availability detection
- Automatic conflict resolution
- Capacity validation
- Best table suggestions

### 🚫 No-Show Protection System
- Automatic no-show detection with configurable grace period (default: 15 minutes)
- Guest no-show history tracking
- Repeat offender identification

### 📱 SMS Reminder System
- Reservation confirmation messages
- 24-hour reminder notifications
- Twilio integration (mock mode available for development)

### 📊 Dashboard
- Today's and upcoming reservations
- Occupied vs. free tables
- No-show percentage
- Busiest hours
- Total reservations overview

### 📈 Analytics
- **Peak Hours**: Most popular reservation times
- **Busiest Days**: Traffic by weekday
- **Average Group Size**: Party size trends
- **No-Show Analysis**: Ratio and patterns
- **Reservation Trends**: Daily/weekly/monthly insights

### 👥 Guest Management
- Guest history and visit tracking
- Phone number and name lookup
- No-show history per guest
- Notes and preferences

### 📆 Calendar Views
- Daily and weekly calendar views
- Visual table occupancy
- Color-coded reservation statuses
- Fast and responsive interface

---

## Tech Stack

### Backend
- **Java 21**
- **Spring Boot 3.3.5**
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate
- **PostgreSQL 16** database
- **Flyway** for database migrations
- **Lombok** for reducing boilerplate

### External Services
- **Twilio SDK** for SMS notifications

### Build Tool
- **Maven**

---

## Getting Started

### Prerequisites
- **Java 21** or higher
- **Maven 3.6+**
- **Docker & Docker Compose** (for PostgreSQL)
- **PostgreSQL 16** (if not using Docker)

### Environment Variables

Create a `.env` file or set the following environment variables:

```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT Secret (use a strong secret in production)
JWT_SECRET=your-secure-jwt-secret-key-here

# SMS Configuration
SMS_ENABLED=false
SMS_MOCK_MODE=true

# Twilio (if SMS is enabled)
TWILIO_ACCOUNT_SID=your-twilio-account-sid
TWILIO_AUTH_TOKEN=your-twilio-auth-token
TWILIO_FROM_NUMBER=your-twilio-phone-number
```

### Running with Docker Compose

1. **Start PostgreSQL:**
```bash
docker-compose up -d
```

2. **Verify PostgreSQL is running:**
```bash
docker ps
```

### Running the Application

1. **Clone the repository:**
```bash
git clone <repository-url>
cd RestaurantReservation
```

2. **Build the project:**
```bash
mvn clean install
```

3. **Run the application:**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Database Setup

The database schema is automatically created and migrated using **Flyway** on application startup.

#### Migration Files:
- `V1__create_core_schema.sql` - Creates core tables (restaurants, users, tables, guests, reservations)
- `V2__seed_dev_data.sql` - Seeds development data
- `V3__make_restaurant_id_nullable_in_users.sql` - Schema adjustment
- `V4__add_grace_period_to_restaurants.sql` - Adds grace period configuration

---

## API Documentation

### Authentication Endpoints
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and receive JWT token

### Restaurant Endpoints
- `GET /api/restaurants` - Get all restaurants
- `GET /api/restaurants/{id}` - Get restaurant by ID
- `POST /api/restaurants` - Create restaurant (OWNER)
- `PUT /api/restaurants/{id}` - Update restaurant (OWNER)
- `DELETE /api/restaurants/{id}` - Delete restaurant (OWNER)

### Table Endpoints
- `GET /api/tables` - Get all tables for restaurant
- `GET /api/tables/{id}` - Get table by ID
- `POST /api/tables` - Create table
- `PUT /api/tables/{id}` - Update table
- `DELETE /api/tables/{id}` - Delete table

### Reservation Endpoints
- `GET /api/reservations` - Get all reservations
- `GET /api/reservations/{id}` - Get reservation by ID
- `POST /api/reservations` - Create reservation
- `PUT /api/reservations/{id}` - Update reservation
- `PUT /api/reservations/{id}/status` - Update reservation status
- `DELETE /api/reservations/{id}` - Cancel reservation

### Guest Endpoints
- `GET /api/guests` - Get all guests
- `GET /api/guests/{id}` - Get guest by ID
- `GET /api/guests/phone/{phone}` - Find guest by phone number
- `POST /api/guests` - Create guest
- `PUT /api/guests/{id}` - Update guest

### Dashboard Endpoints
- `GET /api/dashboard` - Get dashboard overview
- `GET /api/dashboard/today` - Get today's reservations

### Analytics Endpoints
- `GET /api/analytics` - Get analytics data
- `GET /api/analytics/peak-hours` - Get peak reservation hours
- `GET /api/analytics/no-shows` - Get no-show statistics

### Calendar Endpoints
- `GET /api/calendar/daily` - Get daily calendar view
- `GET /api/calendar/weekly` - Get weekly calendar view

### User Management Endpoints
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user (OWNER)
- `PUT /api/users/{id}` - Update user
- `PUT /api/users/{id}/password` - Change password
- `DELETE /api/users/{id}` - Delete user (OWNER)

---

## Project Structure

```
src/main/java/org/example/reservations/
├── analytics/          # Analytics service and controllers
├── auth/              # Authentication & JWT security
├── calendar/          # Calendar views and scheduling
├── config/            # Spring Security configuration
├── dashboard/         # Dashboard service and DTOs
├── exception/         # Global exception handling
├── guest/             # Guest management
├── notification/      # SMS service and reminders
├── reservation/       # Core reservation logic
├── restaurant/        # Restaurant management
├── table/             # Table management
└── user/              # User management and roles

src/main/resources/
├── db/migration/      # Flyway database migrations
└── application.yml    # Application configuration
```

---

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 86400000  # 24 hours

  reservation:
    default-slot-minutes: 90      # Default reservation duration
    grace-period-minutes: 15      # No-show grace period

sms:
  enabled: false
  mock-mode: true

twilio:
  account-sid: ${TWILIO_ACCOUNT_SID}
  auth-token: ${TWILIO_AUTH_TOKEN}
  from-number: ${TWILIO_FROM_NUMBER}
```

---

## Business Rules

### Reservation Duration
- **Default**: 90 minutes
- Table is blocked from start time until start time + duration

### No-Show Detection
- **Grace Period**: 15 minutes (configurable per restaurant)
- If guest doesn't arrive within grace period after reservation time, status automatically changes to `NO_SHOW`

### Reservation Statuses
- `PENDING` - Initial state
- `CONFIRMED` - Reservation confirmed
- `COMPLETED` - Guest arrived and finished
- `CANCELLED` - Reservation cancelled
- `NO_SHOW` - Guest didn't arrive

### Table Availability
- Tables are unavailable during active reservations
- System prevents overlapping reservations
- Capacity validation ensures party size fits table

### User Roles
- **OWNER**: Full access to all features
- **MANAGER**: Access to operations, analytics, tables
- **STAFF**: Reservation operations only

---

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package -DskipTests
```

### Database Access
If using Docker Compose:
```bash
# Access PostgreSQL CLI
docker exec -it restaurant_reservations_postgres psql -U postgres -d restaurant_reservations
```

### API Testing
Use the included Postman collection:
- `Restaurant-API.postman_collection.json`
- `http-client.env.json`

Or refer to `API_TESTING_GUIDE.md` for detailed testing instructions.

---

## Additional Documentation

- **API Testing Guide**: `API_TESTING_GUIDE.md`
- **DBeaver Setup**: `DBEAVER_SETUP_GUIDE.md`
- **DTO Security**: `DTO_SECURITY_EXPLANATION.md`
- **Product Specification**: `MVP.txt`

---

## Deployment

### Docker Deployment
```bash
# Build the application
mvn clean package -DskipTests

# Create Dockerfile (not included, add as needed)
# Deploy with docker-compose
docker-compose up -d
```

### Production Checklist
- [ ] Use strong JWT secret
- [ ] Configure proper database credentials
- [ ] Enable SSL/TLS
- [ ] Set up proper logging
- [ ] Configure SMS provider (Twilio)
- [ ] Set up monitoring and alerts
- [ ] Configure backup strategy
- [ ] Review security settings

---

## Future Roadmap

### Phase 2 - Restaurant Discovery Platform
- Public restaurant pages
- Search and filter system
- Maps integration
- Reviews aggregation
- Availability display

### Phase 3 - Public Booking Platform
- Customer accounts
- Public reservation booking
- Loyalty programs
- Promotions
- Reservation commissions

### Future Features
- Google Maps integration
- Fina/POS integrations
- AI demand forecasting
- Mobile apps (iOS/Android)
- Multi-language support

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

[Add your license here]

---

## Contact

For questions or support, please contact [your contact information]

---

## Acknowledgments

Built for restaurants in Georgia to solve real operational pain and simplify reservation management.

**Goal**: Faster than notebooks, simpler than existing POS systems, easier than Excel.
