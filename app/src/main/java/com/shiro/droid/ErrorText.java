package com.shiro.droid;

import android.content.Context;

final class ErrorText {
    private ErrorText() {}
    private static final String[] STATUS = {
        "bad argument", "malformed input", "truncated input", "unsupported input", "limit exceeded",
        "out of memory", "crypto failure", "validation failed", "wrong state", "timeout", "io error",
        "end of stream", "retry", "not found", "rpc error", "storage error", "proxy authentication failed",
        "proxy refused connection", "unknown status"
    };
    private static final int[] STATUS_TEXT = {
        R.string.error_status_argument, R.string.error_status_malformed, R.string.error_status_truncated,
        R.string.error_status_unsupported, R.string.error_status_limit, R.string.error_status_memory,
        R.string.error_status_crypto, R.string.error_status_security, R.string.error_status_state,
        R.string.error_status_timeout, R.string.error_status_io, R.string.error_status_eof,
        R.string.error_status_retry, R.string.error_status_notfound, R.string.error_status_rpc,
        R.string.error_status_storage, R.string.error_status_proxy_auth, R.string.error_status_proxy,
        R.string.error_status_unknown
    };
    private static final String[] EXACT = {
        "VPN permission was revoked", "VPN permission was refused or revoked", "VPN permission was refused",
        "VPN service stopped", "root helper stopped", "the previous root helper is still cleaning up",
        "the root helper is not part of this build for this cpu", "root access is needed on this android and su could not be started",
        "root access was not granted in time", "interrupted while waiting for root access",
        "root access was refused or the helper could not start", "the saved WARP account is damaged",
        "cannot save the WARP account", "file is too large", "registration failed", "connection failed"
    };
    private static final int[] EXACT_TEXT = {
        R.string.error_vpn_revoked, R.string.error_vpn_refused_or_revoked, R.string.vpn_refused,
        R.string.error_vpn_stopped, R.string.error_root_stopped, R.string.error_root_cleanup,
        R.string.error_root_missing, R.string.error_root_su, R.string.error_root_timeout,
        R.string.error_root_interrupted, R.string.error_root_refused, R.string.error_account_damaged,
        R.string.error_account_save, R.string.error_file_large, R.string.error_registration_failed, R.string.connection_failed
    };
    private static final String[] PREFIX = {
        "the system refused VPN settings: ", "VPN permission is missing: ", "VPN service is unavailable: ",
        "the app path has a character the root helper cannot take: "
    };
    private static final int[] PREFIX_TEXT = {
        R.string.error_vpn_settings, R.string.error_vpn_missing, R.string.error_vpn_unavailable, R.string.error_root_path
    };

    private static final String[] ROOT = {
        "the root helper socket path is unusable",
        "no socket for the root helper",
        "the root helper is not running",
        "the root helper did not take the request",
        "the root helper did not answer",
        "the root helper closed the connection",
        "the root helper answered without a tun device",
        "invalid routing rules",
        "the tunnel needs an ipv4 address and a dns server",
        "the tunnel request does not fit",
        "cannot open /dev/tun",
        "TUNSETIFF failed",
        "no control socket",
        "cannot configure the interface",
        "cannot safely plan the system routes",
        "cannot render a tunnel route",
        "cannot add a tunnel route",
        "cannot start firewall watchdog",
        "cannot install DNS or IPv6 firewall rules",
        "no network has a default route",
        "cannot read /proc/net/route",
        "SO_BINDTODEVICE failed",
    };
    private static final int[] ROOT_TEXT = {
        R.string.error_root_socket_path,
        R.string.error_root_socket_create,
        R.string.error_root_not_running,
        R.string.error_root_request,
        R.string.error_root_answer,
        R.string.error_root_closed,
        R.string.error_root_tun_missing,
        R.string.error_root_routes_invalid,
        R.string.error_root_needs_addresses,
        R.string.error_root_request_large,
        R.string.error_root_tun_open,
        R.string.error_root_tun_create,
        R.string.error_root_control_socket,
        R.string.error_root_iface_config,
        R.string.error_root_route_plan,
        R.string.error_root_route_render,
        R.string.error_root_route_add,
        R.string.error_root_watchdog,
        R.string.error_root_firewall,
        R.string.error_root_no_route,
        R.string.error_root_route_read,
        R.string.error_root_bind,
    };

    static String format(Context context, String error) {
        if (error == null || error.length() == 0) return "";
        if (error.equals("registration: timeout")) return context.getString(R.string.registration_timeout);
        if (error.equals("DNS must be an IPv4 address routed through WARP: bad argument")) return context.getString(R.string.error_dns_invalid);
        for (int i = 0; i < EXACT.length; i++) if (error.equals(EXACT[i])) return context.getString(EXACT_TEXT[i]);
        for (int i = 0; i < PREFIX.length; i++) {
            if (error.startsWith(PREFIX[i])) return detail(context.getString(PREFIX_TEXT[i]), error.substring(PREFIX[i].length()));
        }
        for (int i = 0; i < ROOT.length; i++) {
            if (error.equals(ROOT[i])) return context.getString(ROOT_TEXT[i]);
            if (error.startsWith(ROOT[i] + ": ")) return detail(context.getString(ROOT_TEXT[i]), error.substring(ROOT[i].length() + 2));
        }
        int stage = 0;
        String reason = error;
        if (error.startsWith("registration: ")) { stage = R.string.error_stage_registration; reason = error.substring(14); }
        else if (error.startsWith("account: ")) { stage = R.string.error_stage_account; reason = error.substring(9); }
        else if (error.startsWith("tunnel: ")) { stage = R.string.error_stage_tunnel; reason = error.substring(8); }
        else if (error.startsWith("routing rules: ")) { stage = R.string.error_stage_routing; reason = error.substring(15); }
        String http = "";
        int suffix = reason.indexOf(" (HTTP ");
        if (suffix >= 0 && reason.endsWith(")")) { http = reason.substring(suffix); reason = reason.substring(0, suffix); }
        for (int i = 0; i < STATUS.length; i++) {
            if (reason.equals(STATUS[i])) {
                String text = context.getString(STATUS_TEXT[i]) + http;
                return stage == 0 ? text : context.getString(stage) + ": " + text;
            }
        }
        if (error.startsWith("the root helper") || error.startsWith("root helper") || error.startsWith("cannot open /dev/tun") ||
                error.startsWith("TUNSETIFF") || error.startsWith("cannot configure the interface") ||
                error.startsWith("cannot add a tunnel route") || error.startsWith("cannot install DNS") ||
                error.startsWith("no network has a default route")) return detail(context.getString(R.string.error_root_error), error);
        return detail(context.getString(R.string.error_unexpected_error), error);
    }
    private static String detail(String title, String detail) {
        return detail.length() == 0 ? title : title + ": " + detail;
    }
}
