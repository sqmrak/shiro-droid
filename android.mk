ABI ?= armeabi-v7a
SHIRODROID_API ?= 9
BUILD ?= build/api$(SHIRODROID_API)
SHIRODROID_LLVM ?= $(error set SHIRODROID_LLVM)
SHIRODROID_SYSROOT ?= $(error set SHIRODROID_SYSROOT)
export SHIRODROID_API
TRIPLE := $(shell scripts/android-abi.sh $(ABI) triple)
ABIFLAGS := $(shell scripts/android-abi.sh $(ABI) cflags)
SYSROOT := $(shell SHIRODROID_BUILD=$(abspath $(BUILD)) scripts/android-sysroot.sh $(ABI))
OSSL := $(BUILD)/openssl-$(ABI)
OUT := $(BUILD)/android/$(ABI)
CC := $(SHIRODROID_LLVM)/bin/clang --target=$(TRIPLE) --sysroot=$(SYSROOT)
INC := $(addprefix -I,$(sort $(dir $(wildcard core/*/*.h)))) -Ibackend -I. -I$(OSSL)/include
SRC := $(filter-out core/routing/route_message.c core/routing/tun_plan.c,$(wildcard core/*/*.c)) backend/shiro_registration.c backend/shiro_routes.c jni/shiro_jni.c engine/root_client.c core/fdpass.c
SRC += $(if $(filter armeabi,$(ABI)),backend/armv5_compat.c)
OBJ := $(patsubst %.c,$(OUT)/obj/%.o,$(SRC))
CFLAGS := -MMD -MP -std=c99 -D_GNU_SOURCE $(ABIFLAGS) -O2 -fPIC -fvisibility=hidden -ffunction-sections -fdata-sections -Wall -Wextra -Werror=implicit-function-declaration $(INC)
LDFLAGS := -shared -Wl,--gc-sections -Wl,--no-undefined -Wl,-z,relro -Wl,-z,now -Wl,--exclude-libs,ALL
ifneq ($(filter arm64-v8a x86_64,$(ABI)),)
LDFLAGS += -Wl,-z,max-page-size=16384
endif
.PHONY: all
all: $(OUT)/libshiro.so
$(OUT)/obj/%.o: %.c
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -c $< -o $@
$(OUT)/libshiro.so: $(OBJ) $(OSSL)/lib/libssl.a $(OSSL)/lib/libcrypto.a
	$(CC) $(ABIFLAGS) $(LDFLAGS) $(OBJ) $(OSSL)/lib/libssl.a $(OSSL)/lib/libcrypto.a -llog -lz -ldl -lm -o $@
-include $(OBJ:.o=.d)

ROOT_SRC := rootd/shiro_rootd.c rootd/rootd_linux.c rootd/rootd_proto.c rootd/rootd_serve.c rootd/rootd_firewall.c core/fdpass.c core/routing/shiro_routing.c backend/shiro_root_routes.c
ROOT_OBJ := $(patsubst %.c,$(OUT)/obj/%.o,$(ROOT_SRC))
.PHONY: root-helper
root-helper: $(OUT)/libshirorootd.so
$(OUT)/libshirorootd.so: $(ROOT_OBJ)
	$(CC) $(ABIFLAGS) -no-pie -Wl,--gc-sections -Wl,-z,relro -Wl,-z,now $(ROOT_OBJ) -o $@
-include $(ROOT_OBJ:.o=.d)
