package `in`.gym.trak.studio.network

object ApiEndpoints {
    // Corrected production base URL
    // live url
    const val BASE_URL = "https://api.gym.anantalabs.in/api/v1/"
    // local url
//    const val BASE_URL = "http://192.168.1.73:3000/api/v1/"



    object App {
        const val CONFIG = "app/config"
    }

    object Core {
        //        const val DASHBOARD = "dashboard"
        const val OWNER_DASHBOARD = "dashboard/owner"
        const val PROFILE = "profile"
        const val EXERCISES = "exercises"
        const val META_ALL = "meta/all"
        const val UPDATE_OWNER_PROFILE = "profile/me/owner"

        const val UPDATE_TRAINER_PROFILE = "profile/me/trainer"
    }

    object Auth {
        const val LOGIN = "auth/login"
        const val REGISTER = "auth/register"
        const val VERIFY_OTP = "auth/verify-otp"
        const val REFRESH_TOKEN = "auth/refresh"
        const val LOGOUT = "auth/logout"
        const val RESEND_OTP = "auth/resend-otp"
    }

    object Gym {
        const val SET_GYM = "gym"
    }

    /** List (GET), create (POST), update (PATCH), delete (DELETE). */
    object Gyms {
        const val LIST = "gyms"
        fun detail(id: String) = "gyms/$id"
    }

    object User {
        const val SELECT_ROLE = "user/select-role"
    }

    object Owner {
        const val SWITCH_TO_MEMBER = "owner/switch-to-member"
        const val SWITCH_TO_OWNER = "owner/switch-to-owner"
        const val PROFILE_STATUS = "owner/profile-status"
    }

    object Members {
        const val GET_ALL = "members"
        const val DETAILS = "members/details"
        const val ADD = "members"
        const val DASHBOARD = "members/dashboard"
        const val STATISTICS = "members/statistics"
        fun detail(id: String) = "members/$id"
        fun memberProfile(id: String) = "members/$id/profile"
        fun convert(id: String) = "members/$id/convert"
        fun attendanceSummary(id: String) = "members/$id/attendance/summary"
        fun workouts(id: String) = "members/$id/workouts"
    }

    object MemberOnboarding {
        const val COMPLETE_PROFILE = "onboarding/member"
    }


    object Plans {
        const val LIST = "plans"
        const val COMPAT_CREATE = "plans/compat"
        /** Assign gym plan to member (creates subscription). */
        const val MEMBER_PLANS = "plans/member-plans"
        /** Freeze member subscription using member_subscription_id in body. */
        const val FREEZE = "plans/freeze"
        /** Unfreeze member subscription using member_subscription_id in body. */
        const val UNFREEZE = "plans/unfreeze"
        fun detail(id: String) = "plans/$id"
        fun enrolled(id: String) = "plans/$id/enrolled"


    }

    object Subscriptions {
        const val LIST = "subscriptions"
    }

    object Payments {
        const val BASE = "payments"
        const val ANALYTICS = "payments/analytics"
        const val SALARY = "payments/salary"
    }


    object Trainers {
        const val LIST = "trainers"

        //        const val CREATE = "trainers"
        const val COMPAT_CREATE = "trainers/compat"

        /** Path only; pass `gymId` and optional `role` (TRAINER | STAFF) as query params for GET/PATCH/DELETE. */
        fun detail(id: String) = "trainers/$id"
        fun password(id: String) = "trainers/$id/password"
        fun attendancePunch(id: String) = "trainers/$id/attendance/punch"
    }

    object Uploads {
        const val IMAGE = "uploads/images"
    }

    object Enquiries {
        const val BASE = "enquiries"
        fun detail(id: String) = "enquiries/$id"
        fun convert(id: String) = "enquiries/$id/convert"
    }

    object Workouts {
        const val BASE = "workouts"
        const val HISTORY = "workouts/history"
        const val START = "workouts/start"
        const val STOP = "workouts/stop"
        const val PAUSE = "workouts/pause"
        const val RESUME = "workouts/resume"
        fun detail(workoutId: String) = "workouts/$workoutId"
        fun complete(workoutId: String) = "workouts/$workoutId/complete"
        fun memberCompletion(workoutId: String) = "members/workouts/$workoutId/completion"
        fun trainerDelete(workoutId: String) = "trainers/workouts/$workoutId"
    }

    object ExerciseSets {
        const val BASE = "exercise-sets"
    }

    object Sets {
        const val BASE = "sets"
        fun detail(setId: String) = "sets/$setId"
    }

    object Diet {
        const val MEALS = "diet"
        const val FOOD = "diet/food"
        const val FOOD_CONSUME = "diet/food-consume"
        const val HISTORY = "diet/history"
        fun mealDetail(id: String) = "diet/$id"
    }

    object Expenses {
        const val BASE = "expenses"
        fun detail(id: String) = "expenses/$id"
    }

    object Leaves {
        const val BASE = "leaves"
        fun approve(id: String) = "leaves/$id/approve"
        fun reject(id: String) = "leaves/$id/reject"
    }

    object Products {
        const val BASE = "products"
        fun detail(id: String) = "products/$id"
    }

    object AttendanceQr {
        const val GENERATE = "attendance-qr/generate"
        const val REGENERATE = "attendance-qr/regenerate"
        const val GYM_STATIC = "attendance-qr/gym-static"
        const val MY_QR = "attendance-qr/my-qr"
        const val PUNCH = "attendance-qr/punch"
    }

    object Notifications {
        const val BASE = "notifications"
        const val DEVICE_TOKEN = "notifications/device-token"
        fun read(id: String) = "notifications/$id/read"
    }

    object Broadcast {
        const val CHANNELS = "broadcast/channels"
        const val MEMBERS_LIST = "broadcast/channels/members-list"
        fun members(channelId: String) = "broadcast/channels/$channelId/members"
        fun addMembers(channelId: String) = "broadcast/channels/$channelId/members"
        fun messages(channelId: String) = "broadcast/channels/$channelId/messages"
        fun detail(channelId: String) = "broadcast/channels/$channelId"
        fun delete(channelId: String) = "broadcast/channels/$channelId"
    }

    object Favorites {
        const val BASE = "favorites"
        fun detail(productId: String) = "favorites/$productId"
    }

    object Leaderboard {
        const val BASE = "leaderboard"
    }

    object WhatsApp {
        const val AUTOMATION = "whatsapp/automation"
    }
}

