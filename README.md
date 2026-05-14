# 💰 AI-Powered Personal Finance Tracker

A full-stack personal finance application with AI insights, built with **Spring Boot 3**, **React.js**, **PostgreSQL**, and **OpenAI GPT**.

---

## 📁 Project Structure

```
finance-tracker/
├── backend/                          # Spring Boot 3 + Java 21
│   ├── src/main/java/com/financetracker/
│   │   ├── config/                   # Security, Swagger, App configs
│   │   ├── controller/               # REST API controllers
│   │   ├── dto/                      # Request/Response DTOs
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/                   # JPA Entities
│   │   ├── exception/                # Global exception handling
│   │   ├── repository/               # Spring Data JPA Repositories
│   │   ├── security/                 # JWT Filter, UserDetails
│   │   └── service/impl/             # Business logic services
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                         # React.js + Tailwind CSS
│   ├── src/
│   │   ├── api/                      # Axios API clients
│   │   ├── components/common/        # Layout, Modal components
│   │   ├── context/                  # React Auth Context
│   │   ├── pages/                    # All page components
│   │   └── index.css                 # Tailwind + custom styles
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
│
├── docker/
│   └── init.sql                      # PostgreSQL schema + indexes
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🚀 Quick Start

### Option 1: Docker (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/yourname/finance-tracker.git
cd finance-tracker

# 2. Configure environment
cp .env.example .env
# Edit .env with your values (OpenAI key, Gmail credentials)

# 3. Start everything
docker-compose up --build

# App will be available at:
# Frontend:  http://localhost:3000
# Backend:   http://localhost:8080/api
# Swagger:   http://localhost:8080/api/swagger-ui.html
```

---

### Option 2: Manual Setup

#### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+
- PostgreSQL 14+

#### Backend Setup

```bash
cd backend

# Configure environment variables or edit application.properties
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=finance_tracker
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=YourSecretKeyMinimum512BitsLong1234567890ABCDEFGHIJ
export OPENAI_API_KEY=sk-your-openai-key
export MAIL_USERNAME=your@gmail.com
export MAIL_PASSWORD=your_app_password

# Run the application
mvn spring-boot:run

# Backend starts on http://localhost:8080
```

#### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Configure API URL
echo "REACT_APP_API_URL=http://localhost:8080/api" > .env

# Start development server
npm start

# Frontend starts on http://localhost:3000
```

#### Database Setup

```bash
# Create the PostgreSQL database
psql -U postgres -c "CREATE DATABASE finance_tracker;"

# Apply schema (optional - JPA handles DDL, but for indexes/triggers)
psql -U postgres -d finance_tracker -f docker/init.sql
```

---

## 🔑 Gmail App Password Setup

1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Enable **2-Step Verification**
3. Search for **"App passwords"**
4. Create app password for "Mail" > "Other (custom)"
5. Copy the 16-character password to `MAIL_PASSWORD`

---

## 📡 API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new user |
| POST | `/auth/verify-otp` | Verify email OTP |
| POST | `/auth/resend-otp` | Resend OTP |
| POST | `/auth/login` | Login |
| POST | `/auth/forgot-password` | Request reset email |
| POST | `/auth/reset-password` | Reset with token |

### Expenses
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/expenses` | Get all (filters: startDate, endDate, categoryId) |
| POST | `/expenses` | Create expense |
| PUT | `/expenses/{id}` | Update expense |
| DELETE | `/expenses/{id}` | Delete expense |

### Income
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/income` | Get all (filters: startDate, endDate) |
| POST | `/income` | Create income |
| PUT | `/income/{id}` | Update income |
| DELETE | `/income/{id}` | Delete income |

### Budgets
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/budgets` | Get all (filters: month, year) |
| POST | `/budgets` | Create budget |
| PUT | `/budgets/{id}` | Update budget |
| DELETE | `/budgets/{id}` | Delete budget |

### Categories
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/categories` | Get all |
| POST | `/categories` | Create custom |
| PUT | `/categories/{id}` | Update |
| DELETE | `/categories/{id}` | Delete |

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard` | Analytics (params: month, year) |

### AI Features
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/ai/analyze` | AI spending analysis |
| POST | `/ai/chat` | Chat with AI assistant |
| POST | `/ai/categorize` | AI auto-categorize expense |

**Swagger UI:** `http://localhost:8080/api/swagger-ui.html`

---

## 🗄️ Database Tables

| Table | Description |
|-------|-------------|
| `users` | User accounts, auth, OTP/reset tokens |
| `categories` | Default + custom expense/income categories |
| `expenses` | Expense records with category, payment method |
| `income` | Income records with source type |
| `budgets` | Monthly budgets with alert thresholds |

---

## 🤖 AI Features

### Financial Health Score Algorithm
```
Score = Savings Component (40pts)
      + Expense Diversity / Shannon Entropy (30pts)
      + Has Income (20pts)
      + Expense Ratio < 70% (10pts)

Grade: A (80-100), B (60-79), C (40-59), D (< 40)
```

### Dashboard Statistics Algorithm
```
Monthly Trend: Query income/expense per month for full year
Category Breakdown: GROUP BY category with percentage calculation
Budget Status: (spent / budget) × 100 with color thresholds
Savings Rate: (1 - expenses/income) × 100
```

---

## 🔧 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Java 21, Spring Security |
| Auth | JWT (jjwt 0.12), BCrypt |
| Database | PostgreSQL 16, Spring Data JPA, Hibernate |
| Email | Spring Boot Mail (Gmail SMTP) |
| AI | OpenAI GPT-3.5-turbo REST API |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Frontend | React 18, React Router 6 |
| Styling | Tailwind CSS 3 |
| Charts | Recharts |
| HTTP | Axios |
| Container | Docker, Docker Compose, Nginx |

---

## 📧 Email Features

- **Welcome Email** – Sent on registration
- **OTP Verification** – 6-digit code with 10-minute expiry
- **Password Reset** – Secure link with 1-hour expiry
- **Budget Alert** – Email when spending reaches threshold %
- **Monthly Summary** – Scheduled monthly finance report

---

## 🌐 Deployment

### Production with Docker
```bash
# Set production environment variables
export OPENAI_API_KEY=sk-...
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=xxxx xxxx xxxx xxxx
export JWT_SECRET=<64+ char random string>
export FRONTEND_URL=https://yourdomain.com

docker-compose -f docker-compose.yml up -d --build
```

### Environment Variables Reference
| Variable | Required | Description |
|----------|----------|-------------|
| `OPENAI_API_KEY` | Yes | OpenAI API key |
| `MAIL_USERNAME` | Yes | Gmail address |
| `MAIL_PASSWORD` | Yes | Gmail App Password |
| `JWT_SECRET` | Yes | 64+ char secret string |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `FRONTEND_URL` | No | Frontend URL for emails |

---

## 📱 Features Overview

- ✅ **Register / Login** with JWT
- ✅ **Email OTP** verification
- ✅ **Forgot / Reset Password** via email link
- ✅ **Add / Edit / Delete** Expenses & Income
- ✅ **Category Management** (defaults + custom)
- ✅ **Date & Category Filters**
- ✅ **Monthly Dashboard** with analytics
- ✅ **Pie Charts, Area Charts, Bar Charts**
- ✅ **Budget Management** with progress bars
- ✅ **Budget Alerts** via email
- ✅ **AI Spending Analysis** with health score
- ✅ **AI Finance Chatbot**
- ✅ **AI Auto-Categorization**
- ✅ **Responsive UI** (mobile + desktop)
- ✅ **Swagger API Docs**
- ✅ **Docker Deployment**

---

*Built with ❤️ using Spring Boot 3 + React.js + OpenAI*
#   F i n a n c e - T r a c k e r -  
 