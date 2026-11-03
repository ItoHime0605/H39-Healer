-- H-39 medical tube visibility and single spent-cartridge ejection.
-- Reuses TaCZ's original state machine and an already allocated parallel
-- track. Does NOT mutate TaCZ's shared track counters or add custom states.
local default = require("tacz_default_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local tube_track = default.PARALLEL_TRACK_5

-- Suppress one brass case on every shot; H-39 ejects a medical cartridge
-- only when the ammo count crosses a five-round boundary.
local idle_original = default.main_track_states.idle
local h39_idle = setmetatable({}, {__index = idle_original})
function h39_idle.transition(this, context, input)
    if input == INPUT_SHOOT then
        return this.main_track_states.idle
    end
    return idle_original.transition(this, context, input)
end

local function bounded_rounds(context)
    local count = context:getAmmoCount()
    if type(count) ~= "number" then return 0 end
    return math.max(0, math.min(20, math.floor(count)))
end

local function tube_count(rounds)
    return math.floor((rounds + 4) / 5)
end

local last_rounds = -1
local last_visible = -1

local function update_tube_view(context)
    local rounds = bounded_rounds(context)
    local visible = tube_count(rounds)

    -- Single-shot decrement only. Bulk sync jumps / reloads must never eject.
    if last_rounds >= 0 and rounds == last_rounds - 1
            and tube_count(rounds) < tube_count(last_rounds) then
        context:popShellFrom(0)
    end

    if visible ~= last_visible then
        context:runAnimation(
            "tubes_" .. tostring(visible),
            context:getTrack(STATIC_TRACK_LINE, tube_track),
            true, PLAY_ONCE_HOLD, 0
        )
        last_visible = visible
    end
    last_rounds = rounds
end

-- The default base track is already one of TaCZ's registered states.
-- Add its update hook instead of inserting a new partial state, which
-- could break input transitions (including aiming / right-click).
local base_original = default.base_track_state
local h39_base = setmetatable({}, {__index = base_original})
function h39_base.entry(this, context)
    base_original.entry(this, context)
    update_tube_view(context)
end
function h39_base.update(this, context)
    update_tube_view(context)
end

local M = setmetatable({
    base_track_state = h39_base,
    main_track_states = setmetatable({idle = h39_idle},
                                    {__index = default.main_track_states})
}, {__index = default})

function M:initialize(context)
    last_rounds = -1
    last_visible = -1
    default.initialize(self, context)
end

return M
