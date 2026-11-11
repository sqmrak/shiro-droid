#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include "engine/root_client.h"
#include "shiro_bytes.h"
#include "shiro_core.h"
#include "shiro_https.h"
#include "shiro_registration.h"
#include "shiro_routes.h"
#include "shiro_store.h"

static void fail(JNIEnv *env, const char *stage, shiro_status status, int http) {
    char text[256];
    jclass cls = (*env)->FindClass(env, "java/io/IOException");
    if (!cls) return;
    if (http) snprintf(text, sizeof(text), "%s: %s (HTTP %d)", stage, shiro_status_name(status), http);
    else snprintf(text, sizeof(text), "%s: %s", stage, shiro_status_name(status));
    (*env)->ThrowNew(env, cls, text);
}

static int unpack(JNIEnv *env, jbyteArray data, shiro_warp_identity *identity) {
    uint8_t packed[SHIRO_STORE_LEN];
    if (!data || (*env)->GetArrayLength(env, data) != SHIRO_STORE_LEN) return 0;
    (*env)->GetByteArrayRegion(env, data, 0, SHIRO_STORE_LEN, (jbyte *)packed);
    if ((*env)->ExceptionCheck(env)) return 0;
    shiro_status status = shiro_store_unpack(packed, sizeof(packed), identity);
    shiro_wipe(packed, sizeof(packed));
    return status == SHIRO_OK;
}

JNIEXPORT jbyteArray JNICALL Java_com_shiro_droid_Native_register(JNIEnv *env, jclass cls, jbyteArray roots, jstring proxy_url) {
    jbyteArray result = NULL;
    shiro_warp_identity identity;
    shiro_proxy proxy;
    shiro_status status;
    uint8_t packed[SHIRO_STORE_LEN];
    size_t packed_len = 0;
    int http = 0;
    (void)cls;
    memset(&identity, 0, sizeof(identity));
    memset(&proxy, 0, sizeof(proxy));
    if (!roots || !proxy_url) { fail(env, "registration", SHIRO_E_ARG, 0); return NULL; }
    jsize root_len = (*env)->GetArrayLength(env, roots);
    const char *proxy_text = (*env)->GetStringUTFChars(env, proxy_url, NULL);
    jbyte *root_bytes = (*env)->GetByteArrayElements(env, roots, NULL);
    if (!proxy_text || !root_bytes) goto done;
    status = shiro_proxy_parse(proxy_text, strlen(proxy_text), &proxy);
    if (status == SHIRO_OK) status = shiro_register((const char *)root_bytes, (size_t)root_len, &proxy, shiro_https_post, &identity, &http);
    if (status == SHIRO_OK) status = shiro_store_pack(&identity, packed, sizeof(packed), &packed_len);
    if (status != SHIRO_OK) { fail(env, "registration", status, http); goto done; }
    result = (*env)->NewByteArray(env, (jsize)packed_len);
    if (result) (*env)->SetByteArrayRegion(env, result, 0, (jsize)packed_len, (const jbyte *)packed);
done:
    if (root_bytes) (*env)->ReleaseByteArrayElements(env, roots, root_bytes, JNI_ABORT);
    if (proxy_text) (*env)->ReleaseStringUTFChars(env, proxy_url, proxy_text);
    shiro_wipe(&identity, sizeof(identity));
    shiro_wipe(&proxy, sizeof(proxy));
    shiro_wipe(packed, sizeof(packed));
    return result;
}

JNIEXPORT jstring JNICALL Java_com_shiro_droid_Native_address(JNIEnv *env, jclass cls, jbyteArray data) {
    shiro_warp_identity id;
    char text[16];
    (void)cls;
    if (!unpack(env, data, &id)) { fail(env, "account", SHIRO_E_MALFORMED, 0); return NULL; }
    snprintf(text, sizeof(text), "%u.%u.%u.%u", (unsigned)(id.address >> 24), (unsigned)((id.address >> 16) & 255),
             (unsigned)((id.address >> 8) & 255), (unsigned)(id.address & 255));
    shiro_wipe(&id, sizeof(id));
    return (*env)->NewStringUTF(env, text);
}

typedef struct { JNIEnv *env; jobject service; jmethodID method; } protector;
static int protect_socket(void *context, int fd) {
    protector *p = context;
    jboolean ok = (*p->env)->CallBooleanMethod(p->env, p->service, p->method, (jint)fd);
    return !(*p->env)->ExceptionCheck(p->env) && ok == JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_com_shiro_droid_Native_start(JNIEnv *env, jclass cls, jbyteArray data, jint tun, jstring direct,
                                                          jstring block, jint attempt, jobject service) {
    shiro_warp_identity id;
    shiro_routing routing;
    shiro_core *core = NULL;
    uint32_t ip = 0;
    uint16_t port = 0;
    const char *direct_text = NULL, *block_text = NULL;
    shiro_status status = SHIRO_E_ARG;
    protector p;
    jclass service_class;
    (void)cls;
    memset(&id, 0, sizeof(id));
    if (!service || !direct || !block || tun < 0 || attempt < 0 || !unpack(env, data, &id)) goto done;
    direct_text = (*env)->GetStringUTFChars(env, direct, NULL);
    block_text = (*env)->GetStringUTFChars(env, block, NULL);
    if (!direct_text || !block_text) goto done;
    if (shiro_routing_parse(direct_text, block_text, &routing) != 0) goto done;
    status = shiro_warp_endpoint(&id, (unsigned)attempt, &ip, &port);
    if (status != SHIRO_OK) goto done;
    service_class = (*env)->GetObjectClass(env, service);
    if (!service_class) goto done;
    p.method = (*env)->GetMethodID(env, service_class, "protect", "(I)Z");
    (*env)->DeleteLocalRef(env, service_class);
    if (!p.method) goto done;
    p.env = env; p.service = service;
    int flags = fcntl(tun, F_GETFL, 0);
    if (flags < 0 || fcntl(tun, F_SETFL, flags | O_NONBLOCK) < 0) { status = SHIRO_E_IO; goto done; }
    status = shiro_core_start_android(&core, &id, ip, port, NULL, tun, &routing, protect_socket, &p);
done:
    if (direct_text) (*env)->ReleaseStringUTFChars(env, direct, direct_text);
    if (block_text) (*env)->ReleaseStringUTFChars(env, block, block_text);
    shiro_wipe(&id, sizeof(id));
    if (status != SHIRO_OK && !(*env)->ExceptionCheck(env)) fail(env, "tunnel", status, 0);
    return (jlong)(intptr_t)core;
}

JNIEXPORT jint JNICALL Java_com_shiro_droid_Native_state(JNIEnv *env, jclass cls, jlong handle) {
    (void)env; (void)cls;
    return (jint)shiro_core_state((shiro_core *)(intptr_t)handle);
}
JNIEXPORT jlongArray JNICALL Java_com_shiro_droid_Native_stats(JNIEnv *env, jclass cls, jlong handle) {
    shiro_stats stats;
    jlong values[4];
    jlongArray result;
    (void)cls;
    memset(&stats, 0, sizeof(stats));
    shiro_core_stats((shiro_core *)(intptr_t)handle, &stats);
    values[0] = (jlong)stats.bytes_in; values[1] = (jlong)stats.bytes_out;
    values[2] = (jlong)stats.uptime_ms; values[3] = (jlong)stats.handshakes;
    result = (*env)->NewLongArray(env, 4);
    if (result) (*env)->SetLongArrayRegion(env, result, 0, 4, values);
    return result;
}
JNIEXPORT void JNICALL Java_com_shiro_droid_Native_stop(JNIEnv *env, jclass cls, jlong handle) {
    (void)env; (void)cls;
    if (handle) shiro_core_stop((shiro_core *)(intptr_t)handle);
}

JNIEXPORT jobjectArray JNICALL Java_com_shiro_droid_Native_routes(JNIEnv *env, jclass cls, jstring direct, jstring block) {
    const char *d = NULL, *b = NULL;
    shiro_routing rules;
    shiro_route routes[SHIRO_ROUTE_MAX];
    size_t count = 0, i;
    shiro_status status = SHIRO_E_ARG;
    jobjectArray result = NULL;
    jclass string_class;
    (void)cls;
    if (!direct || !block) goto done;
    d = (*env)->GetStringUTFChars(env, direct, NULL);
    b = (*env)->GetStringUTFChars(env, block, NULL);
    if (!d || !b || shiro_routing_parse(d, b, &rules) != 0) goto done;
    status = shiro_routes_build(&rules, routes, SHIRO_ROUTE_MAX, &count);
    if (status != SHIRO_OK) goto done;
    string_class = (*env)->FindClass(env, "java/lang/String");
    if (!string_class) goto done;
    result = (*env)->NewObjectArray(env, (jsize)count, string_class, NULL);
    (*env)->DeleteLocalRef(env, string_class);
    if (!result) goto done;
    for (i = 0; i < count; ++i) {
        uint32_t ip = routes[i].network;
        char text[24];
        jstring entry;
        snprintf(text, sizeof(text), "%u.%u.%u.%u/%u", (unsigned)(ip >> 24), (unsigned)((ip >> 16) & 255),
                 (unsigned)((ip >> 8) & 255), (unsigned)(ip & 255), routes[i].prefix);
        entry = (*env)->NewStringUTF(env, text);
        if (!entry) break;
        (*env)->SetObjectArrayElement(env, result, (jsize)i, entry);
        (*env)->DeleteLocalRef(env, entry);
        if ((*env)->ExceptionCheck(env)) break;
    }
done:
    if (d) (*env)->ReleaseStringUTFChars(env, direct, d);
    if (b) (*env)->ReleaseStringUTFChars(env, block, b);
    if (status != SHIRO_OK && !(*env)->ExceptionCheck(env)) fail(env, "routing rules", status, 0);
    return result;
}

JNIEXPORT jstring JNICALL Java_com_shiro_droid_Native_reason(JNIEnv *env, jclass cls, jlong handle) {
    (void)cls;
    return (*env)->NewStringUTF(env, shiro_status_name(shiro_core_reason((shiro_core *)(intptr_t)handle)));
}
JNIEXPORT void JNICALL Java_com_shiro_droid_Native_validateDns(JNIEnv *env, jclass cls, jstring dns, jstring direct, jstring block) {
    const char *s = NULL, *d = NULL, *b = NULL;
    uint32_t ip = 0;
    size_t i;
    shiro_routing rules;
    int ok = 0;
    (void)cls;
    if (!dns || !direct || !block) goto done;
    s = (*env)->GetStringUTFChars(env, dns, NULL);
    d = (*env)->GetStringUTFChars(env, direct, NULL);
    b = (*env)->GetStringUTFChars(env, block, NULL);
    if (!s || !d || !b || shiro_warp_parse_ipv4(s, strlen(s), &ip) != SHIRO_OK ||
        !ip || (ip >> 24) >= 224 || (ip >> 24) == 127 || shiro_routing_parse(d, b, &rules) != 0 ||
        shiro_routing_blocked(&rules, ip)) goto done;
    for (i = 0; i < rules.direct_count; ++i)
        if ((ip & rules.direct[i].mask) == rules.direct[i].network) goto done;
    ok = 1;
done:
    if (s) (*env)->ReleaseStringUTFChars(env, dns, s);
    if (d) (*env)->ReleaseStringUTFChars(env, direct, d);
    if (b) (*env)->ReleaseStringUTFChars(env, block, b);
    if (!ok && !(*env)->ExceptionCheck(env)) fail(env, "DNS must be an IPv4 address routed through WARP", SHIRO_E_ARG, 0);
}

JNIEXPORT jint JNICALL Java_com_shiro_droid_Native_rootOpen(JNIEnv *env, jclass cls, jstring path, jstring address,
                                                            jstring dns, jstring direct, jstring block) {
    const char *values[5] = {NULL, NULL, NULL, NULL, NULL};
    jstring inputs[5] = {path, address, dns, direct, block};
    tun_params_t params;
    char error[160] = "";
    int fd = -1, i;
    (void)cls;
    memset(&params, 0, sizeof(params));
    for (i = 0; i < 5; ++i) {
        if (!inputs[i]) goto done;
        values[i] = (*env)->GetStringUTFChars(env, inputs[i], NULL);
        if (!values[i]) goto done;
    }
    if (strlen(values[1]) >= sizeof(params.address4) || strlen(values[2]) >= sizeof(params.dns4) ||
        strlen(values[3]) >= sizeof(params.direct) || strlen(values[4]) >= sizeof(params.block)) {
        snprintf(error, sizeof(error), "the tunnel parameters are too long");
        goto done;
    }
    strcpy(params.address4, values[1]); strcpy(params.dns4, values[2]);
    strcpy(params.direct, values[3]); strcpy(params.block, values[4]);
    params.prefix4 = 32; params.mtu = SHIRO_WARP_MTU;
    fd = root_client_open_tun(values[0], &params, error, sizeof(error));
done:
    for (i = 0; i < 5; ++i) if (values[i]) (*env)->ReleaseStringUTFChars(env, inputs[i], values[i]);
    if (fd < 0 && !(*env)->ExceptionCheck(env)) {
        jclass exception = (*env)->FindClass(env, "java/io/IOException");
        if (exception) (*env)->ThrowNew(env, exception, error[0] ? error : "invalid root tunnel request");
    }
    return fd;
}
JNIEXPORT jboolean JNICALL Java_com_shiro_droid_Native_rootProtect(JNIEnv *env, jclass cls, jstring path, jint fd) {
    char error[160];
    const char *text;
    int status;
    (void)cls;
    if (!path) return JNI_FALSE;
    text = (*env)->GetStringUTFChars(env, path, NULL);
    if (!text) return JNI_FALSE;
    status = root_client_protect(text, fd, error, sizeof(error));
    (*env)->ReleaseStringUTFChars(env, path, text);
    return status == 0 ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL Java_com_shiro_droid_Native_closeFd(JNIEnv *env, jclass cls, jint fd) {
    (void)env; (void)cls;
    if (fd >= 0) close(fd);
}

JNIEXPORT jboolean JNICALL Java_com_shiro_droid_Native_rootPing(JNIEnv *env, jclass cls, jstring path) {
    char error[160];
    const char *text;
    int status;
    (void)cls;
    if (!path) return JNI_FALSE;
    text = (*env)->GetStringUTFChars(env, path, NULL);
    if (!text) return JNI_FALSE;
    status = root_client_ping(text, error, sizeof(error));
    (*env)->ReleaseStringUTFChars(env, path, text);
    return status == 0 ? JNI_TRUE : JNI_FALSE;
}
