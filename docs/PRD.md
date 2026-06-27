# Product Requirement Document (PRD) - GymTrak

## 1. Introduction
**Application Name:** GymTrak  
**Platform:** Mobile (Android & iOS)  
**Primary Theme:** Light Theme  
**Version:** 1.0  

### 1.1 Purpose
GymTrak is a comprehensive gym management mobile application designed to streamline operations for gym owners, simplify management for trainers, and enhance the experience for gym members. It acts as a one-stop solution for attendance, payments, member tracking, and business analytics.

### 1.2 Stakeholders
*   **Gym Owners:** Admin user with full control over the gym's operations, finances, statistics, and staff.
*   **Trainers/Staff:** Users with restricted access tailored to managing assigned clients, attendance, and plans.
*   **Gym Members:** End-users who track their subscriptions, attendance, and workout plans.

## 2. Common App Requirements

### 2.1 UI/UX Standards
*   **Theme:** exclusively Light Theme for a clean, professional look.
*   **Keyboard Handling:**
    *   Tapping anywhere outside a text input field must dismiss the keyboard.
    *   Input fields should scroll into view when the keyboard opens.
*   **Back Navigation:**
    *   Hardware/System back button must consistently navigate to the previous logical screen.
    *   Double back press on the Home Dashboard to exit the app.
*   **Loaders & Feedback:**
    *   Display a circular progress indicator (overlay or inline) for *all* network API calls.
    *   Buttons should be disabled while an action is in progress.

### 2.2 Error & State Handling
*   **No Internet:** Show a non-blocking banner or a full-screen "No Connection" state with a "Retry" button.
*   **API Failure:** Display user-friendly error messages (e.g., toast or snackbar) instead of raw error codes.
*   **Empty States:** All lists (Members, Plans, Transactions) must have visual empty states (illustration + text) when no data is available (e.g., "No Members Found").

---

## 3. Login & Onboarding Flow

### 3.1 S1: Get Started Screen
*   **Overview:** The landing screen for first-time users.
*   **UI Elements:**
    *   **Carousel:** Interactive slider showing app features and company vision.
    *   **Indicators:** Dots indicating current slide.
    *   **CTA Button:** "Get Started" (Primary).
*   **Logic:** Shown only on first launch.

### 3.2 S2: Central Login Screen
*   **Overview:** Unified login entry point.
*   **UI Elements:**
    *   **Phone Input:** Country code + Phone number field.
    *   **Role Switch/Link:** "Login as Trainer/Staff" option (if strictly separated).
    *   **Button:** "Send OTP".
    *   **OTP Verification:** 4/6 digit input fields with auto-submit.
*   **Error States:** Invalid phone format, OTP mismatch.

### 3.3 S3: Role Selection Screen
*   **Overview:** Allows the user to select their intent if the phone number is new or linked to multiple roles (if applicable).
*   **UI Elements:**
    *   **Cards:** "Gym Owner" vs. "Gym Member".
    *   **Description:** Brief text describing access for each role.

### 3.4 S4: Onboarding Screen
*   **Overview:** Collects essential initial data.
*   **Gym Owner Flow:**
    *   **Fields:** Gym Name, Owner Name, Location/City.
*   **User Flow:**
    *   **Fields:** Full Name, Gender, Goal (Weight Loss, Muscle Gain, etc.).
*   **Validation:** All fields mandatory.

---

## 4. Owner Module - Detailed Requirements

### 4.1 Home / Dashboard
*   **Overview:** The command center for the Gym Owner.
*   **UI Elements:**
    *   **Header:** Gym Selector (Dropdown for owners with multiple branches), Notifications Icon.
    *   **Gym Health Monitor:** Summary cards showing Active Members, New Enquiries, Expiring Soon.
    *   **Payments Overview:** Total revenue this month vs. last month (Green/Red indicators).
    *   **Quick Actions:** Attendance marking shortcut.
    *   **Live Attendance:** Count of members currently checked in.
    *   **Expiry Alerts:** List of subscriptions expiring in < 3 days.
    *   **Enquiry Tracker:** Pending follow-ups counter.
*   **Empty State:** "Welcome! Add your first member to see stats."

### 4.2 Members Module
*   **Overview:** Manage gym clientele.
*   **Client List Screen:**
    *   **Features:** Search bar (Name/Phone), Filters (Active, Expired, Inactive).
    *   **List Item:** Name, Photo, Plan Name, Expiry Status (Color-coded).
    *   **FAB:** "Add Member".
*   **Add Member (Onboarding):**
    *   **Form:** Personal Info, Photo, Select Plan, Assigned Trainer, Start Date.
*   **Member Profile Screen:**
    *   **Tabs:**
        *   **Info:** Personal details, goals, emergency contact.
        *   **Subscriptions:** Active/History of plans.
        *   **Attendance:** Calendar view of presence.
        *   **Payments:** Transaction history specific to this user.

### 4.3 Trainer Management
*   **Overview:** Manage staff and trainers.
*   **UI Elements:**
    *   **List:** Trainer profiles with active client count.
    *   **Onboarding:** Add new trainer with specialized skills (Yoga, HIIT, Weightlifting).
    *   **Trainer Detail:**
        *   Assigned Members list.
        *   **Attendance:** Track trainer's own attendance.
        *   **Salary/Payments:** Record salary payouts.

### 4.4 Plans Module
*   **Overview:** Configure sellable items.
*   **Supported Types:**
    *   **Gym Membership:** Standard access.
    *   **Personal Training (PT):** Trainer-specific.
    *   **Batch Plans:** Group classes (Zumba, Yoga).
    *   **Free Trial:** Limited duration access.
*   **UI Elements:**
    *   Create Plan Form: Name, Duration (Months/Days), Price, Description.
    *   List View: Card layout with Edit/Delete options.

### 4.5 Lead / Enquiry Management
*   **Overview:** CRM for potential clients.
*   **UI Elements:**
    *   **List:** Name, Contact, Interest (e.g., "Looking for Cardio"), Status (New, Follow-up, Converted, Dead).
    *   **Actions:** Call, WhatsApp, Convert to Member buttons.

### 4.6 Expense Tracker
*   **Overview:** Track gym operational costs.
*   **UI Elements:**
    *   **Add Expense:** Category (Rent, Electricity, Equipment), Amount, Date, Note.
    *   **History:** Monthly breakdown of expenses.

### 4.7 Subscription Plans (App Monetization)
*   **Overview:** Plans enabling the owner to use the GymTrak app.
*   **Tiers:**
    *   **Basic:** Limited members.
    *   **Plus:** Increased limits.
    *   **Premium:** Unlimited features.

### 4.8 Profile & Settings
*   **UI Elements:**
    *   **Gym Details:** Edit address, logo, contact info.
    *   **QR Scanner:** For member attendance check-in.
    *   **Leave Management:** Approve/Reject staff leave requests.
    *   **Settings:** App preferences.
    *   **Logout:** Secure session termination.

### 4.9 Payments Module
*   **Overview:** Financial health and logs.
*   **UI Elements:**
    *   **Analytics Graph:** Line/Bar chart of Revenue vs. Time.
    *   **Transaction List:** Chronological log of all incoming payments (Cash/Online).
    *   **Filters:** Date Range, Payment Mode.

---

## 5. Automated System Alerts (WhatsApp Integration)
The system should trigger WhatsApp messages (via deeplink or API integration if available) for:
1.  **Welcome Message:** Upon Member registration.
2.  **Payment Receipt:** Upon recording a fee payment.
3.  **Expiry Reminder:** Automated alerts 3 days and 1 day before plan expiry.
4.  **Birthday Wish:** Automated greeting.

---

## 6. Technical & Non-Functional Requirements
*   **Performance:** Dashboard load time < 2 seconds.
*   **Data Integrity:** Offline support for attendance marking (sync when online).
*   **Security:** Token-based authentication; Sensitive data encryption.
*   **Scalability:** Design database to handle multi-branch expansion in future versions.
