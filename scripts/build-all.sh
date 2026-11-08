#!/usr/bin/env bash
set -euo pipefail

root=$(git rev-parse --show-toplevel)
temporary=$(mktemp -d "${TMPDIR:-/tmp}/spdm-build.XXXXXX")
output="$root/build/libs"
mkdir -p "$output"

cleanup() {
    git -C "$root" worktree remove --force "$temporary/neoforge" 2>/dev/null || true
    git -C "$root" worktree remove --force "$temporary/forge" 2>/dev/null || true
    rmdir "$temporary" 2>/dev/null || true
}
trap cleanup EXIT

java_home() {
    local version="$1"
    local variable="JAVA${version}_HOME"
    if [[ -n "${!variable:-}" ]]; then
        printf '%s\n' "${!variable}"
    elif [[ -x /usr/libexec/java_home ]]; then
        /usr/libexec/java_home -v "$version"
    else
        printf 'Set %s to your Java %s installation.\n' "$variable" "$version" >&2
        return 1
    fi
}

git -C "$root" worktree add --detach "$temporary/neoforge" 1.21.1
git -C "$root" worktree add --detach "$temporary/forge" 1.20.1

for loader in neoforge forge; do
    version=21
    [[ "$loader" == forge ]] && version=17
    (
        cd "$temporary/$loader"
        JAVA_HOME=$(java_home "$version") ./gradlew build --console=plain
        cp build/libs/*.jar "$output/"
    )
done

printf '\nArtifacts: %s\n' "$output"
