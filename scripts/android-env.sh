# shared toolchain setup, sourced by the android build scripts
# every root is overrideable, nothing here names a home directory

: "${SHIRODROID_LLVM:?set SHIRODROID_LLVM to the ndk llvm prebuilt directory}"
: "${SHIRODROID_SYSROOT:?set SHIRODROID_SYSROOT to the ndk sysroot}"
SHIRODROID_API="${SHIRODROID_API:-9}"
export SHIRODROID_API

[[ -x "${SHIRODROID_LLVM}/bin/clang" ]] || { echo "no clang in ${SHIRODROID_LLVM}/bin" >&2; return 1; }
[[ -d "${SHIRODROID_SYSROOT}/usr/lib" ]] || { echo "no sysroot at ${SHIRODROID_SYSROOT}" >&2; return 1; }

# abi name -> clang triple, openssl target, cpu flags
shirodroid_abi() {
  local here
  here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  ABI_TRIPLE="$("${here}/android-abi.sh" "$1" triple)"
  ABI_SSL="$("${here}/android-abi.sh" "$1" ssl)"
  ABI_CFLAGS="$("${here}/android-abi.sh" "$1" cflags)"
  ABI_SYSROOT="$("${here}/android-sysroot.sh" "$1")"
}
