-- TaCZ LuaJ smoke test. Drives the real default Lua plus H-39 extension.
-- Not an in-game simulation: validates state, animation and input callbacks.
assert(arg[1] and arg[2], "Need default Lua and H39 Lua paths")
package.preload["tacz_default_state_machine"] = function()
    return dofile(arg[1])
end
INPUT_SHOOT = "shoot"
INPUT_RELOAD = "reload"
LOOP = "loop"
PLAY_ONCE_STOP = "stop"
PLAY_ONCE_HOLD = "hold"

local function same(got, want, label)
    if got ~= want then
        error(label .. ": got " .. tostring(got) .. " expected " .. tostring(want), 2)
    end
end

local function make_context(name)
    local c = {name=name, ammo=20, aim=0, played={}, shells=0, lines={}, signals={}}
    local m = {}
    function m:ensureTrackLineSize(n) self.line_count=n end
    function m:ensureTracksAmount(line, n) self.lines[line]=n end
    function m:getTrack(line, n)
        assert(self.lines[line] and n >= 0 and n < self.lines[line],
            "Invalid TaCZ animation track " .. tostring(line) .. "," .. tostring(n))
        return tostring(line) .. ":" .. tostring(n)
    end
    function m:runAnimation(name, track, additive, mode, transition)
        table.insert(self.played, name)
    end
    function m:stopAnimation(track) end
    function m:setAnimationProgress(track, progress, flag) end
    function m:adjustAnimationProgress(track, progress, flag) end
    function m:getAmmoCount() return self.ammo end
    function m:getAimingProgress() return self.aim end
    function m:hasBulletInBarrel() return false end
    function m:isOverHeat() return false end
    function m:isStopped(track) return false end
    function m:isHolding(track) return true end
    function m:setShouldHideCrossHair(flag) end
    function m:trigger(input) table.insert(self.signals, input) end
    function m:findIdleTrack(line, additive) return tostring(line) .. ":0" end
    function m:isCharging() return false end
    function m:getPutAwayTime() return 0.5 end
    function m:anchorWalkDist() end
    function m:isOnGround() return true end
    function m:popShellFrom(index)
        same(index, 0, "Spent medical tube bone")
        self.shells = self.shells + 1
    end
    setmetatable(c, {__index=m})
    return c
end

local h39 = dofile(arg[2])
local native = package.loaded["tacz_default_state_machine"]
assert(h39 and native)
local old_track_total = native.static_track_top.value
local a, b = make_context("A"), make_context("B")

h39:initialize(a)
same(#h39:states(), 8, "Preserve TaCZ's eight default states")
h39.base_track_state.entry(h39, a)
same(a.played[1], "static_idle", "Default idle animation")
same(a.played[2], "tubes_4", "Initial four tubes")
same(native.static_track_top.value, old_track_total, "Do not allocate shared TaCZ tracks")

-- Aim/right-click must trigger TaCZ's original ADS state transitions.
h39.ADS_states.aiming.entry(h39, a)
a.aim = 1
h39.ADS_states.aiming.update(h39, a)
h39.ADS_states.aiming.transition(h39, a, h39.INPUT_AIM_RETREAT)
assert(#a.played >= 4, "ADS animations missing")
-- Shoot and reload input must not throw errors or eject brass every shot.
same(h39.main_track_states.idle.transition(h39, a, INPUT_SHOOT),
    h39.main_track_states.idle, "H39 shoot transition")
same(a.shells, 0, "No brass casing on ordinary shot")
h39.gun_kick_state.transition(h39, a, INPUT_SHOOT)
same(a.played[#a.played], "shoot", "TaCZ gun-kick animation")
h39.main_track_states.idle.transition(h39, a, INPUT_RELOAD)
same(a.played[#a.played], "reload_tactical", "TaCZ reload animation")

-- Interleave two contexts: a second gun may initialize between shots from A.
-- Old module-global last_rounds failed here (no cartridge ejection for A).
for ammo=19,16,-1 do
    a.ammo=ammo
    h39.base_track_state.update(h39, a)
end
same(a.shells, 0, "First 4 rounds should not eject")
h39:initialize(b)
h39.base_track_state.entry(h39, b)
same(b.played[#b.played], "tubes_4", "Second gun initial display")
a.ammo=15
h39.base_track_state.update(h39, a)
same(a.shells, 1, "A must eject after 16->15, regardless of B")
for ammo=19,15,-1 do
    b.ammo=ammo
    h39.base_track_state.update(h39, b)
end
same(b.shells, 1, "B independent 16->15 ejection")
for ammo=14,0,-1 do
    a.ammo=ammo
    h39.base_track_state.update(h39, a)
end
same(a.shells, 4, "A exactly four tubes after 20 shots")
same(a.played[#a.played], "tubes_0", "All tubes hidden when empty")
same(b.shells, 1, "No cross-instance ejection")

-- Refill, jumps and re-equipping must not eject phantom cartridges.
a.ammo=20
h39.base_track_state.update(h39, a)
a.ammo=10
h39.base_track_state.update(h39, a)
same(a.shells, 4, "Reload and ammo sync never eject")
h39:initialize(a)
a.ammo=8
h39.base_track_state.entry(h39, a)
same(a.shells, 4, "Re-equipping never ejects")
print("PASS: native TaCZ LuaJ ADS/reload/shoot, cartridge timing, independent instances")
