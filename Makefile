ABIS ?= armeabi armeabi-v7a arm64-v8a x86 x86_64
SHIRODROID_API ?= 9
BUILD ?= build/api$(SHIRODROID_API)
SHIRODROID_ANDROID ?= $(abspath ../android/android-toolchain)
JAVA_HOME ?= $(firstword $(wildcard $(SHIRODROID_ANDROID)/jdk-17*))
ANDROID_HOME ?= $(SHIRODROID_ANDROID)/sdk
GRADLE_USER_HOME ?= $(SHIRODROID_ANDROID)/gradle-home
SHIRODROID_AAPT2 ?= $(wildcard $(SHIRODROID_ANDROID)/bin/aapt2)
export SHIRODROID_API JAVA_HOME ANDROID_HOME GRADLE_USER_HOME
export SHIRODROID_BUILD := $(abspath $(BUILD))
GRADLE = ./gradlew --no-daemon -Pshiro.jnilibs=$(abspath $(BUILD))/jniLibs $(if $(SHIRODROID_AAPT2),-Pandroid.aapt2FromMavenOverride=$(SHIRODROID_AAPT2))
.SECONDARY:
.PHONY: test native apk clean FORCE

test:
	$(MAKE) -C tests test
$(BUILD)/openssl-%/lib/libssl.a:
	scripts/build_openssl_android.sh $*
FORCE:
$(BUILD)/jniLibs/%/libshiro.so: $(BUILD)/openssl-%/lib/libssl.a FORCE
	$(MAKE) -f android.mk ABI=$* BUILD=$(BUILD)
	mkdir -p $(dir $@)
	$(SHIRODROID_LLVM)/bin/llvm-strip --strip-unneeded -o $@ $(BUILD)/android/$*/libshiro.so
$(BUILD)/jniLibs/%/libshirorootd.so: $(BUILD)/jniLibs/%/libshiro.so FORCE
	$(MAKE) -f android.mk ABI=$* BUILD=$(BUILD) root-helper
	mkdir -p $(dir $@)
	$(SHIRODROID_LLVM)/bin/llvm-strip --strip-unneeded -o $@ $(BUILD)/android/$*/libshirorootd.so
native: $(foreach abi,$(ABIS),$(BUILD)/jniLibs/$(abi)/libshiro.so) $(foreach abi,$(filter armeabi armeabi-v7a x86,$(ABIS)),$(BUILD)/jniLibs/$(abi)/libshirorootd.so)
apk: native
	$(GRADLE) :app:assembleDebug
clean:
	$(MAKE) -C tests clean
	rm -rf $(BUILD)/android $(BUILD)/jniLibs app/build

HOST_SRC := $(filter-out core/routing/route_message.c core/routing/tun_plan.c,$(wildcard core/*/*.c)) backend/shiro_registration.c backend/shiro_routes.c jni/shiro_jni.c engine/root_client.c core/fdpass.c
HOST_INC := $(addprefix -I,$(sort $(dir $(wildcard core/*/*.h)))) -Ibackend -I. -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
.PHONY: test-java test-android
test-java:
	mkdir -p build/host-jni/classes
	$(CC) -std=c99 -D_GNU_SOURCE -fPIC -shared $(HOST_INC) $(HOST_SRC) -lpthread -lssl -lcrypto -o build/host-jni/libshiro.so
	$(JAVA_HOME)/bin/javac -d build/host-jni/classes app/src/main/java/com/shiro/droid/Native.java tests/java/com/shiro/droid/NativeTest.java
	$(JAVA_HOME)/bin/java -Djava.library.path=build/host-jni -cp build/host-jni/classes com.shiro.droid.NativeTest

test-android: test-java
	SHIRODROID_HOST_LIB_DIR=$(abspath build/host-jni) $(GRADLE) :app:testDebugUnitTest
