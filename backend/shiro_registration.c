#include "shiro_registration.h"
#include "shiro_bytes.h"
#include "shiro_random.h"
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>

#define INSTALL_ALPHABET "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

static shiro_status install_id(char out[SHIRO_WARP_INSTALL_ID_LEN + 1]) {
    size_t filled = 0;
    unsigned round;
    for (round = 0; round < 8 && filled < SHIRO_WARP_INSTALL_ID_LEN; ++round) {
        uint8_t bytes[32];
        size_t i;
        shiro_status status = shiro_random(bytes, sizeof(bytes));
        if (status != SHIRO_OK) return status;
        for (i = 0; i < sizeof(bytes) && filled < SHIRO_WARP_INSTALL_ID_LEN; ++i)
            if (bytes[i] < 248) out[filled++] = INSTALL_ALPHABET[bytes[i] % 62];
        shiro_wipe(bytes, sizeof(bytes));
    }
    out[filled] = '\0';
    return filled == SHIRO_WARP_INSTALL_ID_LEN ? SHIRO_OK : SHIRO_E_CRYPTO;
}

static shiro_status timestamp(char out[32]) {
    struct timeval tv;
    struct tm parts;
    int n;
    if (gettimeofday(&tv, NULL) != 0 || !gmtime_r(&tv.tv_sec, &parts)) return SHIRO_E_IO;
    n = snprintf(out, 32, "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ", parts.tm_year + 1900,
                 parts.tm_mon + 1, parts.tm_mday, parts.tm_hour, parts.tm_min, parts.tm_sec,
                 (int)(tv.tv_usec / 1000));
    return n == 24 ? SHIRO_OK : SHIRO_E_IO;
}

shiro_status shiro_register(const char *roots, size_t roots_len, const shiro_proxy *proxy,
                            shiro_registration_post post, shiro_warp_identity *identity, int *http_status) {
    static const char *const headers[] = {"Content-Type: application/json; charset=UTF-8", "User-Agent: okhttp/3.12.1",
                                          "CF-Client-Version: a-6.38-4471", NULL};
    uint8_t private_key[32] = {0}, public_key[32] = {0};
    char id[SHIRO_WARP_INSTALL_ID_LEN + 1], tos[32], body[1024];
    size_t body_len = 0;
    shiro_https_request request;
    shiro_https_response response;
    shiro_status status;
    int split;
    if (identity) memset(identity, 0, sizeof(*identity));
    if (http_status) *http_status = 0;
    if (!roots || !roots_len || !identity || !http_status || !post) return SHIRO_E_ARG;
    memset(&request, 0, sizeof(request));
    memset(&response, 0, sizeof(response));
    status = shiro_warp_generate_key(private_key, public_key);
    if (status == SHIRO_OK) status = install_id(id);
    if (status == SHIRO_OK) status = timestamp(tos);
    if (status == SHIRO_OK) status = shiro_warp_registration_body(public_key, id, tos, body, sizeof(body), &body_len);
    if (status != SHIRO_OK) goto done;
    request.host = "api.cloudflareclient.com";
    request.port = 443;
    request.path = "/v0a4471/reg";
    request.headers = headers;
    request.body = (const uint8_t *)body;
    request.body_len = body_len;
    request.roots_pem = roots;
    request.roots_len = roots_len;
    request.timeout_ms = 15000;
    request.proxy = proxy && proxy->type != SHIRO_PROXY_NONE ? proxy : NULL;
    /* start with an SNI split, then try the midpoint when the handshake stalls or resets */
    for (split = 1; split <= (request.proxy ? 1 : 2); ++split) {
        request.split_hello = split;
        status = post(&request, &response);
        *http_status = response.status;
        if (status == SHIRO_OK || (status != SHIRO_E_IO && status != SHIRO_E_TIMEOUT) ||
            !response.stage || strcmp(response.stage, "TLS handshake") != 0) break;
        shiro_https_response_free(&response);
    }
    if (status == SHIRO_OK) {
        if (response.status < 200 || response.status > 299) status = SHIRO_E_RPC;
        else status = shiro_warp_parse_registration((const char *)response.body, response.body_len, private_key, identity);
    }
    shiro_https_response_free(&response);
done:
    if (status != SHIRO_OK) shiro_wipe(identity, sizeof(*identity));
    shiro_wipe(private_key, sizeof(private_key));
    shiro_wipe(public_key, sizeof(public_key));
    shiro_wipe(body, sizeof(body));
    return status;
}
