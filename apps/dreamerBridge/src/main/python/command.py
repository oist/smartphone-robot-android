"""The command the policy follows: (forward, turn), each in [-1, 1].

Only a policy trained on the command task (env.robot task 'command' in the
dreamerv3 repo) reads it; the others ignore it. It is chosen here, on the
phone, because the phone runs the policy: the command must be in the
observation the instant the policy acts, and it goes to the trainer with every
observation so the reward there is scored against the same one.

Where it comes from, in order:

* the joystick on the web page (webui.py), while someone is touching it --
  each move posts a command and it holds for JOYSTICK_HOLD seconds;
* otherwise, by mode:
  - 'auto': a random command, held for hold_min..hold_max seconds. Zero with
    p_zero, one axis only with p_axis (forward or turn, evenly), both axes
    otherwise, inside the unit disk like the joystick. Each nonzero axis is a
    multiple of `step` (0.5: -1, -0.5, 0.5, 1; a both-axes pair outside the
    disk is scaled onto it) or, with step 0, anywhere in [-1, 1]. This is what training runs
    on; nobody can steer for an hour.
  - 'manual': zero, which is "balance in place".
  - 'direct': no policy at all. The joystick sets the motor outputs itself
    (main.loop_direct), to see what the wheels can do; zero when let go.

With `ramp` > 0 the command handed out moves toward the chosen one at that
many units per second, from any source: a balancing robot has to lean before
it can speed up, and a step in the command leaves it no time to. The policy
sees and is scored against the ramped command.

Training starts in 'auto', the trainer's handshake sets the sampler
(`command` in its hello, which also resets the mode to the run's), the web
page can switch the mode at any time, and the player starts in 'manual'.
"""

import math
import random
import threading
import time

# A joystick that stops posting is let go after this long, so a phone that
# drops off the WiFi mid-drive cannot leave the robot driving.
JOYSTICK_HOLD = 0.5

# 'auto' from the start: the app trains by default, and a policy that does not
# read commands ignores them. Starting in 'manual' left the page showing
# "Balance when idle" after every app restart until a trainer connected.
# The player switches to 'manual' itself.
DEFAULTS = dict(mode='auto', hold_min=2.0, hold_max=5.0, p_zero=0.3,
                p_axis=0.4, step=0.0, ramp=0.0)


class CommandSource:

    def __init__(self, seed=None):
        self._lock = threading.Lock()
        self._rng = random.Random(seed)
        self.settings = dict(DEFAULTS)
        self._auto = (0.0, 0.0)
        self._auto_until = 0.0
        self._stick = (0.0, 0.0)
        self._stick_at = -1e9
        self.current = (0.0, 0.0)
        self.source = 'idle'
        self._ticked = 0.0

    def configure(self, settings):
        """Sampler settings from the trainer's hello; missing keys keep theirs."""
        with self._lock:
            for key in DEFAULTS:
                if settings and key in settings:
                    self.settings[key] = settings[key]
            self._auto_until = 0.0

    def set_mode(self, mode):
        if mode not in ('auto', 'manual', 'direct'):
            raise ValueError(
                'mode must be auto, manual or direct, not %r' % mode)
        with self._lock:
            self.settings['mode'] = mode
            self._auto_until = 0.0

    def joystick(self, forward, turn):
        """A joystick position, from the web server's thread."""
        forward = max(-1.0, min(1.0, float(forward)))
        turn = max(-1.0, min(1.0, float(turn)))
        # On the run's grid, if it has one: a policy trained on the five
        # levels -1, -0.5, 0, 0.5, 1 never saw anything in between. The page
        # snaps too; this covers any other client.
        step = float(self.settings.get('step') or 0.0)
        if step > 0:
            forward = round(forward / step) * step
            turn = round(turn / step) * step
        with self._lock:
            self._stick = (forward, turn)
            self._stick_at = time.monotonic()

    def release(self):
        with self._lock:
            self._stick_at = -1e9

    def tick(self):
        """The command for this control step, as [forward, turn]."""
        now = time.monotonic()
        with self._lock:
            if now - self._stick_at < JOYSTICK_HOLD:
                cmd, src = self._stick, 'joystick'
            elif self.settings['mode'] == 'auto':
                if now >= self._auto_until:
                    self._auto = self._sample()
                    s = self.settings
                    self._auto_until = now + self._rng.uniform(
                        float(s['hold_min']), float(s['hold_max']))
                cmd, src = self._auto, 'auto'
            else:
                stopped = self.settings['mode'] == 'direct'
                cmd, src = (0.0, 0.0), 'stopped' if stopped else 'idle'
            ramp = float(self.settings.get('ramp') or 0.0)
            if ramp > 0:
                # The first tick holds still: no interval to ramp over yet.
                room = ramp * min(now - self._ticked, 0.1) \
                    if self._ticked else 0.0
                cmd = tuple(old + max(-room, min(room, new - old))
                            for old, new in zip(self.current, cmd))
            self._ticked = now
        self.current, self.source = cmd, src
        return [cmd[0], cmd[1]]

    def _value(self):
        """One nonzero axis value."""
        step = float(self.settings.get('step') or 0.0)
        if step <= 0:
            return self._rng.uniform(-1.0, 1.0)
        levels = max(1, int(round(1.0 / step)))
        return self._rng.choice((-1.0, 1.0)) * min(
            1.0, step * self._rng.randint(1, levels))

    def _sample(self):
        s = self.settings
        r = self._rng.random()
        if r < float(s['p_zero']):
            return (0.0, 0.0)
        if r < float(s['p_zero']) + float(s['p_axis']):
            if self._rng.random() < 0.5:
                return (self._value(), 0.0)
            return (0.0, self._value())
        # Both axes: a point in the unit disk, the joystick's own range. The
        # trainer scores the command as one (forward, turn) vector, and a
        # square corner like (1, 1) asks one wheel for speed + turn at once.
        if float(s.get('step') or 0.0) <= 0:
            r = math.sqrt(self._rng.random())
            a = self._rng.uniform(0.0, 2.0 * math.pi)
            return (r * math.cos(a), r * math.sin(a))
        f, t = self._value(), self._value()
        n = max(1.0, math.hypot(f, t))
        return (f / n, t / n)
