#!/usr/bin/env bash

# -----------------------------------------------------------------------------
# OSC 9;4 progress helper with broad terminal detection + tmux support
# -----------------------------------------------------------------------------

# Detect whether we should emit OSC 9;4
supports_osc94() {
  # Explicit terminal programs
  case "${TERM_PROGRAM:-}" in
    iTerm.app|ghostty|WezTerm|WezTerm*) return 0 ;;
  esac

  # Environment markers
  [[ -n "${WT_SESSION:-}" ]]        && return 0   # Windows Terminal
  [[ -n "${KITTY_WINDOW_ID:-}" ]]   && return 0   # kitty
  [[ -n "${KONSOLE_VERSION:-}" ]]   && return 0   # Konsole
  [[ -n "${WEZTERM_EXECUTABLE:-}" ]] && return 0
  [[ -n "${GHOSTTY_RESOURCES_DIR:-}" ]] && return 0

  # VTE / GNOME Terminal / Ptyxis (VTE ≥ 0.79 → numeric 7900)
  if [[ -n "${VTE_VERSION:-}" ]]; then
    local vte=${VTE_VERSION//[^0-9]/}
    [[ "$vte" -ge 7900 ]] && return 0
  fi

  return 1
}

# Emit a raw OSC sequence, wrapping for tmux when needed
_emit_osc() {
  local seq="$1"
  if [[ -n "${TMUX:-}" ]]; then
    # tmux passthrough: double every ESC and wrap in DCS
    # seq is expected to start with \033]
    local wrapped
    wrapped=$(printf '%s' "$seq" | sed 's/\x1b/\x1b\x1b/g')
    printf '\033Ptmux;%s\033\\' "$wrapped"
  else
    printf '%s' "$seq"
  fi
}

set_progress() {
  local percent=$1
  percent=$(( percent < 0 ? 0 : percent > 100 ? 100 : percent ))

  if supports_osc94; then
    _emit_osc "$(printf '\033]9;4;1;%d\a' "$percent")"
  else
    # Fallback: put progress in the window title
    local width=20
    local filled=$(( width * percent / 100 ))
    local empty=$(( width - filled ))
    local bar
    bar=$(printf '█%.0s' $(seq 1 $filled 2>/dev/null))
    bar+=$(printf '░%.0s' $(seq 1 $empty 2>/dev/null))
    printf '\033]0;[%s] %3d%%\a' "$bar" "$percent"
  fi
}

clear_progress() {
  if supports_osc94; then
    _emit_osc $'\033]9;4;0\a'
  else
    # Restore a sensible title
    printf '\033]0;%s\a' "${PWD##*/}"
  fi
}

set_indeterminate() {
  if supports_osc94; then
    _emit_osc $'\033]9;4;3\a'
  fi
}

set_error() {
  local percent=${1:-}
  if supports_osc94; then
    if [[ -n "$percent" ]]; then
      _emit_osc "$(printf '\033]9;4;2;%d\a' "$percent")"
    else
      _emit_osc $'\033]9;4;2\a'
    fi
  fi
}

# Optional: iTerm2-only tab color (ignored elsewhere)
set_tab_color() {
  local r=$1 g=$2 b=$3
  if [[ "${TERM_PROGRAM:-}" == "iTerm.app" ]]; then
    printf '\033]6;1;bg;red;brightness;%d\a'   "$r"
    printf '\033]6;1;bg;green;brightness;%d\a' "$g"
    printf '\033]6;1;bg;blue;brightness;%d\a'  "$b"
  fi
}

reset_tab_color() {
  if [[ "${TERM_PROGRAM:-}" == "iTerm.app" ]]; then
    printf '\033]6;1;bg;*;default\a'
  fi
}

# -----------------------------------------------------------------------------
# Demo
# -----------------------------------------------------------------------------
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  echo "Testing OSC 9;4 progress (supports_osc94 = $(supports_osc94 && echo yes || echo no))"
  echo "Inside tmux? ${TMUX:+yes}"

  for i in $(seq 0 5 100); do
    set_progress "$i"
    sleep 0.04
  done
  clear_progress
  echo "Done."
fi
