CC ?= cc
BUILD ?= ../build/host
SRC := $(filter-out ../core/routing/route_message.c ../core/routing/tun_plan.c,$(wildcard ../core/*/*.c)) $(wildcard ../backend/*.c) ../rootd/rootd_firewall.c
INC := $(addprefix -I,$(sort $(dir $(wildcard ../core/*/*.h)))) -Icore -I../backend -I../rootd
CFLAGS := -std=c99 -D_POSIX_C_SOURCE=200809L -D_DEFAULT_SOURCE -O2 -g -Wall -Wextra -Werror -Wshadow -Wstrict-prototypes -Wmissing-prototypes -Wconversion -Wno-sign-conversion $(INC)
OBJ := $(patsubst ../%.c,$(BUILD)/%.o,$(SRC))
NAMES := test_shiro_hello test_root_firewall test_root_routes test_routes_android test_registration test_control_io test_log test_packet test_proxy test_routing test_shiro_core_packets test_shiro_android_packets test_shiro_core_udp test_shiro_crypto test_shiro_http test_shiro_https test_shiro_store test_shiro_tcpip test_shiro_udp test_shiro_warp test_shiro_wg test_socket_resolution
TESTS := $(addprefix $(BUILD)/,$(NAMES))
.PHONY: test clean
test: $(TESTS) $(BUILD)/test_rootd $(BUILD)/test_watchdog
	@set -e; for t in $(TESTS) $(BUILD)/test_rootd $(BUILD)/test_watchdog; do $$t; done
$(BUILD)/%.o: ../%.c
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -MMD -MP -c $< -o $@
$(BUILD)/test_socket_resolution: LDFLAGS += -Wl,--wrap=getaddrinfo
$(BUILD)/test_%: core/test_%.c $(OBJ) core/fake_tcp_server.c
	$(CC) $(CFLAGS) $< core/fake_tcp_server.c $(OBJ) -lpthread -lssl -lcrypto $(LDFLAGS) -o $@
clean:
	rm -rf $(BUILD)
-include $(OBJ:.o=.d)
.SECONDARY: $(OBJ)

$(BUILD)/test_rootd: test_rootd.c ../core/fdpass.c ../core/routing/shiro_routing.c ../rootd/rootd_proto.c ../rootd/rootd_serve.c ../engine/root_client.c
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -I.. -I../engine $^ -pthread -o $@

$(BUILD)/test_watchdog: test_watchdog.c ../rootd/rootd_firewall.c ../rootd/rootd_proto.c ../core/routing/shiro_routing.c ../backend/shiro_root_routes.c
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -I.. $^ -o $@
