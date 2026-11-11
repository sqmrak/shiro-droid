#include "shiro_registration.h"
#include "test_util.h"

static int calls, mode;
static shiro_status fake_post(const shiro_https_request *request, shiro_https_response *response) {
    static const char answer[] = "{\"config\":{\"client_id\":\"AEOY\",\"peers\":[{\"public_key\":\"bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=\",\"endpoint\":{\"v4\":\"162.159.192.1:0\",\"ports\":[2408]}}],\"interface\":{\"addresses\":{\"v4\":\"172.16.0.2\"}}}}";
    ++calls;
    CHECK(strcmp(request->host, "api.cloudflareclient.com") == 0);
    CHECK(strcmp(request->path, "/v0a4471/reg") == 0);
    CHECK(request->roots_len != 0);
    CHECK(request->body_len > 0);
    CHECK_EQ_INT(request->split_hello, calls);
    memset(response, 0, sizeof(*response));
    response->stage = mode == 5 ? "direct connection" : "TLS handshake";
    if (mode == 1 && calls == 1) return SHIRO_E_TIMEOUT;
    if (mode == 2) return SHIRO_E_SECURITY;
    if (mode == 4 || mode == 5) return SHIRO_E_TIMEOUT;
    response->status = mode == 3 ? 429 : 200;
    response->body_len = sizeof(answer) - 1;
    response->body = malloc(sizeof(answer));
    if (!response->body) return SHIRO_E_NOMEM;
    memcpy(response->body, answer, sizeof(answer));
    return SHIRO_OK;
}

int main(void) {
    shiro_warp_identity identity;
    shiro_proxy proxy;
    int http;
    memset(&proxy, 0, sizeof(proxy));
    calls = 0; mode = 0;
    CHECK_EQ_INT(shiro_register("roots", 5, NULL, fake_post, &identity, &http), SHIRO_OK);
    CHECK_EQ_INT(identity.address, 0xac100002u); CHECK_EQ_INT(http, 200); CHECK_EQ_INT(calls, 1);
    calls = 0; mode = 1;
    CHECK_EQ_INT(shiro_register("roots", 5, NULL, fake_post, &identity, &http), SHIRO_OK);
    CHECK_EQ_INT(calls, 2);
    calls = 0; mode = 2;
    CHECK_EQ_INT(shiro_register("roots", 5, NULL, fake_post, &identity, &http), SHIRO_E_SECURITY);
    CHECK_EQ_INT(identity.address, 0); CHECK_EQ_INT(calls, 1);
    calls = 0; mode = 3;
    CHECK_EQ_INT(shiro_register("roots", 5, NULL, fake_post, &identity, &http), SHIRO_E_RPC);
    CHECK_EQ_INT(http, 429); CHECK_EQ_INT(identity.address, 0);
    calls = 0; mode = 4;
    CHECK_EQ_INT(shiro_register("roots", 5, NULL, fake_post, &identity, &http), SHIRO_E_TIMEOUT);
    CHECK_EQ_INT(calls, 2); CHECK_EQ_INT(identity.address, 0);
    calls = 0; mode = 5;
    CHECK_EQ_INT(shiro_register("roots", 5, NULL, fake_post, &identity, &http), SHIRO_E_TIMEOUT);
    CHECK_EQ_INT(calls, 1);
    proxy.type = SHIRO_PROXY_HTTP;
    calls = 0; mode = 1;
    CHECK_EQ_INT(shiro_register("roots", 5, &proxy, fake_post, &identity, &http), SHIRO_E_TIMEOUT);
    CHECK_EQ_INT(calls, 1);
    CHECK_EQ_INT(shiro_register(NULL, 0, NULL, fake_post, &identity, &http), SHIRO_E_ARG);
    return test_finish("registration");
}
