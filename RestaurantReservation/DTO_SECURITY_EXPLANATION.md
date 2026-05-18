# DTO Security Pattern Explanation

## The Problem: Mass Assignment Vulnerability

### **❌ Dangerous Approach (Using Entity Directly)**

```java
// BAD - UserController.java
@PutMapping("/profile")
public AppUser updateProfile(@RequestBody AppUser user) {
    return userRepository.save(user);  // DANGER!
}
```

**Attack Scenario:**
```json
// Malicious user sends:
{
  "id": 5,
  "fullName": "Hacker",
  "email": "hacker@evil.com",
  "role": "OWNER",           // ← Promoting themselves!
  "restaurantId": 1,         // ← Changing to another restaurant!
  "passwordHash": "xyz123"   // ← Changing password directly!
}
```

**Result:** User just made themselves OWNER and stole access to another restaurant! 💀

---

## ✅ The Solution: Separate DTOs for Different Operations

We created **4 different DTOs** for user operations:

### **1. AppUserDto** (Read-Only)
```java
// For READING user data - safe to expose
public class AppUserDto {
    private Long id;
    private String fullName;
    private String email;
    private String role;         // Read-only
    private Long restaurantId;   // Read-only
    // NO passwordHash - never expose!
}
```

**Used for:**
- GET /api/users (list all users)
- GET /api/users/{id} (get specific user)
- GET /api/users/me (get current user)

---

### **2. UpdateUserProfileRequest** (User Updates Themselves)
```java
// Only contains fields a USER can safely update
public class UpdateUserProfileRequest {
    private String fullName;  // ✅ Safe
    private String email;     // ✅ Safe
    // NO role        ← User CANNOT change their role
    // NO restaurantId ← User CANNOT switch restaurants
    // NO password    ← Separate endpoint for security
}
```

**Security Benefits:**
- Even if hacker sends `{"role": "OWNER"}`, it's **ignored**
- The DTO class doesn't have a `role` field, so Jackson (JSON parser) skips it
- Service only updates the allowed fields

**Used for:**
- PUT /api/users/me (update own profile)

---

### **3. ChangePasswordRequest** (Password Change)
```java
// Separated for extra security
public class ChangePasswordRequest {
    private String currentPassword;  // Must prove identity
    private String newPassword;      // New password
}
```

**Why separate?**
- Requires current password (proof of identity)
- Different security requirements than profile update
- Can add 2FA later if needed

**Used for:**
- PUT /api/users/me/password (change own password)

---

### **4. CreateUserRequest** (Owner Creates Staff)
```java
// Only OWNER can use this - includes role assignment
public class CreateUserRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;           // OWNER can set this
    private Long restaurantId;   // OWNER can set this
}
```

**Security:**
- Protected by `@PreAuthorize("hasRole('OWNER')")`
- Regular users can't even call this endpoint
- Only OWNER can create users and assign roles

**Used for:**
- POST /api/users (OWNER creates staff member)

---

## How It Works in Practice

### **Scenario 1: User Updates Their Profile**

**Request:**
```http
PUT /api/users/me
Authorization: Bearer <jwt-token>
{
  "fullName": "John Updated",
  "email": "john.new@email.com",
  "role": "OWNER"  ← Hacker tries to promote themselves
}
```

**What Happens:**
1. Spring Security validates JWT token ✅
2. Request is deserialized to `UpdateUserProfileRequest`
3. **The DTO has no `role` field, so it's ignored!** ✅
4. Service gets current user from SecurityContext
5. Only updates `fullName` and `email`
6. Role remains unchanged ✅

**Result:** User updates their name/email, but role stays as STAFF 🎉

---

### **Scenario 2: Hacker Tries to Change Password Without Current Password**

**Request:**
```http
PUT /api/users/me
{
  "fullName": "Hacker",
  "passwordHash": "new-hash-here"  ← Trying to bypass
}
```

**What Happens:**
1. Request maps to `UpdateUserProfileRequest`
2. **No `passwordHash` field exists in DTO** ✅
3. Password remains unchanged ✅

**To actually change password:**
```http
PUT /api/users/me/password
{
  "currentPassword": "old-password",  ← Must provide!
  "newPassword": "new-password"
}
```

Service verifies current password before allowing change ✅

---

### **Scenario 3: OWNER Creates New Staff Member**

**Request:**
```http
POST /api/users
Authorization: Bearer <owner-jwt-token>
{
  "fullName": "Jane Staff",
  "email": "jane@restaurant.com",
  "password": "temp123",
  "role": "MANAGER",
  "restaurantId": 1
}
```

**What Happens:**
1. `@PreAuthorize("hasRole('OWNER')")` checks token ✅
2. If user is not OWNER → 403 Forbidden ✅
3. If user is OWNER → Allowed to set role ✅

---

## Code Comparison

### ❌ Bad (Vulnerable to Mass Assignment)

```java
@PutMapping("/profile")
public AppUser updateProfile(@RequestBody AppUser user) {
    AppUser existing = userRepository.findById(user.getId())
        .orElseThrow();

    // Blindly copy all fields - DANGEROUS!
    existing.setFullName(user.getFullName());
    existing.setEmail(user.getEmail());
    existing.setRole(user.getRole());              // ← Can be exploited!
    existing.setRestaurant(user.getRestaurant());  // ← Can be exploited!

    return userRepository.save(existing);
}
```

### ✅ Good (Secure with DTO)

```java
@PutMapping("/me")
public AppUserDto updateOwnProfile(@RequestBody UpdateUserProfileRequest request) {
    // Get current authenticated user
    String email = SecurityContextHolder.getContext()
        .getAuthentication().getName();

    AppUser user = userRepository.findByEmail(email)
        .orElseThrow();

    // Only update allowed fields from DTO
    user.setFullName(request.getFullName());
    user.setEmail(request.getEmail());

    // Role and restaurant NEVER change - not in DTO!

    userRepository.save(user);
    return toDto(user);
}
```

---

## Security Layers in Our Implementation

1. **DTOs** - Only accept specific fields
2. **Spring Security** - Authenticates user with JWT
3. **@PreAuthorize** - Checks user role before allowing access
4. **Service Layer** - Gets current user from SecurityContext (can't be faked)
5. **Validation** - `@Valid` ensures data integrity

---

## Summary: Why This Matters

| Approach | Security Level | Risk |
|----------|---------------|------|
| Using Entity directly | ❌ Low | User can change role, password, restaurant |
| Using single DTO | ⚠️ Medium | Better, but all operations share same fields |
| Using operation-specific DTOs | ✅ High | Each operation has exactly the fields it needs |

**Our approach:** Different DTOs for:
- Reading data (AppUserDto)
- User updating profile (UpdateUserProfileRequest)
- Changing password (ChangePasswordRequest)
- Owner creating staff (CreateUserRequest)

This follows the **Principle of Least Privilege**: Each operation can only access/modify exactly what it needs, nothing more.

---

## Testing the Security

Try this after registering as STAFF:

```bash
# Get your profile (should work)
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <your-token>"

# Try to promote yourself to OWNER (should be ignored)
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Hacker","email":"hacker@evil.com","role":"OWNER"}'

# Check profile again (role should still be STAFF)
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <your-token>"
```

The role will remain STAFF because the DTO doesn't allow changing it! 🎉
