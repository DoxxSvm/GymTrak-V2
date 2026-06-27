# Mobile Auth API Examples

Base URL: `/api/v1`

## 1) Splash Config

### Request

`GET /app/config`

### Response (200)

```json
{
  "app_name": "GymTrak",
  "app_version": "1.0.0",
  "force_update": false,
  "maintenance_mode": false,
  "support_contact": "+919999999999",
  "default_country_code": "+91",
  "feature_flags": {
    "owner_auth_v2": true
  },
  "login_methods_enabled": {
    "phone": true,
    "email": false,
    "google": false,
    "apple": false
  },
  "splash_assets": {
    "logo_url": "",
    "background_url": ""
  }
}
```

## 2) Send OTP

### Request

`POST /auth/send-otp`

```json
{
  "phone": "9876543210",
  "country_code": "+91"
}
```

### Response (200)

```json
{
  "success": true,
  "message": "OTP sent successfully",
  "expires_in": 300
}
```

## 3) Verify OTP

### Request

`POST /auth/verify-otp`

```json
{
  "phone": "9876543210",
  "country_code": "+91",
  "otp": "123456"
}
```

### Response (200)

```json
{
  "success": true,
  "access_token": "jwt-token",
  "refresh_token": "opaque-refresh-token",
  "user": {
    "id": "cuid",
    "name": "Owner Name",
    "phone": "+919876543210",
    "role": "gym_owner"
  }
}
```

## 4) Trainer/Owner Login

### Request

`POST /auth/login`

```json
{
  "username": "trainer1",
  "password": "secret123"
}
```

### Response (200)

```json
{
  "success": true,
  "access_token": "jwt-token",
  "refresh_token": "opaque-refresh-token",
  "user": {
    "id": "cuid",
    "name": "Trainer Name",
    "role": "trainer"
  }
}
```

## 5) Resend OTP

### Request

`POST /auth/resend-otp`

```json
{
  "phone": "9876543210",
  "country_code": "+91"
}
```

### Response (200)

```json
{
  "success": true,
  "message": "OTP sent successfully",
  "expires_in": 300
}
```

## 6) Logout

### Request

`POST /auth/logout`

```json
{
  "refresh_token": "opaque-refresh-token"
}
```

### Response (200)

```json
{
  "success": true,
  "message": "Logged out successfully"
}
```
