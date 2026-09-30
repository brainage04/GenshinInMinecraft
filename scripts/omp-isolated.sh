#!/usr/bin/env bash
# Start this project's omp session isolated from the owner's desktop.
#
# - Resources: everything the session starts (Gradle, servers, clients, Xvfb)
#   shares one cgroup capped at 24 GiB RAM (reclaim from 20 GiB), 2 GiB swap,
#   and low CPU/IO weight, so interactive use always wins under contention.
# - Display: no DISPLAY/XAUTHORITY, and WAYLAND_DISPLAY points at a socket that
#   does not exist (unset would fall back to the desktop's wayland-0). The
#   session D-Bus is pointed at /dev/null, so nothing can open windows, portal
#   dialogs or desktop notifications on the owner's screen. GUI work runs on a
#   private headless compositor instead (see "Isolation" in docs/GENSHIN_MINECRAFT_BRIEF.md).
# - Audio: PulseAudio/PipeWire are pointed at nonexistent sockets and OpenAL
#   uses its null driver, so game sound never plays on the owner's speakers.
set -euo pipefail
cd "$(dirname "$0")/.."
exec systemd-run --user --scope --quiet --unit="genshin-omp-$$" \
  -p MemoryHigh=20G -p MemoryMax=24G -p MemorySwapMax=2G \
  -p CPUWeight=20 -p IOWeight=20 -p TasksMax=4096 \
  env -u DISPLAY -u XAUTHORITY \
  WAYLAND_DISPLAY=/nonexistent/wayland \
  DBUS_SESSION_BUS_ADDRESS=unix:path=/dev/null \
  PULSE_SERVER=unix:/nonexistent/pulse PIPEWIRE_REMOTE=/nonexistent/pipewire \
  ALSOFT_DRIVERS=null \
  PI_NO_DESKTOP_NOTIFY=1 \
  omp "$@"
