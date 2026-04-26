package io.vainslab.onemoresubscriber.bot;

public final class CallbackPrefix {

    public static final String BACK = "bk";
    public static final String BACK_TO_SERVICE = "bks:";
    public static final String SERVICE = "s:";
    public static final String OPERATION = "op:";
    public static final String DELETE_PAYMENT = "dpx:";
    public static final String DELETE_TIP = "dtx:";

    // Admin prefixes
    public static final String ADMIN_SERVICE = "adm:";
    public static final String ADMIN_REPORT_WEEK = "a:rw:";
    public static final String ADMIN_REPORT_MONTH = "a:rm:";
    public static final String ADMIN_USERS = "a:u:";
    public static final String ADMIN_USER_DETAIL = "a:ud:";
    public static final String ADMIN_KICK = "a:uk:";
    public static final String ADMIN_KICK_CONFIRM = "a:ukx:";
    public static final String ADMIN_BILLING_PAUSE = "a:bp:";
    public static final String ADMIN_BILLING_RESUME = "a:br:";
    public static final String ADMIN_BILLING_RESUME_CONFIRM = "a:brx:";
    public static final String ADMIN_BACK = "a:back";

    private CallbackPrefix() {
    }
}
