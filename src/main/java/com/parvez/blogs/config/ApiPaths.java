package com.parvez.blogs.config;

public final class ApiPaths {

    private ApiPaths() {
    }

    /* ================= BASE ================= */
    public static final String API_BASE = "/api/v1";
    public static final String ADMIN = API_BASE + "/admin";

    /* ================= AUTH ================= */
    public static final String AUTH = API_BASE + "/auth/**";
    public static final String LOGIN = API_BASE + "/auth/login";
    public static final String REGISTER = API_BASE + "/auth/register";
    public static final String FORGOT_PASSWORD = API_BASE + "/auth/forgot-password";
    public static final String RESET_PASSWORD = API_BASE + "/auth/reset-password";
    public static final String LOGOUT = API_BASE + "/auth/logout";
    public static final String REFRESH_TOKEN = API_BASE + "/auth/refresh-token";

    /* ================= HOME ================= */
    public static final String HOME = API_BASE + "/home";

    /* ================= POST ================= */
    public static final String POSTS = API_BASE + "/post";

    /* ================= CATEGORY ================= */
    public static final String CATEGORIES = API_BASE + "/categories/**";

    /* ================= EXPENSE ================= */
    public static final String EXPENSES = API_BASE + "/expenses/**";

    /* ================= EMPLOYEE ================= */
    public static final String EMPLOYEES = API_BASE + "/employees/**";
    public static final String SALARY_INCREMENT = API_BASE + "/employees/*/salary/increment";
}